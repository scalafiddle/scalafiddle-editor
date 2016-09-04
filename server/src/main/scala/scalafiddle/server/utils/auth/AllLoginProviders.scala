package scalafiddle.server.utils.auth

import scalafiddle.shared.LoginProvider

object AllLoginProviders {
  val providers: Map[String, LoginProvider] = Map(
    "github" -> LoginProvider("github", "GitHub", "/assets/images/providers/github.png")
  )
}
