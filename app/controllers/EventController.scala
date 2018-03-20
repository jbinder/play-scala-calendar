package controllers

import javax.inject._
import dao.{EventDAO, LocationDAO}
import forms.AddEventForm
import models.Event
import org.joda.time.DateTime
import play.api.data.Form
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import play.twirl.api.Html
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class EventController @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  eventDao: EventDAO,
  locationDao: LocationDAO,
  cc: ControllerComponents,
)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport with HasDatabaseConfigProvider[JdbcProfile] {

  def index(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    eventDao.all().map(events => Ok(views.html.event.showAll(events)))
  }

  def viewEvent(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    eventDao.get(id).map(event => Ok(views.html.event.viewEvent(event.get)))
  }

  def addEvent(): Action[AnyContent] = Action.async { implicit request =>
    getAddEventView(AddEventForm.form).map(html => Ok(html))
  }

  def addEventPost(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    AddEventForm.form.bindFromRequest.fold(
      formWithErrors => {
        getAddEventView(formWithErrors).map(html => BadRequest(html))
      },
      eventData => {
        val startDateTime = new DateTime(eventData.startsAtDate).plusHours(eventData.startsAtHour).plusMinutes(eventData.startsAtMinute)
        val endDateTime = new DateTime(eventData.endsAtDate).plusHours(eventData.endsAtHour).plusMinutes(eventData.endsAtMinute)
        eventDao.insert(Event(None, eventData.title, eventData.description, startDateTime, endDateTime, DateTime.now, eventData.locationId))
        Future {
          Redirect(routes.HomeController.index()).flashing("success" -> "Event added!")
        }
      }
    )
  }

  private def getAddEventView(form: Form[AddEventForm.Data])(implicit request: Request[AnyContent]): Future[Html] = {
    locationDao.all().map(locations => views.html.event.addEvent(form,
      locationDao.toOptionsList(locations),
      (0 to 23).map(x => (x.toString, x.toString)),
      (0 to 59 by 15).map(x => (x.toString, x.toString))
    ))
  }
}
