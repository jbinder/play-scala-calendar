package controllers

import dao.{EventDAO, SeriesDAO}
import forms.AddSeriesForm
import javax.inject._
import play.api.data.Form
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import play.api.mvc._
import play.twirl.api.Html
import slick.jdbc.JdbcProfile

import scala.concurrent.{ExecutionContext, Future}

@Singleton
class SeriesController @Inject()(
  protected val dbConfigProvider: DatabaseConfigProvider,
  seriesDao: SeriesDAO,
  eventDao: EventDAO,
  cc: ControllerComponents,
)(implicit executionContext: ExecutionContext) extends AbstractController(cc) with play.api.i18n.I18nSupport with HasDatabaseConfigProvider[JdbcProfile] {

  def index(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    seriesDao.all().map(series => Ok(views.html.series.showAll(series)))
  }

  def viewSeries(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    seriesDao.get(id).map(series => Ok(views.html.series.viewSeries(series.get)))
  }

  def addSeries(): Action[AnyContent] = Action.async { implicit request =>
    getAddSeriesView(AddSeriesForm.form, Option.empty).map(html => Ok(html))
  }

  def addSeriesPost(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    AddSeriesForm.form.bindFromRequest.fold(
      formWithErrors => {
        getAddSeriesView(formWithErrors, Option.empty).map(html => BadRequest(html))
      },
      seriesData => {
        seriesDao.insert(seriesData)
        Future { Redirect(routes.HomeController.index()).flashing("success" -> "Series added!") }
      }
    )
  }

  def editSeries(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    seriesDao.get(id).map(data => {
      val series = data.get
      getAddSeriesView(AddSeriesForm.form.fill(AddSeriesForm.Data(
        series.duration,
        series.startsAt.toDate,
        series.startsAt.hourOfDay().get(),
        series.startsAt.minuteOfHour().get(),
        series.endsAt.map(_.toDate),
        series.freq,
        series.byDay,
        series.interval,
        series.eventId
      )), Option(id)).map(html => Ok(html))
    }).flatten
  }

  def editSeriesPost(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    AddSeriesForm.form.bindFromRequest.fold(
      formWithErrors => {
        getAddSeriesView(formWithErrors, Option(id)).map(html => BadRequest(html))
      },
      seriesData => {
        seriesDao.update(id, seriesData)
        Future { Redirect(routes.HomeController.index()).flashing("success" -> "Series edited!") }
      }
    )
  }

  def deleteSeries(id: Long): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] => {
    seriesDao.delete(id).map(_ => Redirect(routes.HomeController.index()).flashing("success" -> "Series deleted!"))
  }}

  private def getAddSeriesView(form: Form[AddSeriesForm.Data], id: Option[Long])(implicit request: Request[AnyContent]): Future[Html] = {
    eventDao.all().map(events => views.html.series.addSeries(form,
      eventDao.toOptionsList(events.map(_._1)),
      (0 to 23).map(x => (x.toString, x.toString)),
      (0 to 59 by 15).map(x => (x.toString, x.toString)),
      id
    ))
  }
}
