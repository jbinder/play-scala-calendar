package forms

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
      "locationId" -> longNumber,
      "tags" -> text,
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    title: String,
    description: String,
    locationId: Long,
    tags: String,
  )
}