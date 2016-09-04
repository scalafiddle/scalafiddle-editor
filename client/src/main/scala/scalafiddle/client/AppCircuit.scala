package scalafiddle.client

import diode.ActionResult.{ModelUpdate, ModelUpdateSilent}
import diode._
import diode.data.{Empty, Ready}
import diode.react.ReactConnector
import upickle.default._

import scala.scalajs.js
import scala.scalajs.js.JSON
import scalafiddle.client.AppRouter.Home
import scalafiddle.shared._

object AppCircuit extends Circuit[AppModel] with ReactConnector[AppModel] {
  // load from global config
  def fiddleData = read[FiddleData](JSON.stringify(js.Dynamic.global.ScalaFiddleData))

  override protected def initialModel =
    AppModel(Home, None, fiddleData, CompilerData(CompilerStatus.Result, None, Seq.empty, None, ""), LoginData(Empty, Empty))

  override protected def actionHandler = composeHandlers(
    new FiddleHandler(zoomRW(_.fiddleData)((m, v) => m.copy(fiddleData = v)), zoomRW(_.fiddleId)((m, v) => m.copy(fiddleId = v))),
    new CompilerHandler(zoomRW(_.compilerData)((m, v) => m.copy(compilerData = v))),
    new LoginHandler(zoomRW(_.loginData)((m, v) => m.copy(loginData = v))),
    navigationHandler
  )

  override def handleFatal(action: Any, e: Throwable): Unit = {
    println(s"Error handling action: $action")
    println(e.toString)
  }

  override def handleError(e: String): Unit = {
    println(s"Error in circuit: $e")
  }

  val navigationHandler: (AppModel, Any) => Option[ActionResult[AppModel]] = (model, action) => action match {
    case NavigateTo(page) =>
      Some(ModelUpdate(model.copy(navLocation = page)))
    case NavigateSilentTo(page) =>
      Some(ModelUpdateSilent(model.copy(navLocation = page)))
    case _ =>
      None
  }
}
