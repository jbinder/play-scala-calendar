package forms

import java.util.Date

import play.api.data.Form
import play.api.data.Forms._

object AddSeriesForm {

  val form = Form(
    mapping(
      "duration" -> number,
      "startsAtDate" -> date,
      "startsAtHour" -> number(min = 0, max = 23),
      "startsAtMinute" -> number(min = 0, max = 59),
      "endsAtDate" -> optional(date),
      "freq" -> number,
      "byDay" -> seq(text),
      "interval" -> number,
      "eventId" -> longNumber,
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    duration: Int,
    startsAtDate: Date,
    startsAtHour: Int,
    startsAtMinute: Int,
    endsAtDate: Option[Date],
    freq: Int,
    byDay: Seq[String],
    interval: Int,
    eventId: Long,
  )
}