package scalafiddle.client

import autowire._
import diode.ActionResult.{ModelUpdate, ModelUpdateEffect}
import diode._
import diode.data.{Pot, Ready}

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

class FiddleHandler[M](modelRW: ModelRW[M, Pot[FiddleData]], fidRW: ModelRW[M, Option[FiddleId]]) extends ActionHandler(modelRW) {

  override def handle = {
    case SelectLibrary(lib) =>
      updated(value.map(fd => fd.copy(libraries = fd.libraries :+ lib)))

    case DeselectLibrary(lib) =>
      updated(value.map(fd => fd.copy(libraries = fd.libraries.filterNot(_ == lib))))

    case UpdateInfo(name, description) =>
      updated(value.map(fd => fd.copy(name = name, description = description)))

    case UpdateSource(source) =>
      updated(value.map(fd => fd.copy(sourceCode = source)))

    case SaveFiddle(source) =>
      value match {
        case Ready(fiddle) =>
          val newFiddle = fiddle.copy(sourceCode = source, available = Seq.empty)
          val save: Future[Action] = AjaxClient[Api].save(newFiddle).call().map {
            case Right(fid) =>
              UpdateId(fid)
            case Left(error) =>
              NoAction
          }
          updated(value.map(fd => fd.copy(sourceCode = source)), Effect(save))
        case _ =>
          noChange
      }

    case UpdateFiddle(source) =>
      value match {
        case Ready(fiddle) if fidRW().isDefined =>
          val newFiddle = fiddle.copy(sourceCode = source, available = Seq.empty)
          val save: Future[Action] = AjaxClient[Api].update(newFiddle, fidRW().get.id).call().map {
            case Right(fid) =>
              UpdateId(fid)
            case Left(error) =>
              NoAction
          }
          updated(value.map(fd => fd.copy(sourceCode = source)), Effect(save))
        case _ =>
          noChange
      }

    case ForkFiddle(source) =>
      value match {
        case Ready(fiddle) if fidRW().isDefined =>
          val newFiddle = fiddle.copy(sourceCode = source, available = Seq.empty)
          val save: Future[Action] = AjaxClient[Api].fork(newFiddle, fidRW().get.id, fidRW().get.version).call().map {
            case Right(fid) =>
              UpdateId(fid)
            case Left(error) =>
              NoAction
          }
          updated(value.map(fd => fd.copy(sourceCode = source)), Effect(save))
        case _ =>
          noChange
      }

    case UpdateId(fid, silent) =>
      if (silent)
        ModelUpdate(fidRW.updated(Some(fid)))
      else
        ModelUpdateEffect(fidRW.updated(Some(fid)), Effect.action(NavigateTo(AppRouter.EditorPage(fid.id, fid.version))))
  }
}
