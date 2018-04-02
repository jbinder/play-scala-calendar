package dao

import javax.inject.Inject
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class TagDAO @Inject() (protected val dbConfigProvider: DatabaseConfigProvider)(implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {
  import profile.api._

  private val Tags = TableQuery[TagsTable]

  def get(id: Long): Future[Option[models.Tag]] = db.run(Tags.filter(_.id === id).result.headOption)

  def get(name: String): Future[Option[models.Tag]] = db.run(Tags.filter(_.name === name).result.headOption)

  def insert(name: String): Future[Long] = {
    val normalizedName = normalizeName(name)
    get(normalizedName).flatMap(tag => {
      if (tag.isEmpty) {
        db.run(Tags.returning(Tags.map(_.id)).into((item, id) => item.copy(id)) += models.Tag(None, normalizedName)).map(_ => tag.get.id.get)
      } else {
        Future { tag.get.id.get }
      }
    })
  }

  private def normalizeName(name: String): String = {
    name.trim.toLowerCase
  }

  class TagsTable(tag: Tag) extends Table[models.Tag](tag, "TAG") {
    def id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    def name = column[String]("NAME")
    def * = (id, name) <> (models.Tag.tupled, models.Tag.unapply)
  }
}
