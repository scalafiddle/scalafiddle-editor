package scalafiddle.client

import diode._
import diode.data.Pot

case class FiddleId(id: String, version: Int)

case class Library(name: String, group: String, artifact: String, version: String, compileTimeOnly: Boolean, dependencies: Seq[Library])

case class FiddleData(
  name: String,
  description: String,
  source: String,
  libraries: Seq[Library],
  forced: Seq[Library],
  available: Seq[Library]
)

case class AppModel(
  navLocation: Page,
  fiddleId: Option[FiddleId],
  fiddleData: Pot[FiddleData]
)

case class NavigateTo(page: Page) extends Action

case class NavigateSilentTo(page: Page) extends Action
