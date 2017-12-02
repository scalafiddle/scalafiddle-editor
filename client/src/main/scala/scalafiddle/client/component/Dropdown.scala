package scalafiddle.client.component

import japgolly.scalajs.react._
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLElement, MouseEvent}
import scalajs.js

object Dropdown {
  import japgolly.scalajs.react.vdom.all._

  case class State(isOpen: Boolean = false)

  case class Props(classes: String, buttonContent: VdomNode, content: (() => Callback) => VdomNode)

  case class Backend($ : BackendScope[Props, State]) {
    def render(props: Props, state: State) = {
      div(cls := s"ui dropdown ${props.classes} ${if (state.isOpen) "active visible" else ""}", onClick ==> {
        (e: ReactEventFromHtml) =>
          dropdownClicked(e, state.isOpen)
      })(
        props.buttonContent,
        props.content(() => closeDropdown()).when(state.isOpen)
      )
    }

    val closeFn: js.Function1[MouseEvent, _] = (e: MouseEvent) => closeDropdown(e)

    def dropdownClicked(e: ReactEventFromHtml, isOpen: Boolean): Callback = {
      if (!isOpen) {
        Callback {
          dom.document.addEventListener("click", closeFn)
        } >> $.modState(s => s.copy(isOpen = true))
      } else {
        Callback.empty
      }
    }

    def closeDropdown(e: MouseEvent): Unit = {
      val state = $.state.runNow()
      val node  = $.getDOMNode.runNow().asInstanceOf[HTMLElement]
      if (state.isOpen && !node.contains(e.target.asInstanceOf[HTMLElement])) {
        closeDropdown().runNow()
      }
    }

    def closeDropdown(): Callback = {
      dom.document.removeEventListener("click", closeFn)
      $.modState(s => s.copy(isOpen = false))
    }
  }

  val component = ScalaComponent
    .builder[Props]("FiddleEditor")
    .initialState(State())
    .renderBackend[Backend]
    .build

  def apply(classes: String, buttonContent: VdomNode)(content: (() => Callback) => VdomNode) =
    component(Props(classes, buttonContent, content))
}
