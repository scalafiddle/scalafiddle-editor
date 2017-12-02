package controllers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
import java.net.{URL, URLDecoder, URLEncoder}
import java.nio.charset.StandardCharsets
import java.util.zip.GZIPInputStream
import javax.inject.{Inject, Named}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.mohiva.play.silhouette.api.{LogoutEvent, Silhouette}
import kamon.Kamon
import play.api.mvc._
import play.api.{Configuration, Environment, Logger, Mode}
import slick.basic.DatabaseConfig
import slick.jdbc.JdbcProfile
import slick.jdbc.meta.MTable
import upickle.default._
import upickle.{json, Js}

import scala.concurrent.duration._
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scalafiddle.server._
import scalafiddle.server.dao.{Fiddle, FiddleDAL}
import scalafiddle.server.models.User
import scalafiddle.server.utils.auth.{AllLoginProviders, DefaultEnv}
import scalafiddle.shared.{Api, FiddleData, Library, UserInfo}

object Router extends autowire.Server[Js.Value, Reader, Writer] {
  override def read[R: Reader](p: Js.Value) = readJs[R](p)
  override def write[R: Writer](r: R)       = writeJs(r)
}

class Application @Inject()(
    implicit val config: Configuration,
    env: Environment,
    ec: ExecutionContext,
    silhouette: Silhouette[DefaultEnv],
    actorSystem: ActorSystem,
    @Named("persistence") persistence: ActorRef
) extends InjectedController {
  implicit val timeout = Timeout(15.seconds)
  val log              = Logger(getClass)
  val libUri           = config.get[String]("scalafiddle.librariesURL")
  val scalafiddleUrl   = new URL(config.get[String]("scalafiddle.scalafiddleURL"))
  val compilerUrl      = config.get[String]("scalafiddle.compilerURL")

  Kamon.start()

  val indexCounter  = Kamon.metrics.counter("index-load")
  val fiddleCounter = Kamon.metrics.counter("fiddle-load")

  private def libSource = {
    if (libUri.startsWith("file:")) {
      // load from file system
      scala.io.Source.fromFile(libUri.drop(5), "UTF-8")
    } else if (libUri.startsWith("http")) {
      System.setProperty(
        "http.agent",
        "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/28.0.1500.29 Safari/537.36")
      scala.io.Source.fromURL(libUri, "UTF-8")
    } else {
      env.resourceAsStream(libUri).map(s => scala.io.Source.fromInputStream(s, "UTF-8")).get
    }
  }

  val librarian = new Librarian(libSource _)
  // refresh libraries every N minutes
  actorSystem.scheduler.schedule(config.get[Int]("scalafiddle.refreshLibraries").seconds,
                                 config.get[Int]("scalafiddle.refreshLibraries").seconds)(librarian.refresh())
  val defaultSource = config.get[String]("scalafiddle.defaultSource")

  if (env.mode != Mode.Prod)
    createTables()

  def index(fiddleId: String, version: String) = silhouette.UserAwareAction.async { implicit request =>
    if (fiddleId.isEmpty) {
      indexCounter.increment()
    } else {
      fiddleCounter.increment()
      persistence ! AddAccess(fiddleId,
                              version.toInt,
                              request.identity.map(_.userID),
                              embedded = false,
                              Option(request.remoteAddress).getOrElse("unknown"))
    }

    val source = request.getQueryString("zrc").flatMap(decodeSource) orElse request.getQueryString("source")

    loadFiddle(fiddleId, version.toInt, source).map {
      case Success(fd) =>
        val fdJson = write(fd)
        Ok(views.html.index("ScalaFiddle", fdJson, if (fiddleId.nonEmpty) Some(s"$fiddleId/$version") else None))
          .withHeaders(CACHE_CONTROL -> "max-age=3600")
      case Failure(ex) =>
        NotFound
    }
  }

  def signOut = silhouette.SecuredAction.async { implicit request =>
    val result = Redirect(routes.Application.index("", "0"))
    silhouette.env.eventBus.publish(LogoutEvent(request.identity, request))
    silhouette.env.authenticatorService.discard(request.authenticator, result)
  }

  def rawFiddle(fiddleId: String, version: String) = Action.async { implicit request =>
    if (fiddleId.nonEmpty)
      persistence ! AddAccess(fiddleId,
                              version.toInt,
                              None,
                              embedded = true,
                              Option(request.remoteAddress).getOrElse("unknown"))

    loadFiddle(fiddleId, version.toInt).map {
      case Success(fd) =>
        // create a source code file for embedded ScalaFiddle
        val nameOpt = fd.name match {
          case empty if empty.replaceAll("\\s", "").isEmpty => None
          case nonEmpty                                     => Some(nonEmpty.replaceAll("\\s", " "))
        }
        val sourceCode = new StringBuilder()
        sourceCode.append(fd.sourceCode)
        val allLibs = fd.libraries.flatMap(lib => Library.stringify(lib) +: lib.extraDeps)
        sourceCode.append(allLibs.map(lib => s"// $$FiddleDependency $lib").mkString("\n", "\n", "\n"))
        sourceCode.append(s"// $$ScalaVersion ${fd.scalaVersion}\n")
        nameOpt.foreach(name => sourceCode.append(s"// $$FiddleName $name\n"))

        Ok(sourceCode.toString).withHeaders(CACHE_CONTROL -> "max-age=7200")
      case Failure(ex) =>
        NotFound
    }
  }

  def htmlFiddle(fiddleId: String, version: String) = Action.async { implicit request =>
    if (fiddleId.nonEmpty)
      persistence ! AddAccess(fiddleId,
                              version.toInt,
                              None,
                              embedded = true,
                              Option(request.remoteAddress).getOrElse("unknown"))

    loadFiddle(fiddleId, version.toInt).map {
      case Success(fd) =>
        val html = HTMLFiddle.process(fd, s"$scalafiddleUrl/sf/$fiddleId/$version", s"$fiddleId/$version")
        Ok(html).as("text/html").withHeaders(CACHE_CONTROL -> "max-age=86400")
      case Failure(ex) =>
        NotFound
    }
  }

  def htmlScript(fiddleId: String, version: String) = Action { implicit request =>
    val js =
      s"""(function() {
        |var listener = function(event) {
        |  console.dir(event);
        |  if(event.data.length != 2) return;
        |  var data, eventName;
        |  eventName = event.data[0];
        |  data = event.data[1];
        |  if(eventName === "embedHeight" && data.fiddleId === "$fiddleId/$version") {
        |    var iframe = document.querySelector("#sf$fiddleId$version");
        |    iframe.style.height = data.height + "px";
        |    window.removeEventListener("message", listener);
        |  }
        |}
        |window.addEventListener("message", listener, false);
        |}).call(this);
      """.stripMargin
    Ok(js).as("application/javascript").withHeaders(CACHE_CONTROL -> "max-age=86400")
  }

  def libraryListing(scalaVersion: String) = Action {
    val libStrings = librarian.libraries
      .filter(_.scalaVersions.contains(scalaVersion))
      .flatMap(lib => Library.stringify(lib) +: lib.extraDeps)
    Ok(write(libStrings)).as("application/json").withHeaders(CACHE_CONTROL -> "max-age=60")
  }

  def resultFrame = Action { request =>
    Ok(views.html.resultframe()).withHeaders(CACHE_CONTROL -> "max-age=3600")
  }

  val loginProviders = config.get[Seq[String]]("scalafiddle.loginProviders").map(AllLoginProviders.providers)

  def autowireApi(path: String) = silhouette.UserAwareAction.async { implicit request =>
    val apiService: Api = new ApiService(persistence, request.identity, loginProviders)

    // get the request body as JSON
    val b = request.body.asText.get

    // call Autowire route
    Router
      .route[Api](apiService)(
        autowire.Core.Request(path.split("/"), json.read(b).asInstanceOf[Js.Obj].value.toMap)
      )
      .map(buffer => {
        val data = json.write(buffer)
        Ok(data)
      })
  }

  val passthroughParams = Seq(
    "responsiveWidth",
    "theme",
    "style",
    "fullOpt",
    "layout",
    "hideButtons",
    "referrer"
  )

  def oembed(url: String, maxwidth: Option[Int], maxheight: Option[Int], format: Option[String]) = {
    oembedBase(url, maxwidth, maxheight, format, url.contains("static=true"))
  }

  def oembedStatic(url: String, maxwidth: Option[Int], maxheight: Option[Int], format: Option[String]) =
    oembedBase(url, maxwidth, maxheight, format, isStatic = true)

  def oembedBase(url: String, maxwidth: Option[Int], maxheight: Option[Int], format: Option[String], isStatic: Boolean) =
    Action.async { implicit request =>
      // parse URL
      Try(new URL(url)).toOption match {
        case None =>
          log.debug(s"Invalid URL $url")
          Future.successful(NotFound)
        case Some(fiddleUrl) =>
          val params = Option(fiddleUrl.getQuery)
            .map { query =>
              query
                .split("&")
                .map { param =>
                  param.split("=") match {
                    case Array(key, value) => URLDecoder.decode(key, "UTF-8") -> Some(URLDecoder.decode(value, "UTF-8"))
                    case Array(key)        => URLDecoder.decode(key, "UTF-8") -> None
                  }
                }
                .toMap
            }
            .getOrElse(Map.empty)
          // validate URL and parameters
          val pathRE = """/sf/(\w{4,8})/(\d{1,6}).*""".r
          if (format.getOrElse("json").toLowerCase != "json") {
            Future.successful(NotImplemented)
          } else if (fiddleUrl.getHost != scalafiddleUrl.getHost) {
            log.debug(s"Invalid host ${fiddleUrl.getHost}")
            Future.successful(NotFound)
          } else {
            // load fiddle metadata
            fiddleUrl.getPath match {
              case pathRE(fiddleId, version) =>
                ask(persistence, FindFiddle(fiddleId, version.toInt)).mapTo[Try[Fiddle]].flatMap {
                  case Success(fiddle) =>
                    ask(persistence, FindUser(fiddle.user))
                      .mapTo[Try[User]]
                      .map {
                        case Success(user) =>
                          user.fullName
                        case Failure(e) =>
                          None
                      }
                      .map { userName =>
                        // create response
                        val height = maxheight.getOrElse(400) min 400
                        val width  = maxwidth.getOrElse(960) min 960
                        val html = if (isStatic) {
                          val embedUrl  = s"$scalafiddleUrl/html/$fiddleId/$version"
                          val scriptUrl = s"$scalafiddleUrl/htmlscript/$fiddleId/$version"
                          s"""<iframe id="sf$fiddleId$version" height="$height" width="$width" frameborder="0" style="width: 100%; height: 100%;" scrolling="no" src="$embedUrl"></iframe>
                             |<script async src="$scriptUrl"></script>""".stripMargin
                        } else {
                          val embedParams = params
                            .filterKeys(passthroughParams.contains)
                            .map {
                              case (key, value) =>
                                s"${URLEncoder.encode(key, "UTF-8")}${value.map(v => "=" + URLEncoder.encode(v, "UTF-8")).getOrElse("")}"
                            }
                            .mkString("&", "&", "")
                          val embedUrl = s"$compilerUrl/embed?sfid=$fiddleId/$version&passive$embedParams"
                          s"""<iframe id="sf$fiddleId$version" height="$height" width="$width" frameborder="0" style="width: 100%; overflow: hidden;" scrolling="no" src="$embedUrl"></iframe>"""
                        }
                        val response = Map[String, Js.Value](
                          "type"          -> Js.Str("rich"),
                          "version"       -> Js.Str("1.0"),
                          "title"         -> Js.Str(s"ScalaFiddle - ${fiddle.name}"),
                          "provider_name" -> Js.Str("ScalaFiddle"),
                          "provider_url"  -> Js.Str(scalafiddleUrl.toString),
                          "cache_age"     -> Js.Num(3600 * 24),
                          "height"        -> Js.Num(height),
                          "width"         -> Js.Num(width),
                          "html"          -> Js.Str(html)
                        ) ++ userName.map(name => "author_name" -> Js.Str(name))
                        Ok(write(response)).as("application/json").withHeaders(CACHE_CONTROL -> "max-age=3600")
                      }
                  case Failure(e) =>
                    log.debug(s"Fiddle $fiddleId/$version not found")
                    Future.successful(NotFound)
                }
              case _ =>
                log.debug(s"Path not found")
                Future.successful(NotFound)
            }
          }
      }
    }

  private def loadFiddle(id: String, version: Int, sourceOpt: Option[String] = None): Future[Try[FiddleData]] = {
    if (id == "") {
      val (source, libs) = parseFiddle(sourceOpt.fold(defaultSource)(identity))
      Future.successful(Success(
        FiddleData("", "", source, libs, librarian.libraries, config.get[String]("scalafiddle.defaultScalaVersion"), None)))
    } else {
      ask(persistence, FindFiddle(id, version)).mapTo[Try[Fiddle]].flatMap {
        case Success(f) if f.user == "anonymous" =>
          Future.successful(
            Success(
              FiddleData(
                f.name,
                f.description,
                f.sourceCode,
                f.libraries.flatMap(librarian.findLibrary),
                librarian.libraries,
                f.scalaVersion,
                None
              )))
        case Success(f) =>
          ask(persistence, FindUser(f.user)).mapTo[Try[User]].map {
            case Success(u) =>
              val user = UserInfo(u.userID, u.name.getOrElse("Anonymous"), u.avatarURL, loggedIn = false)
              Success(
                FiddleData(
                  f.name,
                  f.description,
                  f.sourceCode,
                  f.libraries.flatMap(librarian.findLibrary),
                  librarian.libraries,
                  f.scalaVersion,
                  Some(user)
                ))
            case _ =>
              Success(
                FiddleData(
                  f.name,
                  f.description,
                  f.sourceCode,
                  f.libraries.flatMap(librarian.findLibrary),
                  librarian.libraries,
                  f.scalaVersion,
                  None
                ))
          }
        case Failure(e) =>
          Future.successful(Failure(e))
      }
    }
  }

  private def parseFiddle(source: String): (String, Seq[Library]) = {
    val dependencyRE          = """ *// \$FiddleDependency (.+)""".r
    val lines                 = source.split("\n")
    val (libLines, codeLines) = lines.partition(line => dependencyRE.findFirstIn(line).isDefined)
    val libs                  = libLines.flatMap(line => librarian.findLibrary(dependencyRE.findFirstMatchIn(line).get.group(1)))
    (codeLines.mkString("\n"), libs)
  }

  private def decodeSource(b64: String): Option[String] = {
    try {
      import com.github.marklister.base64.Base64._
      implicit def scheme: B64Scheme = base64Url
      // decode base64 and gzip
      val compressedSource = Decoder(b64).toByteArray
      val bis              = new ByteArrayInputStream(compressedSource)
      val zis              = new GZIPInputStream(bis)
      val buf              = new Array[Byte](1024)
      val bos              = new ByteArrayOutputStream()
      var len              = 0
      while ({ len = zis.read(buf); len > 0 }) {
        bos.write(buf, 0, len)
      }
      zis.close()
      bos.close()
      Some(new String(bos.toByteArray, StandardCharsets.UTF_8))
    } catch {
      case e: Throwable =>
        log.info(s"Invalid encoded source received: $e")
        None
    }
  }

  private def createTables() = {
    log.debug(s"Creating missing tables")
    // create tables
    val dbConfig = DatabaseConfig.forConfig[JdbcProfile](config.get[String]("scalafiddle.dbConfig"))
    val db       = dbConfig.db
    val dal      = new FiddleDAL(dbConfig.profile)
    import dal.driver.api._

    def createTableIfNotExists(tables: Seq[TableQuery[_ <: Table[_]]]): Future[Any] = {
      // create tables in order, waiting for previous "create" to complete before running next
      tables.foldLeft(Future.successful(())) { (f, table) =>
        f.flatMap(_ =>
          db.run(MTable.getTables(table.baseTableRow.tableName)).flatMap { result =>
            if (result.isEmpty) {
              log.debug(s"Creating table: ${table.baseTableRow.tableName}")
              db.run(table.schema.create)
            } else {
              Future.successful(())
            }
        })
      }
    }
    Await.result(createTableIfNotExists(Seq(dal.fiddles, dal.users, dal.accesses)), Duration.Inf)
  }
}
