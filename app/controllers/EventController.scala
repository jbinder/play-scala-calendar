package controllers

import javax.inject._

import dao.EventDAO
import forms.AddEventForm
import models.Event
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

@Singleton
class EventController @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  eventDao: EventDAO,
  cc: ControllerComponents,
)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport with HasDatabaseConfigProvider[JdbcProfile] {

  def index() = Action.async { implicit request: Request[AnyContent] =>
    eventDao.all().map(events => Ok(views.html.event.showAll(events)))
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
        eventDao.insert(Event(eventData.title, eventData.description, new DateTime(eventData.startsAt), new DateTime(eventData.endsAt), DateTime.now))
        Redirect(routes.HomeController.index()).flashing("success" -> "Event added!")
      }
    )
  }
}
