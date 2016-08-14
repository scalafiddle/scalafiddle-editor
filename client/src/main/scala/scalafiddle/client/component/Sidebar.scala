package scalafiddle.client.component

import diode.NoAction
import diode.data.{Pot, Ready}
import diode.react.ModelProxy
import japgolly.scalajs.react._
import org.scalajs.dom.raw.HTMLDivElement

import scalafiddle.client._
import scalafiddle.shared._

object Sidebar {
  import japgolly.scalajs.react.vdom.all._
  import SemanticUI._

  val accordionRef = Ref[HTMLDivElement]("accordion")

  sealed trait LibMode

  case object ForcedLib extends LibMode

  case object SelectedLib extends LibMode

  case object AvailableLib extends LibMode

  case class Props(data: ModelProxy[Pot[FiddleData]]) {
    def dispatch = data.theDispatch
  }

  case class State(showAllVersions: Boolean)

  case class Backend($: BackendScope[Props, State]) {

    def render(props: Props, state: State) = {
      div(cls := "sidebar")(
        props.data() match {
          case Ready(fd) =>
            // filter the list of available libs
            val availableVersions = fd.available.filterNot(lib => fd.libraries.exists(_.name == lib.name))
            // hide alternative versions, if requested
            val available = if (state.showAllVersions)
              availableVersions
            else
              availableVersions.foldLeft(Vector.empty[Library]) {
                case (libs, lib) => if (libs.exists(l => l.name == lib.name)) libs else libs :+ lib
              }
            div(cls := "ui accordion", ref := accordionRef)(
              div(cls := "title large active", "Info", i(cls := "icon dropdown")),
              div(cls := "content active")(
                div(cls := "ui form")(
                  div(cls := "field")(
                    label("Name"),
                    input.text(placeholder := "Untitled", name := "name", value := fd.name,
                      onChange ==> {(e: ReactEventI) => props.dispatch(UpdateInfo(e.target.value, fd.description))})
                  ),
                  div(cls := "field")(
                    label("Description"),
                    input.text(placeholder := "Enter description", name := "description", value := fd.description,
                      onChange ==> {(e: ReactEventI) => props.dispatch(UpdateInfo(fd.name, e.target.value))})
                  )
                )
              ),
              div(cls := "title", "Libraries", i(cls := "icon dropdown")),
              div(cls := "content")(
                div(cls := "ui horizontal divider header", "Selected"),
                div(cls := "liblist")(
                  div(cls := "ui middle aligned divided list")(
                    fd.forced.map(renderLibrary(_, ForcedLib, props.dispatch)) ++
                      fd.libraries.map(renderLibrary(_, SelectedLib, props.dispatch))
                  )
                ),
                div(cls := "ui horizontal divider header", "Available"),
                div(cls := "liblist")(
                  div(cls := "ui middle aligned divided list")(
                    available.map(renderLibrary(_, AvailableLib, props.dispatch))
                  )
                ),
                div(cls := "ui checkbox")(
                  input.checkbox(name := "all-versions", checked := state.showAllVersions,
                    onChange --> $.modState(s => s.copy(showAllVersions = !s.showAllVersions))),
                  label("Show all versions")
                )
              )
            )
          case _ =>
            div(cls := "ui center aligned basic segment")(
              div(cls := "ui active inline loader"),
              div(cls := "ui text", "Loading")
            )
        }
      )
    }

    def renderLibrary(lib: Library, mode: LibMode, dispatch: Any => Callback) = {
      val (action, icon) = mode match {
        case SelectedLib => (DeselectLibrary(lib), i(cls := "remove red icon"))
        case AvailableLib => (SelectLibrary(lib), i(cls := "plus green icon"))
        case ForcedLib => (NoAction, i(cls := "remove grey icon"))
      }
      div(ref := s"${lib.name}${lib.version}", cls := "item")(
        div(cls := "right floated")(
          button(cls := s"mini ui icon basic button ${if (mode == ForcedLib) "disabled" else ""}", onClick --> dispatch(action))(icon)
        ),
        div(cls := "content left floated", b(lib.name), " ", span(cls := "text grey", lib.version))
      )
    }
  }

  val component = ReactComponentB[Props]("Sidebar")
    .initialState(State(false))
    .renderBackend[Backend]
    .componentDidMount(scope => Callback {
      val accordionNode = scope.refs(accordionRef).get
      JQueryStatic(ReactDOM.findDOMNode(accordionNode)).accordion()
    })
    .build

  def apply(data: ModelProxy[Pot[FiddleData]]) = component(Props(data))
}
