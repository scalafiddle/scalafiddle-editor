package scalafiddle.server.dao

import slick.driver.JdbcProfile

class FiddleDAL(val driver: JdbcProfile) extends
  FiddleDAO with
  UserDAO with
  DriverComponent