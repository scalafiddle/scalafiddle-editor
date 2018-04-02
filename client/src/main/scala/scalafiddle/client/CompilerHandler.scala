package scalafiddle.client

import autowire._
import diode._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.scalajs.js
import scala.scalajs.niocharset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global
import scalafiddle.shared.{Api, FiddleVersions}

sealed trait SJSOptimization {
  def flag: String
}

case object FastOpt extends SJSOptimization {
  val flag = "fast"
}

case object FullOpt extends SJSOptimization {
  val flag = "full"
}

case class CompileFiddle(source: String, optimization: SJSOptimization) extends Action

case class AutoCompleteFiddle(source: String, row: Int, col: Int, callback: (Seq[(String, String)]) => Unit) extends Action

case class CompilerSuccess(
    jsCode: String,
    jsDeps: Seq[String],
    cssDeps: Seq[String],
    log: String
) extends Action

case class CompilerFailed(
    annotations: Seq[EditorAnnotation],
    log: String
) extends Action

case class ServerError(message: String) extends Action

case object LoadUserFiddles extends Action

case class UserFiddlesUpdated(fiddles: Seq[FiddleVersions]) extends Action

class CompilerHandler[M](modelRW: ModelRW[M, OutputData]) extends ActionHandler(modelRW) {

  case class CompilationResponse(
      jsCode: Option[String],
      jsDeps: Seq[String],
      cssDeps: Seq[String],
      annotations: Seq[EditorAnnotation],
      log: String
  )

  case class CompletionResponse(completions: List[(String, String)])

  override def handle = {
    case AutoCompleteFiddle(source, row, col, callback) =>
      val effect = {
        val intOffset = col + source
          .split("\n")
          .take(row)
          .map(_.length + 1)
          .sum
        // call the ScalaFiddle compiler to perform completion
        Ajax
          .post(
            url = s"${ScalaFiddleConfig.compilerURL}/complete?offset=$intOffset",
            data = source
          )
          .map { request =>
            val results = read[CompletionResponse](request.responseText)
            println(s"Completion results: $results")
            callback(results.completions)
            NoAction
          }
      }
      effectOnly(Effect(effect))

    case CompileFiddle(source, optimization) =>
      val effect = Ajax
        .post(
          url = s"${ScalaFiddleConfig.compilerURL}/compile?opt=${optimization.flag}",
          data = source
        )
        .map { request =>
          read[CompilationResponse](request.responseText) match {
            case CompilationResponse(Some(jsCode), jsDeps, cssDeps, _, log) =>
              CompilerSuccess(jsCode, jsDeps, cssDeps, log)
            case CompilationResponse(None, _, _, annotations, log) =>
              CompilerFailed(annotations, log)
          }
        } recover {
        case e: dom.ext.AjaxException =>
          e.xhr.status match {
            case 400 =>
              ServerError(e.xhr.responseText)
            case x =>
              ServerError(s"Server responded with $x: ${e.xhr.responseText}")
          }
        case e: Throwable =>
          ServerError(s"Unknown error while compiling")
      }
      updated(CompilerData(CompilerStatus.Compiling, None, Nil, Nil, Nil, None, ""), Effect(effect))

    case CompilerSuccess(jsCode, jsDeps, cssDeps, log) =>
      updated(CompilerData(CompilerStatus.Compiled, Some(jsCode), jsDeps, cssDeps, Nil, None, log))

    case CompilerFailed(annotations, log) =>
      updated(CompilerData(CompilerStatus.Error, None, Nil, Nil, annotations, None, log))

    case ServerError(message) =>
      updated(CompilerData(CompilerStatus.Error, None, Nil, Nil, Nil, Some(message), ""))

    case LoadUserFiddles =>
      effectOnly(Effect(AjaxClient[Api].listFiddles().call().map(UserFiddlesUpdated)))

    case UserFiddlesUpdated(fiddles) =>
      updated(UserFiddleData(fiddles))

    case ShowHelp(url) =>
      updated(ScalaFiddleHelp(url))
  }

  def encodeSource(source: String): String = {
    import com.github.marklister.base64.Base64._
    import js.JSConverters._
    implicit def scheme: B64Scheme = base64Url
    val fullSource                 = source.getBytes(StandardCharsets.UTF_8)
    val compressedBuffer           = new Gzip(fullSource.toJSArray).compress()
    val compressedSource           = new Array[Byte](compressedBuffer.length)
    var i                          = 0
    while (i < compressedBuffer.length) {
      compressedSource(i) = compressedBuffer.get(i).toByte
      i += 1
    }
    Encoder(compressedSource).toBase64
  }
}
