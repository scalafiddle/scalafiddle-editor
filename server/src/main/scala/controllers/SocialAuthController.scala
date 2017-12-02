package controllers

import javax.inject.Inject

import com.mohiva.play.silhouette.api._
import com.mohiva.play.silhouette.api.exceptions.ProviderException
import com.mohiva.play.silhouette.impl.providers._
import kamon.Kamon
import play.api.i18n.I18nSupport
import play.api.mvc.InjectedController

import scala.concurrent.{ExecutionContext, Future}
import scalafiddle.server.models.services.UserService
import scalafiddle.server.utils.auth.DefaultEnv

/**
  * The social auth controller.
  *
  * @param silhouette The Silhouette stack.
  * @param userService The user service implementation.
  * @param socialProviderRegistry The social provider registry.
  */
class SocialAuthController @Inject()(implicit val silhouette: Silhouette[DefaultEnv],
                                     ec: ExecutionContext,
                                     userService: UserService,
                                     socialProviderRegistry: SocialProviderRegistry)
    extends InjectedController
    with I18nSupport
    with Logger {

  val loginCount = Kamon.metrics.counter("login")

  /**
    * Authenticates a user against a social provider.
    *
    * @param provider The ID of the provider to authenticate against.
    * @return The result to display.
    */
  def authenticate(provider: String) = Action.async { implicit request =>
    (socialProviderRegistry.get[SocialProvider](provider) match {
      case Some(p: SocialProvider with CommonSocialProfileBuilder) =>
        p.authenticate().flatMap {
          case Left(result) => Future.successful(result)
          case Right(authInfo) =>
            for {
              profile       <- p.retrieveProfile(authInfo)
              user          <- userService.save(profile)
              authenticator <- silhouette.env.authenticatorService.create(profile.loginInfo)
              value         <- silhouette.env.authenticatorService.init(authenticator)
              result        <- silhouette.env.authenticatorService.embed(value, Redirect(routes.Application.index("", "0")))
            } yield {
              loginCount.increment()
              silhouette.env.eventBus.publish(LoginEvent(user, request))
              result
            }
        }
      case _ => Future.failed(new ProviderException(s"Cannot authenticate with unexpected social provider $provider"))
    }).recover {
      case e: ProviderException =>
        logger.error("Unexpected provider error", e)
        Redirect(routes.Application.index("", "0"))
    }
  }
}
