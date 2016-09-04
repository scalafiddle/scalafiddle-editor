package scalafiddle.client

import autowire._
import diode.ActionResult.{ModelUpdate, ModelUpdateEffect}
import diode._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafiddle.shared._

case class SelectLibrary(lib: Library) extends Action

case class DeselectLibrary(lib: Library) extends Action

case class UpdateInfo(name: String, description: String) extends Action

case class UpdateSource(source: String) extends Action

case class SaveFiddle(source: String) extends Action

case class UpdateFiddle(source: String) extends Action

case class ForkFiddle(source: String) extends Action

case class UpdateId(fiddleId: FiddleId, silent: Boolean = false) extends Action

class FiddleHandler[M](modelRW: ModelRW[M, FiddleData], fidRW: ModelRW[M, Option[FiddleId]]) extends ActionHandler(modelRW) {

  override def handle = {
    case SelectLibrary(lib) =>
      updated(value.copy(libraries = value.libraries :+ lib))

    case DeselectLibrary(lib) =>
      updated(value.copy(libraries = value.libraries.filterNot(_ == lib)))

    case UpdateInfo(name, description) =>
      updated(value.copy(name = name, description = description))

    case UpdateSource(source) =>
      updated(value.copy(sourceCode = source))

    case SaveFiddle(source) =>
      val newFiddle = value.copy(sourceCode = source, available = Seq.empty)
      val save: Future[Action] = AjaxClient[Api].save(newFiddle).call().map {
        case Right(fid) =>
          UpdateId(fid)
        case Left(error) =>
          NoAction
      }
      updated(value.copy(sourceCode = source), Effect(save))

    case UpdateFiddle(source) =>
      if (fidRW().isDefined) {
        val newFiddle = value.copy(sourceCode = source, available = Seq.empty)
        val save: Future[Action] = AjaxClient[Api].update(newFiddle, fidRW().get.id).call().map {
          case Right(fid) =>
            UpdateId(fid)
          case Left(error) =>
            NoAction
        }
        updated(value.copy(sourceCode = source), Effect(save))
      } else {
        noChange
      }

    case ForkFiddle(source) =>
      if (fidRW().isDefined) {
          val newFiddle = value.copy(sourceCode = source, available = Seq.empty)
          val save: Future[Action] = AjaxClient[Api].fork(newFiddle, fidRW().get.id, fidRW().get.version).call().map {
            case Right(fid) =>
              UpdateId(fid)
            case Left(error) =>
              NoAction
          }
          updated(value.copy(sourceCode = source), Effect(save))
      } else {
        noChange
      }

    case UpdateId(fid, silent) =>
      if (silent)
        ModelUpdate(fidRW.updated(Some(fid)))
      else
        ModelUpdateEffect(fidRW.updated(Some(fid)), Effect.action(NavigateTo(AppRouter.EditorPage(fid.id, fid.version))))
  }
}
