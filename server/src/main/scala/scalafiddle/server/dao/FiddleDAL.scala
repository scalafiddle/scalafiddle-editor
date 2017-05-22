package scalafiddle.server.dao

import slick.jdbc.JdbcProfile

class FiddleDAL(val driver: JdbcProfile) extends FiddleDAO with UserDAO with AccessDAO with DriverComponent
