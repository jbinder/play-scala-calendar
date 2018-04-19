package controllers

import dao.{EventDAO, LocationDAO}
import forms.AddEventForm
import javax.inject._
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
    eventDao.all().map(data => Ok(views.html.event.showAll(data)))
  }

  def viewEvent(slug: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    eventDao.get(slug).map(data => Ok(views.html.event.viewEvent(data.get)))
  }

  def addEvent(): Action[AnyContent] = Action.async { implicit request =>
    getAddEventView(AddEventForm.form, Option.empty).map(html => Ok(html))
  }

  def addEventPost(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    AddEventForm.form.bindFromRequest.fold(
      formWithErrors => {
        getAddEventView(formWithErrors, Option.empty).map(html => BadRequest(html))
      },
      eventData => {
        eventDao.insert(eventData)
        Future { Redirect(routes.HomeController.index()).flashing("success" -> "Event added!") }
      }
    )
  }

  def editEvent(slug: String): Action[AnyContent] = Action.async { implicit request =>
    eventDao.get(slug).map(data => {
      val event = data.get._1
      getAddEventView(AddEventForm.form.fill(AddEventForm.Data(
        event.title,
        event.description,
        event.locationId,
        data.get._3.map(tag => tag.name).mkString(", ")
      )), Option(slug)).map(html => Ok(html))
    }).flatten
  }

  def editEventPost(slug: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    AddEventForm.form.bindFromRequest.fold(
      formWithErrors => {
        getAddEventView(formWithErrors, Option(slug)).map(html => BadRequest(html))
      },
      eventData => {
        eventDao.update(slug, eventData)
        Future { Redirect(routes.HomeController.index()).flashing("success" -> "Event updated!") }
      }
    )
  }

  def deleteEvent(slug: String): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    eventDao.delete(slug)
    Future { Redirect(routes.HomeController.index()).flashing("success" -> "Event deleted!") }
  }

  private def getAddEventView(form: Form[AddEventForm.Data], slug: Option[String])(implicit request: Request[AnyContent]): Future[Html] = {
    locationDao.all().map(locations => views.html.event.addEvent(form,
      locationDao.toOptionsList(locations),
      (0 to 23).map(x => (x.toString, x.toString)),
      (0 to 59 by 15).map(x => (x.toString, x.toString)),
      slug
    ))
  }
}
