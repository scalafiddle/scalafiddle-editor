package scalafiddle.client

import org.scalajs.dom

import scala.scalajs.js.annotation.{JSExport, JSExportTopLevel}

@JSExportTopLevel("AppMain")
object AppMain {
  @JSExport
  def main(args: Array[String]): Unit = {
    AppRouter.router().renderIntoDOM(dom.document.getElementById("root"))
  }
}
