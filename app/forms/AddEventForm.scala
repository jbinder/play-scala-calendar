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
      "startsAt" -> date,
      "endsAt" -> date,
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    title: String,
    description: String,
    startsAt: Date,
    endsAt: Date,
  )
}