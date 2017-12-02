package scalafiddle.server

/**
  * Borrowed from https://github.com/lihaoyi/Ammonite/blob/master/amm/repl/src/main/scala/ammonite/repl/Highlighter.scala
  */
import fastparse.all._

import scala.annotation.tailrec
import scalaparse.Scala._
import scalaparse.syntax.Identifiers._

object Highlighter {
  sealed trait HighlightCode

  case object Normal  extends HighlightCode
  case object Comment extends HighlightCode
  case object Type    extends HighlightCode
  case object LString extends HighlightCode
  case object Literal extends HighlightCode
  case object Keyword extends HighlightCode
  case object Reset   extends HighlightCode

  object BackTicked {
    private[this] val regex = "`([^`]+)`".r
    def unapplySeq(s: Any): Option[List[String]] = {
      regex.unapplySeq(s.toString)
    }
  }

  def defaultHighlightIndices(buffer: Array[Char]) = Highlighter.defaultHighlightIndices0(
    CompilationUnit,
    buffer
  )

  def defaultHighlightIndices0(parser: P[_], buffer: Array[Char]) = Highlighter.highlightIndices(
    parser,
    buffer, {
      case Operator | SymbolicKeywords                      => Keyword
      case Literals.Expr.Interp | Literals.Pat.Interp       => Reset
      case Literals.Comment                                 => Comment
      case Literals.Expr.String                             => LString
      case ExprLiteral                                      => Literal
      case TypeId                                           => Type
      case BackTicked(body) if alphaKeywords.contains(body) => Keyword
    },
    Reset
  )

  type HighlightLine = Vector[(String, HighlightCode)]
  def highlightIndices(parser: Parser[_],
                       buffer: Array[Char],
                       ruleColors: PartialFunction[Parser[_], HighlightCode],
                       endColor: HighlightCode): Vector[HighlightLine] = {
    val indices = collection.mutable.Buffer((0, endColor))
    var done    = false
    val input   = buffer.mkString
    parser.parse(
      input,
      instrument = (rule, idx, res) => {
        for (color <- ruleColors.lift(rule)) {
          val closeColor = indices.last._2
          val startIndex = indices.length
          indices += ((idx, color))

          res() match {
            case s: Parsed.Success[_] =>
              val prev = indices(startIndex - 1)._1

              if (idx < prev && s.index <= prev) {
                indices.remove(startIndex, indices.length - startIndex)
              }
              while (idx < indices.last._1 && s.index <= indices.last._1) {
                indices.remove(indices.length - 1)
              }
              indices += ((s.index, closeColor))
              if (s.index == buffer.length) done = true
            case f: Parsed.Failure
                if f.index == buffer.length
                  && (WL ~ End).parse(input, idx).isInstanceOf[Parsed.Failure] =>
              // EOF, stop all further parsing
              done = true
            case _ => // hard failure, or parsed nothing. Discard all progress
              indices.remove(startIndex, indices.length - startIndex)
          }
        }
      }
    )
    // Make sure there's an index right at the start and right at the end! This
    // resets the colors at the snippet's end so they don't bleed into later output
    indices += ((999999999, endColor))
    // create spans
    val spans = indices
      .sliding(2)
      .map {
        case Seq((i0, Reset), (i1, c1)) =>
          (new String(buffer.slice(i0, i1)), Normal)
        case Seq((i0, c0), (i1, Reset)) =>
          (new String(buffer.slice(i0, i1)), c0)
        case Seq((i0, c0), (i1, _)) =>
          (new String(buffer.slice(i0, i1)), c0)
      }
      .filter(_._1.nonEmpty)
      .toVector
    // split lines
    val (sourceLines, lastLine) =
      spans.foldLeft((Vector.empty[HighlightLine], Vector.empty[(String, HighlightCode)])) {
        case ((lines, line), (str, code)) =>
          if (str.contains('\n')) {
            @tailrec def splitLine(lines: Vector[HighlightLine] = lines,
                                   line: HighlightLine = line,
                                   str: String = str): (Vector[HighlightLine], HighlightLine) = {
              if (str.isEmpty) {
                (lines, line)
              } else if (str.contains('\n')) {
                val pre = str.substring(0, str.indexOf('\n'))
                val rem = str.substring(str.indexOf('\n') + 1)
                splitLine(lines :+ (line :+ ((pre, code))), Vector.empty, rem)
              } else {
                (lines, line :+ ((str, code)))
              }
            }
            splitLine()
          } else {
            (lines, line :+ ((str, code)))
          }
      }

    sourceLines :+ lastLine
  }
}
