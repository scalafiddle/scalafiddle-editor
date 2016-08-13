package scalafiddle.server.dao

import slick.driver.JdbcProfile

trait DriverComponent {
  val driver: JdbcProfile
}
