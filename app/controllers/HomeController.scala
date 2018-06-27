package controllers

import dao.{EventDAO, OccurrenceDAO}
import javax.inject._
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(
  eventDao: EventDAO,
  occurrenceDao: OccurrenceDAO,
  cc: ControllerComponents
)(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index(): Action[AnyContent] = Action.async { implicit request: Request[AnyContent] =>
    val data = for {
      upcomingOccurrences <- occurrenceDao.getUpcoming(7)
      upcoming <- eventDao.getByOccurrences(upcomingOccurrences)
      laterOccurrences <- occurrenceDao.getUpcoming(30, Option(7))
      later <- eventDao.getByOccurrences(laterOccurrences)
    } yield (upcoming, later)
    data.map(data => Ok(views.html.home.index(data._1, data._2)))
  }
}
