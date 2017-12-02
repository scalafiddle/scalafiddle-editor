package scalafiddle.client.component

import diode.ModelR
import diode.data.Ready
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{BackendScope, _}

import scalafiddle.client.{AppCircuit, AppModel, LoadUserFiddles, LoginData}

object UserLogin {

  case class Props(loginData: ModelR[AppModel, LoginData])

  case class Backend($ : BackendScope[Props, Unit]) {
    var unsubscribe: () => Unit = () => ()

    def render(props: Props): VdomElement = {
      val loginData = props.loginData()
      loginData.userInfo match {
        case Ready(userInfo) if userInfo.loggedIn =>
          div(cls := "userinfo")(
            img(cls := "author", src := userInfo.avatarUrl.getOrElse("/assets/images/anon.png")),
            Dropdown("top right pointing", div(cls := "username", userInfo.name))(
              closeCB =>
                div(cls := "ui vertical menu", display.block)(
                  a(cls := "item", onClick --> { Callback(AppCircuit.dispatch(LoadUserFiddles)) >> closeCB() })("My fiddles")
              )),
            a(href := "/signout", div(cls := "ui basic button", "Sign out"))
          )
        case Ready(userInfo) if !userInfo.loggedIn =>
          loginData.loginProviders match {
            case Ready(loginProviders) if loginProviders.size == 1 =>
              val provider = loginProviders.head
              a(href := s"/authenticate/${provider.id}")(
                div(cls := "ui button login")(
                  img(src := provider.logoUrl),
                  s"Sign in with ${provider.name}"
                ))
            case Ready(loginProviders) =>
              Dropdown("top basic button embed-options", span("Sign in", Icon.caretDown))(_ =>
                div(cls := "menu", display.block)(div("Login providers")))
            case _ =>
              div(img(src := "/assets/images/wait-ring.gif"))
          }
        case _ =>
          div(img(src := "/assets/images/wait-ring.gif"))
      }
    }

    def mounted(props: Props): Callback = {
      Callback {
        // subscribe to changes in compiler data
        unsubscribe = AppCircuit.subscribe(props.loginData)(_ => $.forceUpdate.runNow())
      }
    }

    def unmounted: Callback = {
      Callback(unsubscribe())
    }
  }

  val component = ScalaComponent
    .builder[Props]("UserLogin")
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .componentWillUnmount(scope => scope.backend.unmounted)
    .build

  def apply(loginInfo: ModelR[AppModel, LoginData]) = component(Props(loginInfo))
}
