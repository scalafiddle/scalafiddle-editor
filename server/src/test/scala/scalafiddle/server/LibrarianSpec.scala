package scalafiddle.server

import org.scalatest._

class LibrarianSpec extends WordSpec with Matchers {
  "Librarian" should {
    "load local libraries" in {
      val librarian = new Librarian(() => scala.io.Source.fromInputStream(getClass.getResourceAsStream("/libraries.json"), "UTF-8"))
      librarian.libraries should not be empty
    }
  }
}
