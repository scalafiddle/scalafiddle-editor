package controllers

import upickle.default._
import upickle.{Js,json}
import com.google.inject.Inject
import play.api.{Configuration, Environment}
import play.api.mvc._

import scalafiddle.shared.Api
import scala.concurrent.ExecutionContext.Implicits.global
import scalafiddle.server.ApiService

object Router extends autowire.Server[Js.Value, Reader, Writer] {
  override def read[R: Reader](p: Js.Value) = readJs[R](p)
  override def write[R: Writer](r: R) = writeJs(r)
}

class Application @Inject() (implicit val config: Configuration, env: Environment) extends Controller {
  val apiService = new ApiService()

  def index = Action {
    Ok(views.html.index("ScalaFiddle"))
  }

  def resultFrame = Action { request =>
    Ok(views.html.resultframe()).withHeaders(CACHE_CONTROL -> "max-age=86400")
  }

  def autowireApi(path: String) = Action.async(parse.raw) {
    implicit request =>
      println(s"Request path: $path")

      // get the request body as JSON
      val b = request.body.toString()

      // call Autowire route
      Router.route[Api](apiService)(
        autowire.Core.Request(path.split("/"), json.read(b).asInstanceOf[Js.Obj].value.toMap)
      ).map(buffer => {
        val data = write(buffer)
        Ok(data)
      })
  }
}
