package dao

import com.github.tototoshi.slick.H2JodaSupport._
import javax.inject.Inject
import models.{Freq, Occurrence, Series}
import org.apache.commons.lang3.time.DateUtils
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

class OccurrenceDAO @Inject() (
  protected val dbConfigProvider: DatabaseConfigProvider)
  (implicit executionContext: ExecutionContext) extends HasDatabaseConfigProvider[JdbcProfile] {

  import profile.api._

  private val Occurrences = TableQuery[OccurrencesTable]

  def all(): Future[Seq[Occurrence]] = db.run(Occurrences.result)

  def get(id: Long): Future[Option[Occurrence]] = db.run(Occurrences.filter(_.id === id).result.headOption)

  def getUpcoming(numDays: Int, offset: Option[Int] = Option.empty): Future[Seq[Occurrence]] = {
    val from = DateTime.now().plusDays(offset.getOrElse(0))
    val to = from.plusDays(numDays)
    db.run(Occurrences.filter(occurrence => occurrence.date >= from && occurrence.date < to && occurrence.deleted === false).result)
  }

  def insert(occurrence: Occurrence): Future[Unit] = {
    db.run(Occurrences += occurrence).map { _ => () }
  }

  def insertAll(series: Series): Unit = {
    if (series.freq == Freq.Weekly.id) {
      val days = series.getByDays()
      val startDayOfWeek = series.startsAt.dayOfWeek().get()

      val next = days.map(day => (day, day.id - startDayOfWeek))

      var startDay = series.startsAt
      while (startDay.getMillis <= series.endsAt.getMillis) {
        val nextDays = next.map(next => startDay.plusDays(next._2))
          .filter(day => day.getMillis <= series.endsAt.getMillis && day.getMillis >= series.startsAt.getMillis && day.getMillis > DateTime.now.getMillis)
        nextDays.foreach(date => insert(Occurrence(None, series.id.get, date, deleted = false, modified = false)))
        startDay = startDay.plusWeeks(series.interval)
      }
    }
  }

  def update(id: Long, occurrence: Occurrence): Future[Int] = {
    val query = Occurrences.filter(_.id === id)
    val update = query
      .map(e => (e.date, e.deleted, e.modified))
      .update((occurrence.date, occurrence.deleted, occurrence.modified))
    db.run(update)
  }

  def updateAll(oldSeries: Series, newSeries: Series): Unit = {
    val freqChanged = oldSeries.freq != newSeries.freq
    val byDaysChanged = !(oldSeries.getByDays() sameElements newSeries.getByDays())
    val startDateChanged = !DateUtils.isSameDay(oldSeries.startsAt.toDate, newSeries.startsAt.toDate)
    val endDateChanged = !DateUtils.isSameDay(oldSeries.endsAt.toDate, newSeries.endsAt.toDate)
    val timeChanged = oldSeries.startsAt.hourOfDay() != newSeries.startsAt.hourOfDay() ||
        oldSeries.startsAt.minuteOfHour() != newSeries.startsAt.minuteOfHour()
    if (freqChanged) {
      // TODO: not supported
    }
    if (!byDaysChanged && !startDateChanged && !endDateChanged && !timeChanged) {
      // nothing to be done
      return
    }
    if (false) {
      val sameDays = oldSeries.getByDays().toSet.intersect(newSeries.getByDays().toSet)
      // TODO: only updated time for sameDays
      val startTimeChange = newSeries.startsAt.getMillis - oldSeries.startsAt.getMillis
      // TODO: deleted days (changed start/end day)
      // TODO: added days (changed start/end day)
      // TODO: changed days/time
    } else {
      // fallback: re-add all occurrences
      // only modify future unmodified and not deleted occurrences
      db.run(Occurrences.filter(o => o.seriesId === oldSeries.id && o.date > DateTime.now && o.deleted === false && o.modified === false).delete).map(_ => {
        insertAll(newSeries)
      })
    }
  }

  def delete(id: Long): Future[Int] = {
    db.run(Occurrences.filter(_.id === id).delete)
  }

  def deleteAll(seriesId: Long): Future[Int] = {
    db.run(Occurrences.filter(_.seriesId === seriesId).delete)
  }

  private class OccurrencesTable(tag: Tag) extends Table[Occurrence](tag, "OCCURRENCE") {
    def id = column[Option[Long]]("ID", O.PrimaryKey, O.AutoInc)
    def seriesId = column[Long]("SERIES_ID")
    def date = column[DateTime]("DATE")
    def deleted = column[Boolean]("DELETED")
    def modified = column[Boolean]("MODIFIED")
    def * = (id, seriesId, date, deleted, modified) <> (models.Occurrence.tupled, models.Occurrence.unapply)
  }
}
