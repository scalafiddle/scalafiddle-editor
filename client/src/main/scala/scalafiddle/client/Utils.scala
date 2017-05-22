package scalafiddle.client

import scala.scalajs.js
import scala.language.implicitConversions
import js.JSConverters._
import scala.scalajs.js.annotation.{JSGlobal, JSName}
import scala.scalajs.js.typedarray.Uint8Array
import org.scalajs.dom

import scala.scalajs.js.|

class JsVal(val value: js.Dynamic) {
  def get(name: String): Option[JsVal] = {
    (value.selectDynamic(name): Any) match {
      case () => None
      case v  => Some(JsVal(v.asInstanceOf[js.Dynamic]))
    }
  }

  def apply(name: String): JsVal = get(name).get
  def apply(index: Int): JsVal   = value.asInstanceOf[js.Array[JsVal]](index)

  def keys: Seq[String]  = js.Object.keys(value.asInstanceOf[js.Object]).toSeq.map(x => x: String)
  def values: Seq[JsVal] = keys.map(x => JsVal(value.selectDynamic(x)))

  def isDefined: Boolean = !js.isUndefined(value)
  def isNull: Boolean    = value eq null

  def asDouble: Double   = value.asInstanceOf[Double]
  def asBoolean: Boolean = value.asInstanceOf[Boolean]
  def asString: String   = value.asInstanceOf[String]

  override def toString: String = js.JSON.stringify(value)
}

object JsVal {
  implicit def jsVal2jsAny(v: JsVal): js.Any = v.value

  implicit def jsVal2String(v: JsVal): js.Any = v.toString

  def parse(value: String) = new JsVal(js.JSON.parse(value))

  def apply(value: js.Any) = new JsVal(value.asInstanceOf[js.Dynamic])

  def obj(keyValues: (String, js.Any)*) = {
    val obj = new js.Object().asInstanceOf[js.Dynamic]
    for ((k, v) <- keyValues) {
      obj.updateDynamic(k)(v.asInstanceOf[js.Any])
    }
    new JsVal(obj)
  }

  def arr(values: js.Any*) = {
    new JsVal(values.toJSArray.asInstanceOf[js.Dynamic])
  }
}

@JSGlobal("Zlib.Gzip")
@js.native
class Gzip(data: js.Array[Byte]) extends js.Object {
  def compress(): Uint8Array = js.native
}

@JSGlobal("Zlib.Gunzip")
@js.native
class Gunzip(data: js.Array[Byte]) extends js.Object {
  def decompress(): Uint8Array = js.native
}

@js.native
@JSGlobal("ScalaFiddleConfig")
object ScalaFiddleConfig extends js.Object {
  val compilerURL: String             = js.native
  val helpURL: String                 = js.native
  val scalaVersions: js.Array[String] = js.native
}

@js.native
@JSGlobal
object Mousetrap extends js.Object {
  def bind(key: String | js.Array[String], f: js.Function1[dom.KeyboardEvent, Boolean], event: String = js.native): Unit =
    js.native

  def bindGlobal(key: String | js.Array[String],
                 f: js.Function1[dom.KeyboardEvent, Boolean],
                 event: String = js.native): Unit = js.native

  def unbind(key: String): Unit = js.native

  def reset(): Unit = js.native
}
