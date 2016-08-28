package scalafiddle.server.dao

import com.mohiva.play.silhouette.api.LoginInfo

import scalafiddle.server.models.User

trait UserDAO {
  self: DriverComponent =>

  import driver.api._

  implicit val loginInfoMapper = MappedColumnType.base[LoginInfo, String](
    info => info.providerID + "\n" + info.providerKey,
    string => {
      val parts = string.split('\n')
      LoginInfo(parts(0), parts(1))
    }
  )

  class Users(tag: Tag) extends Table[User](tag, "user") {
    def userID = column[String]("user_id", O.PrimaryKey)
    def loginInfo = column[LoginInfo]("login_info")
    def firstName = column[Option[String]]("first_name")
    def lastName = column[Option[String]]("last_name")
    def fullName = column[Option[String]]("full_name")
    def email = column[Option[String]]("email")
    def avatarURL = column[Option[String]]("avatar_url")
    def activated = column[Boolean]("activated")

    def * = (userID, loginInfo, firstName, lastName, fullName, email, avatarURL, activated) <> (User.tupled, User.unapply)
  }

  val users = TableQuery[Users]

  def insert(user: User) =
    users += user

  def update(user: User) =
    users.filter(_.userID === user.userID).update(user)

  def findUser(id: String) =
    users.filter(_.userID === id).result.headOption

  def findUser(loginInfo: LoginInfo) =
    users.filter(_.loginInfo === loginInfo).result.headOption
}
