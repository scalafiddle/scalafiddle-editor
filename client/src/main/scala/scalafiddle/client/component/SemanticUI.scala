package scalafiddle.client.component

import scala.language.implicitConversions
import scala.scalajs.js

object SemanticUI {
  @js.native
  trait SemanticJQuery extends JQuery {
    def accordion(): SemanticJQuery = js.native
    def dropdown(params: js.Any*): SemanticJQuery = js.native
    def checkbox(): SemanticJQuery = js.native
  }

  implicit def jq2bootstrap(jq: JQuery): SemanticJQuery = jq.asInstanceOf[SemanticJQuery]
}
