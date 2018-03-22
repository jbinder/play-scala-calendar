package dao

import com.github.slugify.Slugify
import javax.inject.Inject
import models.{Event, Location}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import com.github.tototoshi.slick.H2JodaSupport._
import forms.AddEventForm
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

class EventDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider, protected val locationDAO: LocationDAO, protected val slugify: Slugify)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Events = TableQuery[EventsTable]
  private val Locations = TableQuery[locationDAO.LocationTable]

  def all(): Future[Seq[(Event, Location)]] = {
    val q = for {
      (event, location) <- Events.join(Locations).on(_.locationId === _.id)
    } yield (event, location)
    db.run(q.result)
  }

  def get(id: Long): Future[Option[(Event, Location)]] = {
    getEvent(Events.filter(x => x.id === id))
  }

  def get(slug: String): Future[Option[(Event, Location)]] = {
    getEvent(Events.filter(x => x.slug === slug))
  }

  private def getEvent(event: Query[EventsTable, Event, Seq]) = {
    val q = for {
      (event, location) <- event.join(Locations).on(_.locationId === _.id)
    } yield (event, location)
    db.run(q.result.headOption)
  }

  def insert(eventData: AddEventForm.Data): Future[Unit] = {
    val startDateTime = new DateTime(eventData.startsAtDate).plusHours(eventData.startsAtHour).plusMinutes(eventData.startsAtMinute)
    val endDateTime = new DateTime(eventData.endsAtDate).plusHours(eventData.endsAtHour).plusMinutes(eventData.endsAtMinute)
    buildSlug(eventData.title).map(slug =>
    {
      val event = Event(None, eventData.title, slug, eventData.description, startDateTime, endDateTime, DateTime.now, eventData.locationId)
      db.run(Events += event).map { _ => () }
    })
  }

  private def buildSlug(title: String): Future[String] = {
    val desiredSlug = slugify.slugify(title)
    db.run(Events.filter(x => x.slug.startsWith(desiredSlug)).result).map(events => {
      val conflictingSlugs = events.map(event => event.slug)

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
}
