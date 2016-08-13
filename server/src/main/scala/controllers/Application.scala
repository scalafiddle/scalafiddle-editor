package controllers

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.google.inject.Inject
import play.api.mvc._
import play.api.{Configuration, Environment, Mode}
import play.api.Logger
import slick.backend.DatabaseConfig
import slick.driver.JdbcProfile
import slick.jdbc.meta.MTable
import upickle.default._
import upickle.{Js, json}

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}
import scalafiddle.server.dao.{Fiddle, FiddleDAL}
import scalafiddle.server.{ApiService, FindFiddle, Persistence}
import scalafiddle.shared.{Api, FiddleData, Library}

object Router extends autowire.Server[Js.Value, Reader, Writer] {
  override def read[R: Reader](p: Js.Value) = readJs[R](p)
  override def write[R: Writer](r: R) = writeJs(r)
}

class Application @Inject()(implicit val config: Configuration, env: Environment, actorSystem: ActorSystem) extends Controller {
  implicit val timeout = Timeout(15.seconds)
  val log = Logger(getClass)
  val libraries = loadLibraries(config.getString("scalafiddle.librariesURL").get)
  val defaultSource = config.getString("scalafiddle.defaultSource").get
  val errorSource = config.getString("scalafiddle.errorSource").get

  val persistence = actorSystem.actorOf(Props(new Persistence(config)), "persistence")

  if(env.mode != Mode.Prod)
    createTables()

  def index(fiddleId: String, version: String) = Action.async {
    loadFiddle(fiddleId, version.toInt).map {
      case Success(fd) =>
        val fdJson = write(fd)
        Ok(views.html.index("ScalaFiddle", fdJson))
      case Failure(ex) =>
        NotFound
    }
  }

  def resultFrame = Action { request =>
    Ok(views.html.resultframe()).withHeaders(CACHE_CONTROL -> "max-age=86400")
  }

  def autowireApi(path: String) = Action.async {
    implicit request =>
      val apiService: Api = new ApiService(persistence, "anonymous")
      println(s"Request path: $path")

      // get the request body as JSON
      val b = request.body.asText.get

      // call Autowire route
      Router.route[Api](apiService)(
        autowire.Core.Request(path.split("/"), json.read(b).asInstanceOf[Js.Obj].value.toMap)
      ).map(buffer => {
        val data = json.write(buffer)
        Ok(data)
      })
  }

  def loadLibraries(uri: String): Seq[Library] = {
    val data = if (uri.startsWith("file:")) {
      // load from file system
      scala.io.Source.fromFile(uri.drop(5), "UTF-8").mkString
    } else {
      env.resourceAsStream(uri).map(s => scala.io.Source.fromInputStream(s, "UTF-8").mkString).get
    }
    read[Seq[Library]](data)
  }

  def loadFiddle(id: String, version: Int): Future[Try[FiddleData]] = {
    if(id == "") {
      // build default fiddle data
      val (source, libs) = parseFiddle(defaultSource)
      Future(Success(FiddleData("", "", source, libs, Seq.empty, libraries)))
    } else {
      ask(persistence, FindFiddle(id, version)).mapTo[Try[Fiddle]].map(_.map(f =>
        FiddleData(f.name, f.description, f.sourceCode, f.libraries.flatMap(findLibrary), Seq.empty, libraries)
      ))
    }
  }

  val repoSJSRE = """([^ %]+) *%%% *([^ %]+) *% *([^ %]+)""".r
  val repoRE = """([^ %]+) *%% *([^ %]+) *% *([^ %]+)""".r

  def findLibrary(libDef: String): Option[Library] = libDef match {
    case repoSJSRE(group, artifact, version) =>
      libraries.find(l => l.group == group && l.artifact == artifact && l.version == version && !l.compileTimeOnly)
    case repoRE(group, artifact, version) =>
      libraries.find(l => l.group == group && l.artifact == artifact && l.version == version && l.compileTimeOnly)
    case _ =>
      None
  }

  def parseFiddle(source: String): (String, Seq[Library]) = {
    val dependencyRE = """ *// \$FiddleDependency (.+)"""
    val lines = source.split("\n")
    val (libLines, codeLines) = lines.partition(_.matches(dependencyRE))
    val libs = libLines.flatMap(findLibrary)
    (codeLines.mkString("\n"), libs)
  }

  def createTables() = {
    log.debug(s"Creating missing tables")
    // create tables
    val dbConfig = DatabaseConfig.forConfig[JdbcProfile](config.getString("scalafiddle.dbConfig").get)
    val db = dbConfig.db
    val dal = new FiddleDAL(dbConfig.driver)
    import dal.driver.api._

    def createTableIfNotExists(tables: Seq[TableQuery[_ <: Table[_]]]): Future[Any] = {
      // create tables in order, waiting for previous "create" to complete before running next
      tables.foldLeft(Future.successful(())) { (f, table) =>
        f.flatMap(_ => db.run(MTable.getTables(table.baseTableRow.tableName)).flatMap { result =>
          if (result.isEmpty) {
            log.debug(s"Creating table: ${table.baseTableRow.tableName}")
            db.run(table.schema.create)
          } else {
            Future.successful(())
          }
        })
      }
    }
    Await.result(createTableIfNotExists(Seq(dal.fiddles)), Duration.Inf)
  }
}
