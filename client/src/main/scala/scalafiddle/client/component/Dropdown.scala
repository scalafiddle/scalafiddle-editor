package scalafiddle.client.component

import japgolly.scalajs.react._
import org.scalajs.dom
import org.scalajs.dom.raw.{HTMLElement, MouseEvent}

object Dropdown {
  import japgolly.scalajs.react.vdom.all._

  case class State(isOpen: Boolean = false)

  case class Props(classes: String, content: ReactNode)

  case class Backend($: BackendScope[Props, State]) {
    def render(props: Props, state: State, children: PropsChildren) = {
      div(cls := s"ui dropdown ${props.classes} ${if(state.isOpen) "active visible" else ""}", onClick ==> { (e: ReactEventH) => dropdownClicked(e, state.isOpen) })(
        props.content,
        div(cls := "menu", if(state.isOpen) display.block else display.none)(children)
      )
    }

    def dropdownClicked(e: ReactEventH, isOpen: Boolean): Callback = {
      if (!isOpen) {
        Callback {
          dom.document.addEventListener("click", closeDropdown _)
        } >> $.modState(s => s.copy(isOpen = true))
      } else {
        Callback.empty
      }
    }

    def closeDropdown(e: MouseEvent): Unit = {
      if($.accessDirect.state.isOpen && !$.getDOMNode().asInstanceOf[HTMLElement].contains(e.target.asInstanceOf[HTMLElement])) {
        dom.document.removeEventListener("click", closeDropdown _)
        $.modState(s => s.copy(isOpen = false)).runNow()
      }
    }
  }

  val component = ReactComponentB[Props]("FiddleEditor")
    .initialState_P(props => State())
    .renderBackend[Backend]
    .build

  def apply(classes: String, content: ReactNode)(children: ReactNode*) = component(Props(classes, content), children: _*)
}
