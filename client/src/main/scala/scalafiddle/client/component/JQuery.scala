package scalafiddle.client.component

import org.scalajs.dom._

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal

/**
  * Minimal facade for JQuery. Use https://github.com/scala-js/scala-js-jquery or
  * https://github.com/jducoeur/jquery-facade for more complete one.
  */
@js.native
trait JQueryEventObject extends Event {
  var data: js.Any = js.native
}

@js.native
@JSGlobal("jQuery")
object JQueryStatic extends js.Object {
  def apply(selector: js.Any): JQuery = js.native
}

@js.native
trait JQuery extends js.Object {}
