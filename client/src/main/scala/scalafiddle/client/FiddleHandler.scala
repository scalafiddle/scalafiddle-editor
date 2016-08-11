package scalafiddle.client

import diode._
import diode.data.Pot

case class SelectLibrary(lib: Library) extends Action

case class DeselectLibrary(lib: Library) extends Action

class FiddleHandler[M](modelRW: ModelRW[M, Pot[FiddleData]]) extends ActionHandler(modelRW) {

  override def handle = {
    case SelectLibrary(lib) =>
      updated(value.map(fd => fd.copy(libraries = fd.libraries :+ lib)))

    case DeselectLibrary(lib) =>
      updated(value.map(fd => fd.copy(libraries = fd.libraries.filterNot(_ == lib))))
  }
}
