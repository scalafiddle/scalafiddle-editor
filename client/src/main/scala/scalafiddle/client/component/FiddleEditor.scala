package scalafiddle.client.component

import diode.data.Pot
import diode.react.ModelProxy
import japgolly.scalajs.react._

import scalafiddle.client.{FiddleData, FiddleId}

object FiddleEditor {
  import japgolly.scalajs.react.vdom.all._

  case class Backend($: BackendScope[Props, Unit]) {
    def render(P: Props) = {
      /*
            <header>
              <div class="logo"><a href="#"><img src="@routes.Assets.versioned("images/scalafiddle-logo.png")" alt="ScalaFiddle"></a></div>
                <div class="ui basic button"><i class="icon play"></i>Run</div>
                <div class="ui basic button"><i class="icon write"></i>Save</div>
                <div class="ui basic button"><i class="icon fork"></i>Fork</div>
                <div class="ui basic button">Embed<i class="icon caret down"></i></div>
              </header>
      */
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
        div(cls:="main")(
          Sidebar(P.data)
        )
      )
    }
  }

  val component = ReactComponentB[Props]("FiddleEditor")
    .renderBackend[Backend]
    .build

  case class Props(data: ModelProxy[Pot[FiddleData]], fiddleId: Option[FiddleId])

  def apply(data: ModelProxy[Pot[FiddleData]], fiddleId: Option[FiddleId]) = component(Props(data, fiddleId))
}
