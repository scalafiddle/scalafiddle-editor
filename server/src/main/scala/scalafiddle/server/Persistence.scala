package scalafiddle.server

import java.util.UUID

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import play.api.Configuration
import slick.backend.DatabaseConfig
import slick.dbio.{DBIOAction, NoStream}
import slick.driver.JdbcProfile

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafiddle.server.dao.{Fiddle, FiddleDAL}
import scalafiddle.shared.{FiddleData, FiddleId, Library}

case class AddFiddle(fiddle: FiddleData, user: String)

case class UpdateFiddle(fiddle: FiddleData, id: String, user: String)

case class ForkFiddle(fiddle: FiddleData, id: String, version: Int, user: String)

case class FindFiddle(id: String, version: Int)

case class FindLastFiddle(id: String)

case object GetFiddleInfo

case class FiddleInfo(name: String, id: FiddleId)

case class RemoveFiddle(id: String, version: Int)

class Persistence(config: Configuration) extends Actor with ActorLogging {
  val dbConfig = DatabaseConfig.forConfig[JdbcProfile](config.getString("scalafiddle.dbConfig").get)
  val db = dbConfig.db
  val dal = new FiddleDAL(dbConfig.driver)

  def runAndReply[T, R](stmt: DBIOAction[T, NoStream, Nothing])(process: T => Try[R]) = {
    db.run(stmt).map(r => process(r)).recover {
      case e: Throwable =>
        log.error(e, s"Error accessing database ${stmt.toString}")
        Failure(e)
    }
  }

  val mapping = ('0' to '9') ++ ('A' to 'Z') ++ ('a' to 'z')

  def createId = {
    var id = math.abs(UUID.randomUUID().getLeastSignificantBits)
    // encode to 0-9A-Za-z
    var strId = ""
    while (strId.length < 6) {
      strId += mapping((id % mapping.length).toInt)
      id /= mapping.length
    }
    strId
  }

  def receive = {
    case AddFiddle(fiddle, user) =>
      val id = createId
      val newFiddle = Fiddle(id, 0, fiddle.name, fiddle.description, fiddle.sourceCode, fiddle.libraries.map(Library.stringify).toList, user)
      log.debug(s"Storing new fiddle: $newFiddle")
      runAndReply(dal.insert(newFiddle))(r => Success(FiddleId(id, 0))) pipeTo sender()

    case ForkFiddle(fiddle, id, version, user) =>
      val id = createId
      val newFiddle = Fiddle(id, 0, fiddle.name, fiddle.description, fiddle.sourceCode, fiddle.libraries.map(Library.stringify).toList, user, Some(s"$id/$version"))
      log.debug(s"Forking new fiddle: $newFiddle")
      runAndReply(dal.insert(newFiddle))(r => Success(FiddleId(id, 0))) pipeTo sender()

    case FindFiddle(id, version) =>
      val res = db.run(dal.find(id, version)).map {
        case Some(fiddle) => Success(fiddle)
        case None => Failure(new NoSuchElementException)
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case UpdateFiddle(fiddle, id, user) =>
      // find last fiddle version for this id
      val res = db.run(dal.findVersions(id)).flatMap {
        case fiddles if fiddles.nonEmpty =>
          val newVersion = fiddles.last.version + 1
          val newFiddle = Fiddle(id, newVersion, fiddle.name, fiddle.description, fiddle.sourceCode, fiddle.libraries.map(Library.stringify).toList, user)
          runAndReply(dal.insert(newFiddle))(r => Success(FiddleId(id, newVersion)))
        case _ => Future.successful(Failure(new NoSuchElementException))
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case FindLastFiddle(id) =>
      val res = db.run(dal.findVersions(id)).map {
        case fiddles if fiddles.nonEmpty => Success(fiddles.last)
        case _ => Failure(new NoSuchElementException)
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case RemoveFiddle(id, version) =>
      val res = db.run(dal.remove(id, version)).map {
        case 1 => Success(id)
        case _ => Failure(new NoSuchElementException)
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case GetFiddleInfo =>
      val res = db.run(dal.getAll).map(r => Success(r.map {
        case (id, version, name) => FiddleInfo(name, FiddleId(id, version))
      })).recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()
  }
}
