package scalafiddle.server

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scalafiddle.server.dao.Fiddle
import scalafiddle.server.models.User
import scalafiddle.shared.{FiddleVersions, _}

class ApiService(persistence: ActorRef, user: Option[User], _loginProviders: Seq[LoginProvider])(implicit ec: ExecutionContext) extends Api {
  implicit val timeout = Timeout(15.seconds)

  override def save(fiddle: FiddleData): Future[Either[String, FiddleId]] = {
    ask(persistence, AddFiddle(fiddle, user.fold("anonymous")(_.userID))).mapTo[Try[FiddleId]].map {
      case Success(fid) => Right(fid)
      case Failure(ex) => Left(ex.toString)
    }
  }

  override def update(fiddle: FiddleData, id: String): Future[Either[String, FiddleId]] = {
    // allow update only when user is the same as fiddle author (or fiddle is anonymous)
    val updateOk = (fiddle.author.map(_.id), user.map(_.userID)) match {
      case (Some(authorId), Some(userId)) if authorId == userId => true
      case (None, _) => true
      case _ => false
    }
    if (updateOk) {
      ask(persistence, UpdateFiddle(fiddle, id)).mapTo[Try[FiddleId]].map {
        case Success(fid) => Right(fid)
        case Failure(ex) => Left(ex.toString)
      }
    } else {
      Future.successful(Left("Not allowed to update fiddle"))
    }
  }

  override def fork(fiddle: FiddleData, id: String, version: Int): Future[Either[String, FiddleId]] = {
    ask(persistence, ForkFiddle(fiddle, id, version, user.fold("anonymous")(_.userID))).mapTo[Try[FiddleId]].map {
      case Success(fid) => Right(fid)
      case Failure(ex) => Left(ex.toString)
    }
  }

  override def loginProviders(): Seq[LoginProvider] = _loginProviders

  override def userInfo(): UserInfo =
    user.fold(UserInfo("", "", None, loggedIn = false))(u => UserInfo(u.userID, u.name.getOrElse("Anonymous"), u.avatarURL, loggedIn = true))

  override def listFiddles(): Future[Seq[FiddleVersions]] = {
    user match {
      case Some(currentUser) =>
        ask(persistence, FindUserFiddles(currentUser.userID)).mapTo[Try[Map[String, Seq[Fiddle]]]].map {
          case Success(fiddles) =>
            fiddles.toList.map { case (id, versions) =>
              val latest = versions.last
              FiddleVersions(id, latest.name, latest.libraries, latest.version, latest.created)
            }
          case Failure(e) =>
            Seq.empty
        }
      case None =>
        Future.successful(Seq.empty)
    }
  }
}
