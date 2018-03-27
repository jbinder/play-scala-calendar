package controllers

import dao.EventDAO
import javax.inject._
import play.api.mvc._

import scala.concurrent.ExecutionContext

/**
 * This controller creates an `Action` to handle HTTP requests to the
 * application's home page.
 */
@Singleton
class HomeController @Inject()(eventDao: EventDAO, cc: ControllerComponents)(implicit executionContext: ExecutionContext) extends AbstractController(cc) {

  /**
   * Create an Action to render an HTML page.
   *
   * The configuration in the `routes` file means that this method
   * will be called when the application receives a `GET` request with
   * a path of `/`.
   */
  def index() = Action.async { implicit request: Request[AnyContent] =>
    val data = for {
      upcoming <- eventDao.getUpcoming(7)
      later <- eventDao.getUpcoming(365, Option(7))
    } yield (upcoming, later)
    data.map(data => Ok(views.html.home.index(data._1, data._2)))
  }
}
