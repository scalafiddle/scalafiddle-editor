package scalafiddle.client

import autowire._
import diode.ActionResult.{ModelUpdate, ModelUpdateEffect}
import diode._
import org.scalajs.dom

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafiddle.shared._

case class SelectLibrary(lib: Library) extends Action

case class DeselectLibrary(lib: Library) extends Action

case class SelectScalaVersion(version: String) extends Action

case class UpdateInfo(name: String, description: String) extends Action

case class UpdateSource(source: String) extends Action

case class SaveFiddle(source: String) extends Action

case class UpdateFiddle(source: String) extends Action

case class ForkFiddle(source: String) extends Action

case class LoadFiddle(url: String) extends Action

case class UpdateId(fiddleId: FiddleId, silent: Boolean = false) extends Action

case class ShowHelp(url: String) extends Action

class FiddleHandler[M](modelRW: ModelRW[M, FiddleData], fidRW: ModelRW[M, Option[FiddleId]]) extends ActionHandler(modelRW) {

  override def handle = {
    case SelectLibrary(lib) =>
      updated(value.copy(libraries = value.libraries :+ lib))

    case DeselectLibrary(lib) =>
      updated(value.copy(libraries = value.libraries.filterNot(_ == lib)))

    case SelectScalaVersion(version) =>
      updated(value.copy(scalaVersion = version))

    case UpdateInfo(name, description) =>
      updated(value.copy(name = name, description = description))

    case UpdateSource(source) =>
      updated(value.copy(sourceCode = source, modified = System.currentTimeMillis()))

    case SaveFiddle(source) =>
      val newFiddle = value.copy(sourceCode = source, available = Seq.empty)
      val saveF: Future[Action] = AjaxClient[Api].save(newFiddle).call().map {
        case Right(fid) =>
          UpdateId(fid)
        case Left(error) =>
          NoAction
      }
      updated(value.copy(sourceCode = source), Effect(saveF))

    case UpdateFiddle(source) =>
      if (fidRW().isDefined) {
        val newFiddle = value.copy(sourceCode = source, available = Seq.empty)
        val updateF: Future[Action] = AjaxClient[Api].update(newFiddle, fidRW().get.id).call().map {
          case Right(fid) =>
            UpdateId(fid)
          case Left(error) =>
            NoAction
        }
        updated(value.copy(sourceCode = source), Effect(updateF))
      } else {
        noChange
      }

    case LoadFiddle(url) =>
      val loadF = dom.ext.Ajax.get(url).map(r => UpdateSource(r.responseText))
      effectOnly(Effect(loadF))

    case ForkFiddle(source) =>
      if (fidRW().isDefined) {
        val newFiddle = value.copy(sourceCode = source, available = Seq.empty)
        val forkF: Future[Action] = AjaxClient[Api].fork(newFiddle, fidRW().get.id, fidRW().get.version).call().map {
          case Right(fid) =>
            UpdateId(fid)
          case Left(error) =>
            NoAction
        }
        updated(value.copy(sourceCode = source), Effect(forkF))
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
