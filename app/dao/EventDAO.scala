package dao

import com.github.slugify.Slugify
import com.github.tototoshi.slick.H2JodaSupport._
import forms.AddEventForm
import javax.inject.Inject
import models.{Event, EventTag, Location}
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class EventDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  protected val locationDAO: LocationDAO,
  protected val tagDAO: TagDAO,
  protected val slugify: Slugify)
  (implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val Events = TableQuery[EventsTable]
  private val Locations = TableQuery[locationDAO.LocationTable]
  private val Tags = TableQuery[tagDAO.TagsTable]
  private val EventsTags = TableQuery[EventsTagsTable]

  def all(): Future[Seq[(Event, Location)]] = {
    val q = for {
      (event, location) <- Events.join(Locations).on(_.locationId === _.id)
    } yield (event, location)
    db.run(q.result)
  }

  def get(id: Long): Future[Option[(Event, Location, Seq[models.Tag])]] = {
    getEvents(Events.filter(event => event.id === id)).map(events => events.headOption)
  }

  def get(slug: String): Future[Option[(Event, Location, Seq[models.Tag])]] = {
    getEvents(Events.filter(event => event.slug === slug)).map(events => events.headOption)
  }

  def getUpcoming(numDays: Int, offset: Option[Int] = Option.empty): Future[Seq[(Event, Location, Seq[models.Tag])]] = {
    getEvents(Events) // TODO: filter for upcoming
  }

  private def getEvents(event: Query[EventsTable, Event, Seq]): Future[Seq[(Event, Location, Seq[models.Tag])]] = {
    val q = for {
      (event, location) <- event.join(Locations).on(_.locationId === _.id)
    } yield (event, location)
    db.run(q.result).map { (row) =>
      row.map { r => {
        val event = r._1
        val location = r._2
        db.run((for {(_, tag) <- EventsTags.filter(eventTag => eventTag.eventId === event.id).join(Tags).on(_.tagId === _.id)} yield tag).result).map { (tags) =>
          (event, location, tags)
        }
      }}
    }.map((x) => Future.sequence(x)).flatten
  }

  def insert(eventData: AddEventForm.Data): Future[Unit] = {
    prepareEvent(eventData, Option.empty).map(slug => {
      val event = Event(None, eventData.title, slug, eventData.description, DateTime.now, eventData.locationId)
      val tagNames = getTags(eventData.tags)
      // TODO: transaction!
      val tagIds = tagNames.map(tag => tagDAO.insert(tag))
      db.run(Events.returning(Events.map(_.id)) += event).map(id => {
        tagIds.map((tags) => tags.map(tagId => db.run(EventsTags += EventTag(id.get, tagId))))
      })
    })
  }

  def update(slug: String, eventData: AddEventForm.Data): Future[Unit] = {
    prepareEvent(eventData, Option(slug)).map(slug => {
      val tagNames = getTags(eventData.tags)
      val tagIds = tagNames.map(tag => tagDAO.insert(tag))

      val eventQuery = Events.filter(_.slug === slug)
      val existingTagsQuery = for {
        event <- eventQuery
        eventTag <- EventsTags.filter(_.eventId === event.id)
        tag <- Tags.filter(_.id === eventTag.tagId)
      } yield tag

      // TODO: transaction!
      db.run(existingTagsQuery.result).map((existingTags) => {
        Future.sequence(tagIds).map((tagIds) => {
          // dissociate obsolete tags
          db.run(eventQuery.result).map(events => {
            db.run(existingTagsQuery.filter(!_.id.inSet(tagIds)).result).map(tags => {
              val obsoleteTags = for {
                eventTag <- EventsTags.filter(eventTag => eventTag.eventId === events.head.id.get && eventTag.tagId.inSet(tags.map(_.id.get)))
              } yield eventTag
              db.run(obsoleteTags.delete)
            })
          })

          // associate new tags
          val newTags = for {
            event <- eventQuery
            tag <- Tags.filter(_.id.inSet(tagIds)).filterNot(_.id.inSet(existingTags.map(_.id.get)))
          } yield (event, tag)
          db.run(newTags.result).map((data) => {
            data.map(eventTag => {
              val event = eventTag._1
              val tag = eventTag._2
              db.run(EventsTags += EventTag(event.id.get, tag.id.get))
            })
          })

          // update event
          val update = eventQuery
            .map(e => (e.title, e.slug, e.description, e.locationId))
            .update((eventData.title, slug, eventData.description, eventData.locationId))
          db.run(update)
        })
      })
    })
  }

  def delete(slug: String): Unit = {
    db.run(Events.filter(_.slug === slug).result).map(events => {
      db.run(EventsTags.filter(_.eventId === events.head.id.get).delete)
      db.run(Events.filter(_.slug === slug).delete)
    })
  }

  def isLocationInUse(locationId: Long): Future[Boolean] = {
    db.run(Events.filter(_.locationId === locationId).exists.result)
  }

  private def getTags(text: String): Seq[String] = {
    text.split("(;|,|\\s)").filter(tag => !tag.isEmpty)
  }

  private def prepareEvent(eventData: AddEventForm.Data, slug: Option[String]): Future[String] = {
    buildSlug(eventData.title, slug)
  }

  private def buildSlug(title: String, slug: Option[String]): Future[String] = {
    val desiredSlug = slugify.slugify(title)
    if (slug.isDefined && desiredSlug == slug.get) return Future{slug.get}
    db.run(Events.filter(x => x.slug.startsWith(desiredSlug)).result).map(events => {
      val conflictingSlugs = events.map(event => event.slug).filter(s => slug.isEmpty || s != slug.get)

      def buildSlug: (String, Int) => String = (title, i) => slugify.slugify(title + (if (i > 0) " " + i else ""))

      def findNonConflictingSlug: (String, Int) => String = (title, i) => {
        val candidateSlug = buildSlug(title, i)
        if (conflictingSlugs.contains(candidateSlug)) {
          findNonConflictingSlug(title, i + 1)
        } else {
          candidateSlug
        }
      }

      findNonConflictingSlug(title, 0)
    })
  }

  private class EventsTable(tag: Tag) extends Table[Event](tag, "EVENT") {
    def id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    def title = column[String]("TITLE")
    def slug = column[String]("SLUG")
    def description = column[String]("DESCRIPTION")
    def createdAt = column[DateTime]("CREATED_AT")
    def locationId = column[Long]("LOCATION_ID")
    def * = (id, title, slug, description, createdAt, locationId) <> (Event.tupled, Event.unapply)
  }

  private class EventsTagsTable(tag: Tag) extends Table[EventTag](tag, "EVENT_TAG") {
    def eventId = column[Long]("EVENT_ID")
    def tagId = column[Long]("TAG_ID")

    def * = (eventId, tagId) <> (EventTag.tupled, EventTag.unapply)

    def pk = primaryKey("pk_events_tags", (eventId, tagId))
    def eventFk = foreignKey("fk_events", eventId, Events)(event => event.id.get)
    def tagFk = foreignKey("fk_tags", eventId, Tags)(tag => tag.id.get)
  }
}
