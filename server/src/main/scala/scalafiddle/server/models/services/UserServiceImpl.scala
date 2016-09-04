package scalafiddle.server.models.services
import java.util.UUID
import javax.inject.{Inject, Named}

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile
import play.api.Logger

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Success, Try}
import scalafiddle.server._
import scalafiddle.server.models.User

class UserServiceImpl @Inject() (@Named("persistence") persistence: ActorRef) extends UserService {
  implicit val timeout = Timeout(15.seconds)
  val log = Logger(getClass)

  /**
    * Retrieves a user that matches the specified ID.
    *
    * @param id The ID to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given ID.
    */
  override def retrieve(id: UUID): Future[Option[User]] = {
    ask(persistence, FindUser(id.toString)).mapTo[Try[User]].map {
      case Success(user) =>
        Some(user)
      case _ =>
        None
    }
  }

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  override def save(user: User): Future[User] = {
    ask(persistence, AddUser(user)).mapTo[Try[User]].map {
      case Success(u) =>
        u
      case _ =>
        throw new Exception("Unable to save user")
    }
  }

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
    *
    * @param profile The social profile to save.
    * @return The user for whom the profile was saved.
    */
  override def save(profile: CommonSocialProfile): Future[User] = {
    log.debug(s"User $profile logged in")
    val user = User(UUID.randomUUID().toString, profile.loginInfo, profile.firstName, profile.lastName, profile.fullName, profile.email, profile.avatarURL, true)
    ask(persistence, AddUser(user)).mapTo[Try[User]].map {
      case Success(u) =>
        u
      case _ =>
        throw new Exception("Unable to save user")
    }
  }

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = {
    ask(persistence, FindUserLogin(loginInfo)).mapTo[Try[User]].map {
      case Success(user) =>
        Some(user)
      case _ =>
        None
    }
  }
}
