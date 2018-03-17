package forms

import play.api.data.Form
import play.api.data.Forms._

/**
 * The form which handles adding locations.
 */
object AddLocationForm {

  val form = Form(
    mapping(
      "title" -> nonEmptyText(maxLength = 64),
      "address" -> nonEmptyText(maxLength = 255),
      "city" -> nonEmptyText(maxLength = 32),
      "zipCode" -> nonEmptyText(maxLength = 32),
      "state" -> nonEmptyText(maxLength = 32),
      "country" -> nonEmptyText(minLength = 2, maxLength = 2),
    )(Data.apply)(Data.unapply)
  )

  case class Data(
    title: String,
    address: String,
    city: String,
    zipCode: String,
    state: String,
    country: String,
  )
}