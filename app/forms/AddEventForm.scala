package forms

import java.util.Date

import play.api.data.Form
import play.api.data.Forms._

/**
 * The form which handles adding a calendar event.
 */
object AddEventForm {

  val form = Form(
    mapping(
      "title" -> nonEmptyText,
      "description" -> nonEmptyText,
      "startsAtDate" -> date,
      "startsAtHour" -> number(min = 0, max = 23),
      "startsAtMinute" -> number(min = 0, max = 59),
      "endsAtDate" -> date,
      "endsAtHour" -> number(min = 0, max = 23),
      "endsAtMinute" -> number(min = 0, max = 59),
      "locationId" -> longNumber,
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    title: String,
    description: String,
    startsAtDate: Date,
    startsAtHour: Int,
    startsAtMinute: Int,
    endsAtDate: Date,
    endsAtHour: Int,
    endsAtMinute: Int,
    locationId: Long,
  )
}