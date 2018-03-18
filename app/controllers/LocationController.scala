package controllers

import javax.inject._

import dao.LocationDAO
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
  cc: ControllerComponents,
)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport with HasDatabaseConfigProvider[JdbcProfile] {

  def index() = Action.async { implicit request: Request[AnyContent] =>
    locationDao.all().map(locations => Ok(views.html.location.showAll(locations)))
  }

  def addLocation() = Action { implicit request =>
    Ok(views.html.location.addLocation(AddLocationForm.form))
  }

  def addLocationPost() = Action { implicit request: Request[AnyContent] =>
    AddLocationForm.form.bindFromRequest.fold(
      formWithErrors => {
        BadRequest(views.html.location.addLocation(formWithErrors))
      },
      locationData => {
        locationDao.insert(Location(None, locationData.title, locationData.address, locationData.city, locationData.zipCode, locationData.state, locationData.country))
        Redirect(routes.HomeController.index()).flashing("success" -> "Location added!")
      }
    )
  }
}
