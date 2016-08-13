package scalafiddle.shared

import scala.concurrent.Future

trait Api {
  def save(fiddle: FiddleData): Future[Either[String, FiddleId]]

  def update(fiddle: FiddleData, id: String): Future[Either[String, FiddleId]]

  def fork(fiddle: FiddleData, id: String, version: Int): Future[Either[String, FiddleId]]
}
