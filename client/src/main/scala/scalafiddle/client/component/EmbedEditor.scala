package scalafiddle.client.component

import japgolly.scalajs.react._
import org.scalajs.dom.raw.HTMLTextAreaElement

import scalafiddle.client.ScalaFiddleConfig
import scalafiddle.shared.FiddleId
import scala.scalajs.js

object EmbedEditor {
  import japgolly.scalajs.react.vdom.Implicits._

  sealed trait Layout {
    def split: Int
    def updated(split: Int): Layout
  }

  case class Horizontal(split: Int) extends Layout {
    override def updated(split: Int): Layout = Horizontal(split)
  }

  case class Vertical(split: Int) extends Layout {
    override def updated(split: Int): Layout = Vertical(split)
  }

  val themes = Seq("light", "dark")

  case class State(theme: String, layout: Layout)

  case class Props(fiddleId: FiddleId)

  case class Backend($ : BackendScope[Props, State]) {
    def render(props: Props, state: State) = {
      import japgolly.scalajs.react.vdom.all._

      def createIframeSource: String = {
        val src = new StringBuilder(s"${ScalaFiddleConfig.compilerURL}/embed?sfid=${props.fiddleId}")
        state.theme match {
          case "light" =>
          case theme   => src.append(s"&theme=$theme")
        }
        state.layout match {
          case Horizontal(50)    =>
          case Horizontal(split) => src.append(s"&layout=h$split")
          case Vertical(split)   => src.append(s"&layout=v$split")
        }
        src.toString()
      }

      def createEmbedCode: String = {
        s"""<iframe height="300" frameborder="0" style="width: 100%; overflow: hidden;" src="$createIframeSource"></iframe>"""
      }

      div(cls := "embed-editor")(
        div(cls := "options")(
          div(cls := "ui form")(
            div(cls := "header", "Theme"),
            div(cls := "grouped fields")(
              themes.toTagMod { theme =>
                div(cls := "field")(
                  div(cls := "ui radio checkbox")(
                    input.radio(name := "theme", checked := (state.theme == theme), onChange --> themeChanged(theme)),
                    label(theme)
                  )
                )
              }
            ),
            div(cls := "header", "Layout"),
            div(cls := "two fields")(
              div(cls := "grouped fields")(
                label(`for` := "layout")("Direction"),
                div(cls := "field")(
                  div(cls := "ui radio checkbox")(
                    input.radio(name := "layout",
                                checked := state.layout.isInstanceOf[Horizontal],
                                onChange --> layoutChanged(Horizontal(state.layout.split))),
                    label("Horizontal")
                  )
                ),
                div(cls := "field")(
                  div(cls := "ui radio checkbox")(
                    input.radio(name := "layout",
                                checked := state.layout.isInstanceOf[Vertical],
                                onChange --> layoutChanged(Vertical(state.layout.split))),
                    label("Vertical")
                  )
                )
              ),
              div(cls := "field")(
                label(`for` := "split")("Split"),
                input.range(min := 15, max := 85, value := state.layout.split, onChange ==> splitChanged)
              )
            ),
            div(cls := "field")(
              div(cls := "header", "Embed code"),
              textarea(
                cls := "embed-code",
                defaultValue := createEmbedCode,
                onClick ==> { (e: ReactEventFromHtml) =>
                  e.target.focus(); e.target.asInstanceOf[HTMLTextAreaElement].select(); Callback.empty
                }
              )
            )
          )
        ),
        div(cls := "preview")(
          div(cls := "header", "Preview"),
          iframe(
            height := "300px",
            width := "100%",
            frameBorder := 0,
            style := js.Dictionary("width" -> "100%", "overflow" -> "hidden"),
            src := s"$createIframeSource&preview=true"
          )
        )
      )
    }

    def themeChanged(theme: String): Callback = {
      $.modState(_.copy(theme = theme))
    }

    def layoutChanged(layout: Layout): Callback = {
      $.modState(_.copy(layout = layout))
    }

    def splitChanged(e: ReactEventFromInput): Callback = {
      val split = e.target.value.toInt
      $.modState(s => s.copy(layout = s.layout.updated(split = split)))
    }

  }

  val component = ScalaComponent
    .builder[Props]("EmbedEditor")
    .initialState(State("light", Horizontal(50)))
    .renderBackend[Backend]
    .build

  def apply(fiddleId: FiddleId) = component(Props(fiddleId))
}
