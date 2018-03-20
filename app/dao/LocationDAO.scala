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

  def get(id: Long): Future[Option[Location]] = db.run(Locations.filter(_.id === id).result.headOption)

  def insert(location: Location): Future[Unit] = db.run(Locations += location).map { _ => () }

  def toOptionsList(locations: Seq[Location]): Seq[(String, String)] = locations.map(location => (location.id.get.toString, location.title))

  private class LocationTable(tag: Tag) extends Table[Location](tag, "LOCATION") {
    def id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    def title = column[String]("TITLE")
    def address = column[String]("ADDRESS")
    def city = column[String]("CITY")
    def zipCode = column[String]("ZIP_CODE")
    def state = column[String]("STATE")
    def country = column[String]("COUNTRY")
    def * = (id, title, address, city, zipCode, state, country) <> (Location.tupled, Location.unapply)
  }
}
