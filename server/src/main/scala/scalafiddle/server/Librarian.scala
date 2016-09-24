package scalafiddle.server

import org.slf4j.LoggerFactory
import upickle.default._

import scala.io.BufferedSource
import scalafiddle.shared._

class Librarian(libSource: () => BufferedSource) {
  case class LibraryVersion(
    scalaVersions: Seq[String],
    extraDeps: Seq[String]
  )

  case class LibraryDef(
    name: String,
    organization: String,
    artifact: String,
    doc: String,
    versions: Map[String, LibraryVersion],
    compileTimeOnly: Boolean
  )

  case class LibraryGroup(
    group: String,
    libraries: Seq[LibraryDef]
  )

  val repoSJSRE = """([^ %]+) *%%% *([^ %]+) *% *([^ %]+)""".r
  val repoRE = """([^ %]+) *%% *([^ %]+) *% *([^ %]+)""".r
  val log = LoggerFactory.getLogger(getClass)
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
    val data = libSource().mkString
    val libGroups = read[Seq[LibraryGroup]](data)
    for {
      (group, idx) <- libGroups.zipWithIndex
      lib <- group.libraries
      (version, versionDef) <- lib.versions
    } yield {
      Library(lib.name, lib.organization, lib.artifact, version, lib.compileTimeOnly, versionDef.scalaVersions, versionDef.extraDeps, f"$idx%02d:${group.group}", createDocURL(lib.doc))
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
      case _ => doc
    }
  }

}
