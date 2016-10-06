package scalafiddle.client.component

import japgolly.scalajs.react._
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLElement, MouseEvent}
import scalajs.js

object Dropdown {
  import japgolly.scalajs.react.vdom.all._

  case class State(isOpen: Boolean = false)

  case class Props(classes: String, buttonContent: ReactNode, content: (() => Callback) => ReactNode)

  case class Backend($: BackendScope[Props, State]) {
    def render(props: Props, state: State, children: PropsChildren) = {
      div(cls := s"ui dropdown ${props.classes} ${if(state.isOpen) "active visible" else ""}", onClick ==> { (e: ReactEventH) => dropdownClicked(e, state.isOpen) })(
        props.buttonContent,
        state.isOpen ?= props.content(closeDropdown)
      )
    }

    val closeFn: js.Function1[MouseEvent, _] = (e: MouseEvent) => closeDropdown(e)

    def dropdownClicked(e: ReactEventH, isOpen: Boolean): Callback = {
      if (!isOpen) {
        Callback {
          dom.document.addEventListener("click", closeFn)
        } >> $.modState(s => s.copy(isOpen = true))
      } else {
        Callback.empty
      }
    }

    def closeDropdown(e: MouseEvent): Unit = {
      if($.accessDirect.state.isOpen && !$.getDOMNode().asInstanceOf[HTMLElement].contains(e.target.asInstanceOf[HTMLElement])) {
        closeDropdown().runNow()
      }
    }

    def closeDropdown(): Callback = {
      dom.document.removeEventListener("click", closeFn)
      $.modState(s => s.copy(isOpen = false))
    }
  }

  val component = ReactComponentB[Props]("FiddleEditor")
    .initialState_P(props => State())
    .renderBackend[Backend]
    .build

  def apply(classes: String, buttonContent: ReactNode)(content: (() => Callback) => ReactNode) = component(Props(classes, buttonContent, content))
}
