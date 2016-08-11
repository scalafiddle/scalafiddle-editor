package scalafiddle.client

import diode._
import diode.data.Pot

case class FiddleId(id: String, version: Int)

case class Library(name: String, group: String, artifact: String, version: String, compileTimeOnly: Boolean, dependencies: Seq[Library])

case class FiddleData(
  name: String,
  description: String,
  origSource: String,
  libraries: Seq[Library],
  forced: Seq[Library],
  available: Seq[Library]
)

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
