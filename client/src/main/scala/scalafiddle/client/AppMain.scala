package scalafiddle.client

import japgolly.scalajs.react.ReactDOM
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.JSExport

@JSExport("AppMain")
object AppMain extends js.JSApp {
  @JSExport
  def main(): Unit = {
    ReactDOM.render(AppRouter.router(), dom.document.getElementById("root"))
  }
}
