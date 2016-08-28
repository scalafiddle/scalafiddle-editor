package scalafiddle.server.models.services
import java.util.UUID
import javax.inject.{Inject, Named}

import akka.actor.ActorRef
import com.mohiva.play.silhouette.api.LoginInfo
import com.mohiva.play.silhouette.impl.providers.CommonSocialProfile

import scala.concurrent.Future
import scalafiddle.server.models.User

class UserServiceImpl @Inject() (@Named("persistence") persistence: ActorRef) extends UserService {
  /**
    * Retrieves a user that matches the specified ID.
    *
    * @param id The ID to retrieve a user.
    * @return The retrieved user or None if no user could be retrieved for the given ID.
    */
  override def retrieve(id: UUID): Future[Option[User]] = ???

  /**
    * Saves a user.
    *
    * @param user The user to save.
    * @return The saved user.
    */
  override def save(user: User): Future[User] = ???

  /**
    * Saves the social profile for a user.
    *
    * If a user exists for this profile then update the user, otherwise create a new user with the given profile.
    *
    * @param profile The social profile to save.
    * @return The user for whom the profile was saved.
    */
  override def save(profile: CommonSocialProfile): Future[User] = ???

  override def retrieve(loginInfo: LoginInfo): Future[Option[User]] = ???
}
