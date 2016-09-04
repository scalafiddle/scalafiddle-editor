package scalafiddle.client

import autowire._
import diode._
import diode.data.Ready

import scala.concurrent.ExecutionContext.Implicits.global
import scalafiddle.shared._

case object UpdateLoginInfo extends Action

case class UpdateLoginProviders(loginProviders: Seq[LoginProvider]) extends Action

case class UpdateUserInfo(userInfo: UserInfo) extends Action

class LoginHandler[M](modelRW: ModelRW[M, LoginData]) extends ActionHandler(modelRW) {
  override def handle = {
    case UpdateLoginInfo =>
      val loginProviders = Effect(AjaxClient[Api].loginProviders().call().map(UpdateLoginProviders))
      val userInfo = Effect(AjaxClient[Api].userInfo().call().map(UpdateUserInfo))
      effectOnly(loginProviders + userInfo)

    case UpdateLoginProviders(loginProviders) =>
      updated(value.copy(loginProviders = Ready(loginProviders)))

    case UpdateUserInfo(userInfo) =>
      updated(value.copy(userInfo = Ready(userInfo)))
  }
}
