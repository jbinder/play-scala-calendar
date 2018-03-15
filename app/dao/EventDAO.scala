package dao

import javax.inject.Inject

import models.Event
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import com.github.tototoshi.slick.H2JodaSupport._
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}

class EventDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Events = TableQuery[EventsTable]

  def all(): Future[Seq[Event]] = db.run(Events.result)

  def insert(event: Event): Future[Unit] = db.run(Events += event).map { _ => () }

  private class EventsTable(tag: Tag) extends Table[Event](tag, "EVENT") {
    def title = column[String]("TITLE", O.PrimaryKey)
    def description = column[String]("DESCRIPTION")
    def startsAt = column[DateTime]("STARTS_AT")
    def endsAt = column[DateTime]("ENDS_AT")
    def createdAt = column[DateTime]("CREATED_AT")
    def * = (title, description, startsAt, endsAt, createdAt) <> (Event.tupled, Event.unapply)
  }
}
