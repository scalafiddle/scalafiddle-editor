package scalafiddle.server

import org.slf4j.LoggerFactory
import upickle.Js
import upickle.default._

import scala.io.BufferedSource
import scalafiddle.shared._

class Librarian(libSource: () => BufferedSource) {

  case class LibraryVersion(
      version: String,
      scalaVersions: Seq[String],
      extraDeps: Seq[String],
      organization: Option[String],
      artifact: Option[String],
      doc: Option[String],
      example: Option[String]
  )

  implicit val libraryVersionReader = upickle.default.Reader[LibraryVersion] {
    case Js.Obj(valueSeq @ _*) =>
      val values = valueSeq.toMap
      LibraryVersion(
        readJs[String](values("version")),
        readJs[Seq[String]](values("scalaVersions")),
        readJs[Seq[String]](values.getOrElse("extraDeps", Js.Arr())),
        values.get("organization").map(readJs[String]),
        values.get("artifact").map(readJs[String]),
        values.get("doc").map(readJs[String]),
        values.get("example").map(readJs[String])
      )
  }

  case class LibraryDef(
      name: String,
      organization: String,
      artifact: String,
      doc: String,
      versions: Seq[LibraryVersion],
      compileTimeOnly: Boolean
  )

  case class LibraryGroup(
      group: String,
      libraries: Seq[LibraryDef]
  )

  val repoSJSRE = """([^ %]+) *%%% *([^ %]+) *% *([^ %]+)""".r
  val repoRE    = """([^ %]+) *%% *([^ %]+) *% *([^ %]+)""".r
  val log       = LoggerFactory.getLogger(getClass)
  var libraries = Seq.empty[Library]
  refresh()

  def refresh(): Unit = {
    try {
      log.debug(s"Loading libraries...")
      val newLibs = loadLibraries
      log.debug(s"  loaded ${newLibs.size} libraries")
      libraries = newLibs
    } catch {
      case e: Throwable =>
        log.error(s"Error loading libraries", e)
    }
  }

  def loadLibraries: Seq[Library] = {
    val data      = libSource().mkString
    val libGroups = read[Seq[LibraryGroup]](data)
    for {
      (group, idx) <- libGroups.zipWithIndex
      lib          <- group.libraries
      versionDef   <- lib.versions
    } yield {
      Library(
        lib.name,
        versionDef.organization.getOrElse(lib.organization),
        versionDef.artifact.getOrElse(lib.artifact),
        versionDef.version,
        lib.compileTimeOnly,
        versionDef.scalaVersions,
        versionDef.extraDeps,
        f"$idx%02d:${group.group}",
        createDocURL(versionDef.doc.getOrElse(lib.doc)),
        versionDef.example
      )
    }
  }

  def findLibrary(libDef: String): Option[Library] = libDef match {
    case repoSJSRE(group, artifact, version) =>
      libraries.find(l => l.organization == group && l.artifact == artifact && l.version == version && !l.compileTimeOnly)
    case repoRE(group, artifact, version) =>
      libraries.find(l => l.organization == group && l.artifact == artifact && l.version == version && l.compileTimeOnly)
    case _ =>
      None
  }

  private def createDocURL(doc: String): String = {
    val githubRef = """([^/]+)/([^/]+)""".r
    doc match {
      case githubRef(org, lib) => s"https://github.com/$org/$lib"
      case _                   => doc
    }
  }
}
