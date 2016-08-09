package scalafiddle.client

import diode.data.Pot
import diode._
import org.scalajs.dom.ext.Ajax
import upickle.default._

import scala.scalajs.js
import scala.scalajs.niocharset.StandardCharsets
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue

case class SelectLibrary(lib: Library) extends Action

case class DeselectLibrary(lib: Library) extends Action

sealed trait SJSOptimization

case object FastOpt extends SJSOptimization
case object FullOpt extends SJSOptimization

case class CompileFiddle(source: String, optimization: SJSOptimization) extends Action

case class AutoCompleteFiddle(source: String, row: Int, col: Int, callback: (Seq[(String, String)]) => Unit) extends Action

class FiddleHandler[M](modelRW: ModelRW[M, Pot[FiddleData]]) extends ActionHandler(modelRW) {
  override def handle = {
    case SelectLibrary(lib) =>
      updated(value.map(fd => fd.copy(libraries = fd.libraries :+ lib)))
    case DeselectLibrary(lib) =>
      updated(value.map(fd => fd.copy(libraries = fd.libraries.filterNot(_ == lib))))
    case AutoCompleteFiddle(source, row, col, callback) =>
      val effect = {
        val intOffset = col + source.split("\n")
          .take(row)
          .map(_.length + 1)
          .sum
        val flag = if (source.take(intOffset).endsWith(".")) "member" else "scope"
        Ajax.get(
          url = s"http://localhost:8080/complete?flag=$flag&offset=$intOffset&source=${encodeSource(source)}"
        ).map { request =>
          val results = read[List[(String, String)]](request.responseText)
          println(s"Completion results: $results")
          callback(results)
          NoAction
        }
      }
      effectOnly(Effect(effect))
  }

  def encodeSource(source: String): String = {
    import com.github.marklister.base64.Base64._
    import js.JSConverters._
    implicit def scheme: B64Scheme = base64Url
    val fullSource = source.getBytes(StandardCharsets.UTF_8)
    val compressedBuffer = new Gzip(fullSource.toJSArray).compress()
    val compressedSource = new Array[Byte](compressedBuffer.length)
    var i = 0
    while(i < compressedBuffer.length) {
      compressedSource(i) = compressedBuffer.get(i).toByte
      i += 1
    }
    Encoder(compressedSource).toBase64
  }

}
