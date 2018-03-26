package controllers

import javax.inject._
import dao.{EventDAO, LocationDAO}
import forms.AddLocationForm
import models.Location
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import slick.jdbc.JdbcProfile

import scala.concurrent.ExecutionContext

@Singleton
class LocationController @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  locationDao: LocationDAO,
  eventDao: EventDAO,
  cc: ControllerComponents,
)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport with HasDatabaseConfigProvider[JdbcProfile] {

  def index(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    locationDao.all().map(locations => Ok(views.html.location.showAll(locations)))
  }

  def viewLocation(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    locationDao.get(id).map(location => Ok(views.html.location.viewLocation(location.get)))
  }

  def addLocation(): Action[AnyContent] = Action { implicit request =>
    Ok(views.html.location.addLocation(AddLocationForm.form, Option.empty))
  }

  def addLocationPost(): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    AddLocationForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.location.addLocation(formWithErrors, Option.empty))
      },
      locationData => {
        locationDao.insert(Location(None, locationData.title, locationData.address, locationData.city, locationData.zipCode, locationData.state, locationData.country))
        Redirect(routes.HomeController.index()).flashing("success" -> "Location added!")
      }
    )
  }

  def editLocation(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    locationDao.get(id).map(data => {
      val location = data.get
      views.html.location.addLocation(AddLocationForm.form.fill(AddLocationForm.Data(location.title, location.address, location.city, location.zipCode, location.state, location.country)), Option(id))
    }).map(html => Ok(html))
  }

  def editLocationPost(id: Long): Action[AnyContent] = Action { implicit request: Request[AnyContent] =>
    AddLocationForm.form.bindFromRequest.fold(
       formWithErrors => {
        BadRequest(views.html.location.addLocation(formWithErrors, Option(id)))
      },
      locationData => {
        locationDao.update(id, locationData)
        Redirect(routes.HomeController.index()).flashing("success" -> "Location edited!")
      }
    )
  }

  def deleteLocation(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    eventDao.isLocationInUse(id).map(isInUse => {
      if (!isInUse) {
        locationDao.delete(id)
        Redirect(routes.HomeController.index()).flashing("success" -> "Location deleted!")
      } else {
        Redirect(routes.HomeController.index()).flashing("error" -> "Unable to delete location.")
      }
    })
  }
}
