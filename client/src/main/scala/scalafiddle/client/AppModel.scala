package scalafiddle.client

import diode._
import diode.data.Pot

import scalafiddle.shared._

case class EditorAnnotation(row: Int, col: Int, text: Seq[String], tpe: String)

sealed trait CompilerStatus {
  def show: String
}

object CompilerStatus {
  case object Result extends CompilerStatus {
    val show = "RESULT"
  }
  case object Compiling extends CompilerStatus {
    val show = "COMPILING"
  }
  case object Compiled extends CompilerStatus {
    val show = "COMPILED"
  }
  case object Running extends CompilerStatus {
    val show = "RUNNING"
  }
  case object Error extends CompilerStatus {
    val show = "ERROR"
  }
}

sealed trait OutputData

case class CompilerData(
    status: CompilerStatus,
    jsCode: Option[String],
    annotations: Seq[EditorAnnotation],
    errorMessage: Option[String],
    log: String
) extends OutputData

case class UserFiddleData(
    fiddles: Seq[FiddleVersions]
) extends OutputData

case class ScalaFiddleHelp(url: String) extends OutputData

case class LoginData(
    userInfo: Pot[UserInfo],
    loginProviders: Pot[Seq[LoginProvider]]
)

case class AppModel(
    navLocation: Page,
    fiddleId: Option[FiddleId],
    fiddleData: FiddleData,
    outputData: OutputData,
    loginData: LoginData
)

case class NavigateTo(page: Page) extends Action

case class NavigateSilentTo(page: Page) extends Action
