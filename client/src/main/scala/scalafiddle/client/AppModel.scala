package scalafiddle.client

import diode._
import diode.data.Pot

import scalafiddle.shared.{FiddleData, FiddleId}

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

case class CompilerData(
  status: CompilerStatus,
  jsCode: Option[String],
  annotations: Seq[EditorAnnotation],
  errorMessage: Option[String],
  log: String
)

case class AppModel(
  navLocation: Page,
  fiddleId: Option[FiddleId],
  fiddleData: Pot[FiddleData],
  compilerData: CompilerData
)

case class NavigateTo(page: Page) extends Action

case class NavigateSilentTo(page: Page) extends Action
