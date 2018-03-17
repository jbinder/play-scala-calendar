package dao

import javax.inject.Inject

import models.Location
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class LocationDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Locations = TableQuery[LocationTable]

  def all(): Future[Seq[Location]] = db.run(Locations.result)

  def insert(location: Location): Future[Unit] = db.run(Locations += location).map { _ => () }

  private class LocationTable(tag: Tag) extends Table[Location](tag, "LOCATION") {
    def title = column[String]("TITLE", O.PrimaryKey)
    def address = column[String]("ADDRESS")
    def city = column[String]("CITY")
    def zipCode = column[String]("ZIP_CODE")
    def state = column[String]("STATE")
    def country = column[String]("COUNTRY")
    def * = (title, address, city, zipCode, state, country) <> (Location.tupled, Location.unapply)
  }
}
