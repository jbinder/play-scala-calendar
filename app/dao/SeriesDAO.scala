package dao

import com.github.tototoshi.slick.H2JodaSupport._
import forms.AddSeriesForm
import javax.inject.Inject
import models.{Occurrence, Series}
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class SeriesDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider,
  protected val occurrenceDAO: OccurrenceDAO)
  (implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val Series = TableQuery[SeriesTable]

  def all(): Future[Seq[Series]] = db.run(Series.result)

  def get(id: Long): Future[Option[Series]] = db.run(Series.filter(_.id === id).result.headOption)

  def get(ids: Seq[Long]): Future[Seq[Series]] = db.run(Series.filter(_.id.inSet(ids)).result)

  def insert(seriesData: AddSeriesForm.Data): Future[Unit] = {
    val series = models.Series(None, seriesData.duration, getStartDateFromInput(seriesData), getEndDateFromInput(seriesData), seriesData.freq, seriesData.byDay.mkString(","), seriesData.interval, DateTime.now, seriesData.eventId)
    db.run(Series.returning(Series.map(_.id.get)) into ((series, id) => series.copy(id=Some(id))) += series).map { series =>
      occurrenceDAO.insertAll(series)
    }
  }

  def update(id: Long, seriesData: AddSeriesForm.Data): Future[Unit] = {
    val query = Series.filter(_.id === id)
    val oldSeries = db.run(query.result.head)
    val update = query
      .map(e => (e.duration, e.startsAt, e.endsAt, e.freq, e.byDay, e.interval, e.eventId))
      .update((seriesData.duration, getStartDateFromInput(seriesData), getEndDateFromInput(seriesData), seriesData.freq, seriesData.byDay.mkString(","), seriesData.interval, seriesData.eventId))
    db.run(update)
    val newSeries = db.run(query.result.head)
    oldSeries.map(so => newSeries.map(sn => occurrenceDAO.updateAll(so, sn)))
  }

  private def getEndDateFromInput(seriesData: AddSeriesForm.Data) = {
    new DateTime(seriesData.endsAtDate)
  }

  private def getStartDateFromInput(seriesData: AddSeriesForm.Data) = {
    new DateTime(seriesData.startsAtDate).plusHours(seriesData.startsAtHour).plusMinutes(seriesData.startsAtMinute)
  }

  def delete(id: Long): Future[Unit] = {
    // TODO: transaction
    for {
      _ <- occurrenceDAO.deleteAll(id)
      _ <- db.run(Series.filter(_.id === id).delete)
    } yield()
  }

  private class SeriesTable(tag: Tag) extends Table[Series](tag, "SERIES") {
    def id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    def duration = column[Int]("DURATION")
    def startsAt = column[DateTime]("STARTS_AT")
    def endsAt = column[DateTime]("ENDS_AT")
    def freq = column[Int]("FREQ")
    def byDay = column[String]("BY_DAY")
    def interval = column[Int]("INTERVAL")
    def eventId = column[Long]("EVENT_ID")
    def createdAt = column[DateTime]("CREATED_AT")
    def * = (id, duration, startsAt, endsAt, freq, byDay, interval, createdAt, eventId) <> (models.Series.tupled, models.Series.unapply)
  }
}
