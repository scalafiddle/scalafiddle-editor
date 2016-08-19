package scalafiddle.client

import diode._
import org.scalajs.dom
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.scalajs.js
import scala.scalajs.niocharset.StandardCharsets
import scala.concurrent.ExecutionContext.Implicits.global

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

case class CompilerResult(result: Either[Seq[EditorAnnotation], String], log: String) extends Action

case class ServerError(message: String) extends Action

class CompilerHandler[M](modelRW: ModelRW[M, CompilerData]) extends ActionHandler(modelRW) {

  case class CompilerResponse(jsCode: Option[String], annotations: Seq[EditorAnnotation], log: String)

  override def handle = {
    case AutoCompleteFiddle(source, row, col, callback) =>
      val effect = {
        val intOffset = col + source.split("\n")
          .take(row)
          .map(_.length + 1)
          .sum
        // call the ScalaFiddle compiler to perform completion
        Ajax.get(
          url = s"${ScalaFiddleConfig.compilerURL}/complete?offset=$intOffset&source=${encodeSource(source)}"
        ).map { request =>
          val results = read[List[(String, String)]](request.responseText)
          println(s"Completion results: $results")
          callback(results)
          NoAction
        }
      }
      effectOnly(Effect(effect))

    case CompileFiddle(source, optimization) =>
      val effect = Ajax.get(
        url = s"${ScalaFiddleConfig.compilerURL}/compile?opt=${optimization.flag}&source=${encodeSource(source)}"
      ).map { request =>
        read[CompilerResponse](request.responseText) match {
          case CompilerResponse(Some(jsCode), _, log) =>
            CompilerResult(Right(jsCode), log)
          case CompilerResponse(None, annotations, log) =>
            CompilerResult(Left(annotations), log)
        }
      } recover {
        case e: dom.ext.AjaxException =>
          ServerError(s"Network error: ${e.xhr.responseText}")
        case e: Throwable =>
          ServerError(s"Unknown error while compiling")
      }
      updated(value.copy(status = CompilerStatus.Compiling, jsCode = None, annotations = Nil, errorMessage = None, log = ""), Effect(effect))

    case CompilerResult(Right(jsCode), log) =>
      updated(CompilerData(CompilerStatus.Compiled, Some(jsCode), Seq.empty, None, log))
    case CompilerResult(Left(annotations), log) =>
      updated(CompilerData(CompilerStatus.Error, None, annotations, None, log))
    case ServerError(message) =>
      updated(CompilerData(CompilerStatus.Error, None, Seq.empty, Some(message), ""))

  }

  def encodeSource(source: String): String = {
    import com.github.marklister.base64.Base64._
    import js.JSConverters._
    implicit def scheme: B64Scheme = base64Url
    val fullSource = source.getBytes(StandardCharsets.UTF_8)
    val compressedBuffer = new Gzip(fullSource.toJSArray).compress()
    val compressedSource = new Array[Byte](compressedBuffer.length)
    var i = 0
    while (i < compressedBuffer.length) {
      compressedSource(i) = compressedBuffer.get(i).toByte
      i += 1
    }
    Encoder(compressedSource).toBase64
  }
}
