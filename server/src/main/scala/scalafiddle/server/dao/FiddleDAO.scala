package scalafiddle.server.dao

case class Fiddle(
    id: String,
    version: Int,
    name: String,
    description: String,
    sourceCode: String,
    libraries: List[String],
    scalaVersion: String,
    user: String,
    parent: Option[String] = None,
    created: Long = System.currentTimeMillis(),
    removed: Boolean = false
)

trait FiddleDAO { self: DriverComponent =>

  import driver.api._

  implicit val stringListMapper = MappedColumnType.base[List[String], String](
    list => list.mkString("\n"),
    string => string.split('\n').toList
  )

  class Fiddles(tag: Tag) extends Table[Fiddle](tag, "fiddle") {
    def id           = column[String]("id")
    def version      = column[Int]("version")
    def name         = column[String]("name")
    def description  = column[String]("description")
    def sourceCode   = column[String]("sourcecode")
    def libraries    = column[List[String]]("libraries")
    def scalaVersion = column[String]("scala_version")
    def user         = column[String]("user")
    def parent       = column[Option[String]]("parent")
    def created      = column[Long]("created")
    def removed      = column[Boolean]("removed")
    def pk           = primaryKey("pk_fiddle", (id, version))

    def * =
      (id, version, name, description, sourceCode, libraries, scalaVersion, user, parent, created, removed) <> (Fiddle.tupled, Fiddle.unapply)
  }

  val fiddles = TableQuery[Fiddles]

  def insertFiddle(fiddle: Fiddle) =
    fiddles += fiddle

  def findFiddle(id: String, version: Int) =
    fiddles.filter(f => f.id === id && f.version === version && f.removed === false).result.headOption

  def findFiddleVersions(id: String) =
    fiddles.filter(_.id === id).sortBy(_.version).result

  def findUserFiddles(userId: String) =
    fiddles.filter(_.user === userId).result

  def removeEvent(id: String, version: Int) =
    fiddles.filter(f => f.id === id && f.version === version).map(_.removed).update(true)

  def getAllEvents =
    fiddles.filter(_.name =!= "").map(f => (f.id, f.version, f.name)).result
}
