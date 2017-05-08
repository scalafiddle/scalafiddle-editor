package scalafiddle.client

import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("AppMain")
object AppMain extends js.JSApp {
  @JSExport
  def main(): Unit = {
    AppRouter.router().renderIntoDOM(dom.document.getElementById("root"))
  }
}
