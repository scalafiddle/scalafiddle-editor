package scalafiddle.server

import java.util.UUID
import javax.inject.Inject

import akka.actor.{Actor, ActorLogging}
import akka.pattern.pipe
import com.mohiva.play.silhouette.api.LoginInfo
import play.api.Configuration
import slick.backend.DatabaseConfig
import slick.dbio.{DBIOAction, NoStream}
import slick.driver.JdbcProfile

import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scalafiddle.server.dao.{Fiddle, FiddleDAL}
import scalafiddle.server.models.User
import scalafiddle.shared.{FiddleData, FiddleId, Library}

case class AddFiddle(fiddle: FiddleData, user: String)

case class UpdateFiddle(fiddle: FiddleData, id: String)

case class ForkFiddle(fiddle: FiddleData, id: String, version: Int, user: String)

case class FindFiddle(id: String, version: Int)

case class FindLastFiddle(id: String)

case class FindUserFiddles(userId: String)

case object GetFiddleInfo

case class FiddleInfo(name: String, id: FiddleId)

case class RemoveFiddle(id: String, version: Int)

case class FindUser(id: String)

case class FindUserLogin(loginInfo: LoginInfo)

case class AddUser(user: User)

case class UpdateUser(user: User)

class Persistence @Inject()(config: Configuration) extends Actor with ActorLogging {
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
    while (strId.length < 7) {
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
      runAndReply(dal.insertFiddle(newFiddle))(r => Success(FiddleId(id, 0))) pipeTo sender()

    case ForkFiddle(fiddle, id, version, user) =>
      val id = createId
      val newFiddle = Fiddle(id, 0, fiddle.name, fiddle.description, fiddle.sourceCode, fiddle.libraries.map(Library.stringify).toList, user, Some(s"$id/$version"))
      log.debug(s"Forking new fiddle: $newFiddle")
      runAndReply(dal.insertFiddle(newFiddle))(r => Success(FiddleId(id, 0))) pipeTo sender()

    case FindFiddle(id, version) =>
      val res = db.run(dal.findFiddle(id, version)).map {
        case Some(fiddle) => Success(fiddle)
        case None => Failure(new NoSuchElementException)
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case FindUserFiddles(userId) =>
      val res = db.run(dal.findUserFiddles(userId)).map { fiddles =>
        Success(fiddles.groupBy(_.id).map { case (id, versions) => id -> versions.sortBy(_.version) })
      } recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case UpdateFiddle(fiddle, id) =>
      // find last fiddle version for this id
      val res = db.run(dal.findFiddleVersions(id)).flatMap {
        case fiddles if fiddles.nonEmpty =>
          val latest = fiddles.last
          val newVersion = latest.version + 1
          val newFiddle = Fiddle(id, newVersion, fiddle.name, fiddle.description, fiddle.sourceCode, fiddle.libraries.map(Library.stringify).toList, latest.user, Some(s"${latest.id}/${latest.version}"))
          runAndReply(dal.insertFiddle(newFiddle))(r => Success(FiddleId(id, newVersion)))
        case _ => Future.successful(Failure(new NoSuchElementException))
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case FindLastFiddle(id) =>
      val res = db.run(dal.findFiddleVersions(id)).map {
        case fiddles if fiddles.nonEmpty => Success(fiddles.last)
        case _ => Failure(new NoSuchElementException)
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case RemoveFiddle(id, version) =>
      val res = db.run(dal.removeEvent(id, version)).map {
        case 1 => Success(id)
        case _ => Failure(new NoSuchElementException)
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case GetFiddleInfo =>
      val res = db.run(dal.getAllEvents).map(r => Success(r.map {
        case (id, version, name) => FiddleInfo(name, FiddleId(id, version))
      })).recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case FindUser(id) =>
      val res = db.run(dal.findUser(id)).map {
        case Some(user) => Success(user)
        case None => Failure(new NoSuchElementException)
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case FindUserLogin(loginInfo) =>
      val res = db.run(dal.findUser(loginInfo)).map {
        case Some(user) => Success(user)
        case None => Failure(new NoSuchElementException)
      }.recover {
        case e: Throwable => Failure(e)
      }
      res pipeTo sender()

    case AddUser(user) =>
      runAndReply(dal.insert(user))(_ => Success(user)) pipeTo sender()

    case UpdateUser(user) =>
      runAndReply(dal.update(user))(_ => Success(user)) pipeTo sender()
  }
}
