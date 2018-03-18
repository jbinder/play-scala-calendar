package controllers

import javax.inject._
import dao.{EventDAO, LocationDAO}
import forms.AddEventForm
import models.Event
import org.joda.time.DateTime
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventController @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  eventDao: EventDAO,
  locationDao: LocationDAO,
  cc: ControllerComponents,
)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport with HasDatabaseConfigProvider[JdbcProfile] {

  def index() = Action.async { implicit request: Request[AnyContent] =>
    eventDao.all().map(events => Ok(views.html.event.showAll(events)))
  }

  def addEvent() = Action.async { implicit request =>
    locationDao.all().map(locations => Ok(views.html.event.addEvent(AddEventForm.form, locationDao.toOptionsList(locations))))
  }

  def addEventPost() = Action.async { implicit request: Request[AnyContent] =>
    AddEventForm.form.bindFromRequest.fold(
      formWithErrors => {
        locationDao.all().map(locations => BadRequest(views.html.event.addEvent(formWithErrors, locationDao.toOptionsList(locations))))
      },
      eventData => {
        eventDao.insert(Event(None, eventData.title, eventData.description, new DateTime(eventData.startsAt), new DateTime(eventData.endsAt), DateTime.now, eventData.locationId))
        Future{Redirect(routes.HomeController.index()).flashing("success" -> "Event added!")}
      }
    )
  }
}
