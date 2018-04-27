package models

import org.joda.time.DateTime

case class Series(id: Option[Long], duration: Int, startsAt: DateTime, endsAt: Option[DateTime], freq: Int, byDay: String, interval: Int, createdAt: DateTime, eventId: Long)

object Freq extends Enumeration {
  type Freq = Value
  val Daily = Value(1)
  val Weekly = Value(2)
}