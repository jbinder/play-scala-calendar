package models

import org.joda.time.DateTime

case class Series(id: Option[Long], duration: Int, startsAt: DateTime, endsAt: DateTime, freq: Int, byDay: String, interval: Int, createdAt: DateTime, eventId: Long) {
  def getByDays(): Array[models.Day.Value] = {
    byDay.split(",").map(x => Day.withName(x))
  }
}

object Freq extends Enumeration {
  type Freq = Value
  // TODO: val Daily = Value(1)
  val Weekly = Value(2)
}

object Day extends Enumeration {
  type Day = Value
  val Monday = Value(1, "MO")
  val Tuesday = Value(2, "TU")
  val Wednesday = Value(3, "WE")
  val Thursday = Value(4, "TH")
  val Friday = Value(5, "FR")
  val Saturday = Value(6, "SA")
  val Sunday = Value(7, "SU")
}