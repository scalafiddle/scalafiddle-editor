package scalafiddle.client

import diode.ActionResult.{ModelUpdate, ModelUpdateSilent}
import diode._
import diode.data.{Pot, Ready}
import diode.react.ReactConnector

import scalafiddle.client.AppRouter.Home

object AppCircuit extends Circuit[AppModel] with ReactConnector[AppModel] {
  override protected def initialModel = AppModel(Home, None, Ready(
    FiddleData("Testing", "desc", "println(5)", Seq.empty, Seq(
      Library("DOM", "", "scalajs-dom", "0.9.1", false, Nil),
      Library("Scalatags", "", "scalatags", "0.5.4", false, Nil)
    ), Seq(
      Library("Cats", "", "cats", "0.7.0", false, Nil),
      Library("Cats", "", "cats", "0.6.0", false, Nil),
      Library("Cats", "", "cats", "0.5.0", false, Nil),
      Library("Scala.js React", "", "scalajs-react", "0.11.2", false, Nil)
    )))
  )

  override protected def actionHandler = composeHandlers(
    new FiddleHandler(zoomRW(_.fiddleData)((m, v) => m.copy(fiddleData = v))),
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
