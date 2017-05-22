package scalafiddle.server.dao

import slick.jdbc.JdbcProfile

trait DriverComponent {
  val driver: JdbcProfile
}
