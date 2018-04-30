package models

import org.joda.time.DateTime

case class Series(id: Option[Long], duration: Int, startsAt: DateTime, endsAt: DateTime, freq: Int, byDay: String, interval: Int, createdAt: DateTime, eventId: Long)

object Freq extends Enumeration {
  type Freq = Value
  // TODO: val Daily = Value(1)
  val Weekly = Value(2)
}

object Day extends Enumeration {
  type Day = Value
  val Monday = Value("MO")
  val Tuesday = Value("TU")
  val Wednesday = Value("WE")
  val Thursday = Value("TH")
  val Friday = Value("FR")
  val Saturday = Value("SA")
  val Sunday = Value("SU")
}