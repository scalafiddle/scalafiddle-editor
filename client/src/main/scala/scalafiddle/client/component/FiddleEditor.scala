package scalafiddle.client.component

import diode.NoAction
import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react._
import org.scalajs.dom

import scala.scalajs.js
import scala.scalajs.js.Dynamic._
import scala.scalajs.js.{Dynamic => Dyn}
import scalafiddle.client.{AutoCompleteFiddle, FiddleData, FiddleId, JsVal}

object FiddleEditor {
  import japgolly.scalajs.react.vdom.Implicits._

  val editorRef = Ref("editor")

  case class EditorBinding(name: String, keys: String, action: () => Any)

  case class Backend($: BackendScope[Props, State]) {
    def render(props: Props, state: State) = {
      import japgolly.scalajs.react.vdom.all._
      div(cls := "full-screen")(
        header(
          div(cls := "logo")(
            a(href := "#")(
              img(src := "/assets/images/scalafiddle-logo.png", alt := "ScalaFiddle")
            )
          ),
          div(cls := "ui basic button")(Icon.play, "Run"),
          div(cls := "ui basic button")(Icon.pencil, "Save"),
          div(cls := "ui basic button")(Icon.codeFork, "Fork"),
          div(cls := "ui basic button")("Embed", Icon.caretDown)
        ),
        div(cls := "main")(
          Sidebar(props.data),
          div(cls := "editor-area")(
            div(cls := "editor")(
              div(cls := "label")("SCALA", i(cls := "icon setting")),
              div(id := "editor", ref := editorRef)
            ),
            div(cls := "output")(
              div(cls := "label", "RESULT"),
              iframe(
                id := "resultframe",
                width := "100%",
                height := "100%",
                frameBorder := "0",
                sandbox := "allow-scripts",
                src := s"resultframe?theme=light")
            )
          )
        )
      )
    }

    def complete(editor: Dyn): Unit = {
      editor.completer.showPopup(editor)
      // needed for firefox on mac
      editor.completer.cancelContextMenu()
    }

    def mounted(editorElement: dom.Element, props: Props): Callback = {
      import JsVal.jsVal2jsAny
      // create the Ace editor and configure it
      val Autocomplete = global.require("ace/autocomplete").Autocomplete
      val completer = Dyn.newInstance(Autocomplete)()
      val editor: Dyn = global.ace.edit(editorElement)

      editor.setTheme("ace/theme/eclipse")
      editor.getSession().setMode("ace/mode/scala")
      editor.getSession().setTabSize(2)
      editor.setShowPrintMargin(false)
      editor.getSession().setOption("useWorker", false)
      editor.updateDynamic("completer")(completer) // because of SI-7420

      val bindings = Seq(
        EditorBinding("Compile", "Enter", () => NoAction),
        EditorBinding("FullOptimize", "Shift-Enter", () => NoAction),
        EditorBinding("Save", "S", () => NoAction),
        EditorBinding("Complete", "Space", () => complete(editor))
      )
      for (EditorBinding(name, key, func) <- bindings) {
        val binding = s"Ctrl-$key|Cmd-$key"
        editor.commands.addCommand(JsVal.obj(
          "name" -> name,
          "bindKey" -> JsVal.obj(
            "win" -> binding,
            "mac" -> binding,
            "sender" -> "editor|cli"
          ),
          "exec" -> func
        ))
      }

      editor.completers = js.Array(JsVal.obj(
        "getCompletions" -> { (editor: Dyn, session: Dyn, pos: Dyn, prefix: Dyn, callback: Dyn) => {
          def applyResults(results: Seq[(String, String)]): Unit = {
            val aceVersion = results.map { case (name, value) =>
              JsVal.obj(
                "value" -> value,
                "caption" -> (value + name)
              ).value
            }
            callback(null, js.Array(aceVersion: _*))
          }
          // dispatch an action to fetch completion results
          props.data.dispatch(AutoCompleteFiddle(
            session.getValue().asInstanceOf[String],
            pos.row.asInstanceOf[Int],
            pos.column.asInstanceOf[Int],
            applyResults)
          ).runNow()
        }
        }
      ).value)

      $.setState(State(editor))
    }
  }

  val component = ReactComponentB[Props]("FiddleEditor")
    .initialState(State(js.Dynamic.literal("empty" -> "empty")))
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(ReactDOM.findDOMNode(scope.refs(editorRef).get), scope.props))
    .build

  case class Props(data: ModelProxy[Pot[FiddleData]], fiddleId: Option[FiddleId])

  case class State(editor: js.Dynamic)

  def apply(data: ModelProxy[Pot[FiddleData]], fiddleId: Option[FiddleId]) = component(Props(data, fiddleId))
}
