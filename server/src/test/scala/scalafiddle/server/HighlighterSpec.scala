package scalafiddle.server

import org.scalatest._

class HighlighterSpec extends WordSpec with Matchers {
  "Highlighter" should {
    "highlight Scala code" in {
      val code =
        """
          |object Highlighter {
          |  sealed trait HighlightCode
          |
          |  case object Comment extends HighlightCode
          |  case object Type    extends HighlightCode
          |  case object Literal extends HighlightCode
          |  case object Keyword extends HighlightCode
          |  case object Reset   extends HighlightCode
          |
          |  object BackTicked {
          |    private[this] val regex = "`([^`]+)`".r
          |    def unapplySeq(s: Any): Option[List[String]] = {
          |      regex.unapplySeq(s.toString)
          |    }
          |  }
          |}
        """.stripMargin

      val res = Highlighter.defaultHighlightIndices(code.toCharArray)
      println(res)
    }

    "highlight string interpolation" in {
      val code =
        """
          |object Highlighter {
          |  val x = 5
          |  val y = s"Test ${x+2} thing"
          |}
        """.stripMargin
      val res = Highlighter.defaultHighlightIndices(code.toCharArray)
      println(res)
    }

    "highlight multiline string" in {
      val code =
        s"""
          |object A {
          |  ${"\"\"\""}
          |  Test
          |  Test
          |${"\"\"\""}
          |}
        """.stripMargin
      val res = Highlighter.defaultHighlightIndices(code.toCharArray)
      println(res)
    }

    "highlight multiline comment" in {
      val code =
        s"""
          |object A {
          |  /*
          |  Test
          |  Test
          |  */
          |}
        """.stripMargin
      val res = Highlighter.defaultHighlightIndices(code.toCharArray)
      println(res)
    }

    "highlight long text" in {
      val code =
        s"""|import fiddle.Fiddle, Fiddle.println
       |import scalajs.js
       |
       |@js.annotation.JSExportTopLevel("ScalaFiddle")
       |object ScalaFiddle {
       |  // $$FiddleStart
       |  // Start writing your ScalaFiddle code here
       |def libraryListing(scalaVersion: String) = Action {
       |  val libStrings = librarian.libraries
       |    .filter(_.scalaVersions.contains(scalaVersion))
       |    .flatMap(lib => Library.stringify(lib) +: lib.extraDeps)
       |  Ok(write(libStrings)).as("application/json").withHeaders(CACHE_CONTROL -> "max-age=60")
       |}
       |def libraryListing(scalaVersion: String) = Action {
       |  val libStrings = librarian.libraries
       |    .filter(_.scalaVersions.contains(scalaVersion))
       |    .flatMap(lib => Library.stringify(lib) +: lib.extraDeps)
       |  Ok(write(libStrings)).as("application/json").withHeaders(CACHE_CONTROL -> "max-age=60")
       |}
       |def libraryListing(scalaVersion: String) = Action {
       |  val libStrings = librarian.libraries
       |    .filter(_.scalaVersions.contains(scalaVersion))
       |    .flatMap(lib => Library.stringify(lib) +: lib.extraDeps)
       |  Ok(write(libStrings)).as("application/json").withHeaders(CACHE_CONTROL -> "max-age=60")
       |}
       |def libraryListing(scalaVersion: String) = Action {
       |  val libStrings = librarian.libraries
       |    .filter(_.scalaVersions.contains(scalaVersion))
       |    .flatMap(lib => Library.stringify(lib) +: lib.extraDeps)
       |  Ok(write(libStrings)).as("application/json").withHeaders(CACHE_CONTROL -> "max-age=60")
       |}
       |// $$FiddleEnd
       |}
     """.stripMargin
      val res = Highlighter.defaultHighlightIndices(code.toCharArray)
      println(res)
    }
  }
}
