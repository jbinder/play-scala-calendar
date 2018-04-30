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
      "endsAtDate" -> date,
      "freq" -> number,
      "byDay" -> seq(text),
      "interval" -> default(number, 1),
      "eventId" -> longNumber,
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    duration: Int,
    startsAtDate: Date,
    startsAtHour: Int,
    startsAtMinute: Int,
    endsAtDate: Date,
    freq: Int,
    byDay: Seq[String],
    interval: Int,
    eventId: Long,
  )
}