package scalafiddle.server.dao

case class FiddleAccess(
    id: Int,
    fiddleId: String,
    version: Int,
    timeStamp: Long,
    userId: Option[String],
    embedded: Boolean,
    sourceIP: String
)

trait AccessDAO {
  self: DriverComponent =>

  import driver.api._

  class Accesses(tag: Tag) extends Table[FiddleAccess](tag, "access") {
    def id        = column[Int]("id", O.PrimaryKey, O.AutoInc)
    def fiddleId  = column[String]("fiddle_id")
    def version   = column[Int]("version")
    def timeStamp = column[Long]("timestamp")
    def userId    = column[Option[String]]("user_id")
    def embedded  = column[Boolean]("embedded")
    def sourceIP  = column[String]("source_ip")

    def * = (id, fiddleId, version, timeStamp, userId, embedded, sourceIP) <> (FiddleAccess.tupled, FiddleAccess.unapply)
  }

  val accesses = TableQuery[Accesses]

  def insertAccess(access: FiddleAccess) =
    accesses += access

  def findAccesses(fiddleId: String) =
    accesses.filter(_.fiddleId === fiddleId).result

  def findAccesses(startTime: Long, endTime: Long) =
    accesses.filter(r => r.timeStamp < endTime && r.timeStamp >= startTime).result
}
