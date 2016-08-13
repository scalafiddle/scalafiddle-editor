package scalafiddle.server

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}
import scalafiddle.shared.{Api, FiddleData, FiddleId}

class ApiService(persistence: ActorRef, user: String)(implicit ec: ExecutionContext) extends Api {
  implicit val timeout = Timeout(15.seconds)

  override def save(fiddle: FiddleData): Future[Either[String, FiddleId]] = {
    ask(persistence, AddFiddle(fiddle, user)).mapTo[Try[FiddleId]].map {
      case Success(fid) => Right(fid)
      case Failure(ex) => Left(ex.toString)
    }
  }

  override def update(fiddle: FiddleData, id: String): Future[Either[String, FiddleId]] = {
    ask(persistence, UpdateFiddle(fiddle, id, user)).mapTo[Try[FiddleId]].map {
      case Success(fid) => Right(fid)
      case Failure(ex) => Left(ex.toString)
    }
  }

  override def fork(fiddle: FiddleData, id: String, version: Int): Future[Either[String, FiddleId]] = {
    ask(persistence, ForkFiddle(fiddle, id, version, user)).mapTo[Try[FiddleId]].map {
      case Success(fid) => Right(fid)
      case Failure(ex) => Left(ex.toString)
    }
  }
}
