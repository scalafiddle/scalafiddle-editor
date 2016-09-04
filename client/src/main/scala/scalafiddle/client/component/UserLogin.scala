package scalafiddle.client.component

import diode.{ModelR, ModelRO}
import diode.data.Ready
import japgolly.scalajs.react.vdom.all._
import japgolly.scalajs.react.{BackendScope, ReactComponentB, _}

import scalafiddle.client.{AppCircuit, AppModel, LoginData}

object UserLogin {

  case class Props(loginData: ModelR[AppModel, LoginData])

  case class Backend($: BackendScope[Props, Unit]) {
    var unsubscribe: () => Unit = () => ()

    def render(props: Props): ReactElement = {
      val loginData = props.loginData()
      loginData.userInfo match {
        case Ready(userInfo) if userInfo.loggedIn =>
          div(cls := "userinfo")(
            img(src := userInfo.avatarUrl.getOrElse("/assets/images/anon.png")),
            div(cls :="username", userInfo.name),
            a(href := "/signout", div(cls := "ui basic button", "Sign out"))
          )
        case Ready(userInfo) if !userInfo.loggedIn =>
          loginData.loginProviders match {
            case Ready(loginProviders) if loginProviders.size == 1 =>
              val provider = loginProviders.head
              a(href := s"/authenticate/${provider.id}")(div(cls := "ui blue button login")(
                img(src := provider.logoUrl),
                s"Sign in with ${provider.name}"
              ))
            case Ready(loginProviders) =>
              Dropdown("top basic button embed-options", span("Sign in", Icon.caretDown))(
                div(cls := "menu", display.block)(div("Login providers"))
              )
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

  val component = ReactComponentB[Props]("UserLogin")
    .renderBackend[Backend]
    .componentDidMount(scope => scope.backend.mounted(scope.props))
    .componentWillUnmount(scope => scope.backend.unmounted)
    .build

  def apply(loginInfo: ModelR[AppModel, LoginData]) = component(Props(loginInfo))
}
