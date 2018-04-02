package dao

import com.github.slugify.Slugify
import javax.inject.Inject
import models.{Event, EventTag, Location}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import com.github.tototoshi.slick.H2JodaSupport._
import forms.AddEventForm
import org.joda.time.DateTime

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
    val now = DateTime.now()
    getEvents(Events.filter(event => event.startsAt > now.plusDays(offset.getOrElse(0)) && event.startsAt < now.plusDays(numDays + offset.getOrElse(0))))
  }

  private def getEvents(event: Query[EventsTable, Event, Seq]): Future[Seq[(Event, Location, Seq[models.Tag])]] = {
    val q = for {
      (event, location) <- event.join(Locations).on(_.locationId === _.id)
    } yield (event, location)
    db.run(q.result).map { (row) =>
      row.map { r => {
        val event = r._1
        val location = r._2
        db.run((for {(_, tag) <- EventsTags.filter(eventTag => eventTag.eventId === event.id).join(Tags).on(_.tagId === _.id)} yield tag).result).map { (row) =>
          (event, location, row)
        }
      }}
    }.map((x) => Future.sequence(x)).flatten
  }

  def insert(eventData: AddEventForm.Data): Future[Unit] = {
    prepareEvent(eventData, Option.empty).map(data => {
      val event = Event(None, eventData.title, data._3, eventData.description, data._1, data._2, DateTime.now, eventData.locationId)
      val tagNames = eventData.tags.split("(;|\\s)")
      // TODO: transaction!
      val tagIds = tagNames.map(tag => tagDAO.insert(tag))
      db.run(Events.returning(Events.map(_.id)) += event).map(id => {
        tagIds.map((tags) => tags.map(tagId => db.run(EventsTags += EventTag(id.get, tagId))))
      })
    })
  }

  def update(slug: String, eventData: AddEventForm.Data): Future[Unit] = {
    val query = Events.filter(_.slug === slug)
    prepareEvent(eventData, Option(slug)).map(data => {
      val update = query.result.head.flatMap {event =>
        query.update(event.patch(Option(eventData.title), Option(data._3), Option(eventData.description), Option(data._1), Option(data._2), Option(eventData.locationId)))
        // TODO: tags
      }
      db.run(update)
    })
  }

  def delete(slug: String): Unit = {
    db.run(Events.filter(_.slug === slug).delete)
  }

  def isLocationInUse(locationId: Long): Future[Boolean] = {
    db.run(Events.filter(_.locationId === locationId).exists.result)
  }

  private def prepareEvent(eventData: AddEventForm.Data, slug: Option[String]): Future[(DateTime, DateTime, String)] = {
    val startDateTime = new DateTime(eventData.startsAtDate).plusHours(eventData.startsAtHour).plusMinutes(eventData.startsAtMinute)
    val endDateTime = new DateTime(eventData.endsAtDate).plusHours(eventData.endsAtHour).plusMinutes(eventData.endsAtMinute)
    buildSlug(eventData.title, slug).map(slug => (startDateTime, endDateTime, slug))
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
    def startsAt = column[DateTime]("STARTS_AT")
    def endsAt = column[DateTime]("ENDS_AT")
    def createdAt = column[DateTime]("CREATED_AT")
    def locationId = column[Long]("LOCATION_ID")
    def * = (id, title, slug, description, startsAt, endsAt, createdAt, locationId) <> (Event.tupled, Event.unapply)
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
