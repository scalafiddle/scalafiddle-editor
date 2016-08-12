package scalafiddle.client

import diode.ActionResult.{ModelUpdate, ModelUpdateSilent}
import diode._
import diode.data.{Pot, Ready}
import diode.react.ReactConnector

import scalafiddle.client.AppRouter.Home

object AppCircuit extends Circuit[AppModel] with ReactConnector[AppModel] {
  def sourceCode =
    """import scalatags.JsDom.all._
      |import org.scalajs.dom
      |import fiddle.Fiddle, Fiddle.println
      |import scalajs.js
      |
      |object ScalaFiddle extends js.JSApp {
      |  def main() = {
      |    // $FiddleStart
      |    val textbox = input(placeholder := "message").render
      |    val result = div.render
      |
      |    textbox.onkeyup = (e: dom.Event) => {
      |      result.textContent = textbox.value
      |    }
      |
      |    val content = div(
      |      h2("Hello Scala.js"),
      |      textbox,
      |      result
      |      )
      |
      |    println(content)
      |    // $FiddleEnd
      |  }
      |}
    """.stripMargin

  override protected def initialModel = AppModel(Home, None, Ready(
    FiddleData("Testing", "desc", sourceCode, Seq.empty, Seq(
      Library("DOM", "", "scalajs-dom", "0.9.1", false, Nil),
      Library("Scalatags", "", "scalatags", "0.6.0", false, Nil)
    ), Seq(
      Library("Cats", "", "cats", "0.7.0", false, Nil),
      Library("Cats", "", "cats", "0.6.0", false, Nil),
      Library("Cats", "", "cats", "0.5.0", false, Nil),
      Library("Scala.js React", "", "scalajs-react", "0.11.2", false, Nil)
    ))),
    CompilerData(CompilerStatus.Result, None, Seq.empty, None, "")
  )

  override protected def actionHandler = composeHandlers(
    new FiddleHandler(zoomRW(_.fiddleData)((m, v) => m.copy(fiddleData = v))),
    new CompilerHandler(zoomRW(_.compilerData)((m, v) => m.copy(compilerData = v))),
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
