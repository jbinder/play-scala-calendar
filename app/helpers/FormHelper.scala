package helpers

import views.html

object FormHelper {
  import views.html.helper.FieldConstructor
  implicit val fields = FieldConstructor(html.common.fieldConstructorTemplate.apply)
}
