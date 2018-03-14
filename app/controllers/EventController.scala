package controllers

import javax.inject._

import forms.AddEventForm
import play.api.mvc._

@Singleton
class EventController @Inject()(cc: ControllerComponents) extends AbstractController(cc) with play.api.i18n.I18nSupport {

  def index() = Action { implicit request: Request[AnyContent] =>
    Ok(views.html.event.showAll())
  }

  def addEvent() = Action { implicit request =>
    Ok(views.html.event.addEvent(AddEventForm.form))
  }

  def addEventPost() = Action { implicit request: Request[AnyContent] =>
    AddEventForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.event.addEvent(formWithErrors))
      },
      eventData => {
        // TODO: persist
        Redirect(routes.HomeController.index()).flashing("success" -> "Event added!")
      }
    )
  }
}
