package scalafiddle.server

import scalafiddle.shared.FiddleData

object HTMLFiddle {
  val fiddleStart = """\s*// \$FiddleStart\s*$""".r
  val fiddleEnd   = """\s*// \$FiddleEnd\s*$""".r

  // separate source code into pre,main,post blocks
  def extractCode(src: String): (List[String], List[String], List[String]) = {
    val lines = src.split("\n")
    val (pre, main, post) = lines.foldLeft((List.empty[String], List.empty[String], List.empty[String])) {
      case ((preList, mainList, postList), line) =>
        line match {
          case fiddleStart() =>
            (line :: mainList ::: preList, Nil, Nil)
          case fiddleEnd() if preList.nonEmpty =>
            (preList, mainList, line :: postList)
          case l if postList.nonEmpty =>
            (preList, mainList, line :: postList)
          case _ =>
            (preList, line :: mainList, postList)
        }
    }
    (pre.reverse, main.reverse, post.reverse)
  }

  def classFor(hl: Highlighter.HighlightCode): String = {
    import Highlighter._
    hl match {
      case Normal  => "hl-n"
      case Comment => "hl-c"
      case Type    => "hl-t"
      case LString => "hl-s"
      case Literal => "hl-l"
      case Keyword => "hl-k"
      case Reset   => "hl-r"
    }
  }

  val logo = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAJ8AAAAoCAYAAADg1CgtAAALpUlEQVR42u1cCXQV1Rn+37x5UDhCtYpUtLSIHgiISO1yBFpOsWxSSlvKprVaQZYiIRRZNAiEhMgii0AChEAWEggIYa/s0ADdZBHasgr0sFoCGIRHyAK3/533j9zcd2fevJeHtHHuOd8h3Dv/nZl7v/nXmQfXIQvuMnREX8R3ozHf57AQGCxF7IF8GAbVQAe3/Z+2u0S4+xAdEZMQpxEM8Wx0iLcG8QHEQ3d381zyZVVHNEa8jEhGbEMUEeFENKoc8TLgFqxA4q2HdtDM3bivGPk8iG8gWiNiEVMQGxEXFERTISZS4l2DTCiDXCTeVugAT7mbVsXJ9yCiFaI/abN8xDGE3yHRokY+Trwb+C+D3bAMYt0Nq2LkewjRCzEOsQRxqJIkiyr5/Eg+Bn9EgzvS3awqSL6P7wLRokI+7ufxAKMY8tyNqqLkO/0lka+pvXnNQL9uEZJtneHbBbDTMLedoLm7UVWUfIfuleYLEI4HEssRBYgNsBqGw9vwSwMJ0BPiMMRwm0u+SFFEQUu1YC23CbEKzkAKJMHvoB08626IS77QuAHZrBgWsVLIYbdhMWOwDP9dwjA6vY3jxxFzEL9F3K8m3QYMI8Yj4ZpDDajmboRLvmD4EZxkJQg0kUiyJYilBuF4/2VYwE55UthOLbnwoDbttVLIfcIfyAkqqhM8Sfwhkc7Vci75BPJxbXaTSBbQZksReQbxruH4p5757Ig2i+3RprCtWiJb4Y1n8/U/sCm+gSzR19ef743veBlJhtqwQp7uukG8bUYw0Rl+4C662wINiXKSE4wTjZOOa7qLkM5OeGaz/dpU9ictma31jmE5+gg2V49j032DWJLvdTbe15e9h6RL04eyfO9odlSbyYk7pQhNqx+yvzCzt4yAYjtshmRoA43dBXfbnXYe0v59QJvOdmgT2DokWa4+8guSTfD1Ywm+PmySbwCb6RtsaLkl+ii2wZvA/qJNYoeRcJeQqLcMLZnHzfC4O8TjlQlOvLWQD6PchXZbcMvVR9Sfp8f1mq3HzkrRY8+l6rEsU38TtVk82+JNNEh2EMl5Ev26QvTvuHkO+H4Bk4w+nmGuyXSPq1iZ2AbvQE93kd2mboWQ/vAVWNjpKmSOLvSk773gSWNXIQO1WDZptADJuA9YhpEt9wkFsskYZ2o9hvquEH29r7nv27nNxuc7wdMlJQiu0coR/O9ie5LZki+g9XbAW9BVdcrqiFaI1xEDEM8jHvofWIqXETsRmxFbEdEKx5si+EK0DwF+TAzJxCEKEFsQmxD1wzjf0ErIdkTsoDXYQf+PxryW0e6pKCaUDfLdhg/gLMyFmgbPKrTRhpsJyM2KuIFYieiGuFeqcrp0TT+P0ryLFfdrhSySWSb1Px3G+fIrITtEkh0SpXkjy/OFS77AywA74X14RTyNRtrEyQasv0fkGyddR/sozZsVBvlyokC+dEk2JgzZPpJsnyjN+6WQb1opPujXYTm0rphWiVcs9DXSeHJ/chUjX7riHm9ZkC+NZPoh1pE1WIF4zCVfaLzK8KEtRu0nNF47uySR7jeI2og6iO70xPOxIoTH5nJrIJ5E8NdcvunwFvk5+Hv3TRAPRIl8/DoaIFqQ7+MLgww/QzxOmyeCa7d6YW4fX4NnpLXIcEiSr9M5v3OXyFeLjmlGexA++QLltUDlgwcjxUa1I1M85jxiPuL5gMndBLtgvPhVWYx00YctLpYHHj+1GONzzENcFOa5idhLWkJu3yIyfSRpVz85zC9GQD7uOvwCsYiuw9ReZYjTiBTEow7IV8cBqcYgjiIOIPYR0cXGvyVYTWtgrkUePdBjQ5CkBl1rEY2XU6DVlPagsuR7jpRJkeTTB6+7ST6ztMYj33KhhhvI5eUYJLwAaeWXYUEBEnAaygxCPEMfEBm5vVLIxZ1Yi+qggUwc8aL5Zk222ChV+7WwyFbYgLiPjudP820H/tWkMMmX6WDO84ooUN60+x3cs+zziV9M8fpkscX5+cO41oYk/Nz/tJC9ilhKZIyUfIPDCKr447v4DCdZOSWLiyCDnfXMY0e1WWyfNpUVaO+y9d6xLEsfzqse5R9p7z3M38HzK14I5bm9U0bQCHZm10QJhfTvIL5nsQnNLG7gcwsCApnWMurjmiOJiPUuLbAo0yYM8o0UxhbT8RxrJLmCEOTri/iJkGLh6YwXEA862Gj+gF1xuB4qkqxXjN+UfNBbEZKvnWJ/55CiOSuNTTMkjnpm7i3Qktk6JFiePoot1N9kqfqQoPJaih7LdmsTuZlt6DdyeSrybUGd313ltL3k4Ingqr+LJLdDQbAW5E8MpIU7SzfTW5DrRpsstyaWT2Fo8nFzlWjhay6XZBuGCDhUeMHBRsupkFJEf1qPloi/2pCvuWJsBqIumXVVVB4O+U4KY59KrgL3wfZI8o1gizcRJvr6t5yjD5kwT487w+u6GUjAZd63jRrun5Fwh7T3jVenuCkugUUx1h90/w3VS28rU8IHTjjYhDfo+AZS/yeKOetT0tqqNSKf8PeUt6sj+Y3bIww4+FyDiOD8GoZLsp0jIF9XG5kGFg/jAIVfelg6xkw7JEv9uYr7yo2QfD+UxqYq5u4mHTOeE6fFZVj44lXISECC7eflNW56/WiCAzXcvAp+3zXF6/B3PuheB23tv6v1UiUhy8IUm6hJjmukKZi2iF0Wc5cIf28Kg3xcoU803OTgOcuk/3ew2TT+EP0DcYhwBHGcqj5WMmaq5bjQV2zx4MlprSct8o1tFbKtIyTfG9LYGcS/EMcIh+i+xWNW4R1knzfLa2awUWpfww0iHw80ynGudoZVc9xqkIZYqdjMDhR9iX0jHM77I4U/tJH8S9lf2hgG+WR/aT9du8qBb2+zaar3yjwh/MTHFKbtM4v7l7XwE9SfI/V/XyHbKkLyDQ0jkW5iFyfPJ5X9ECgQaMywIwRPUbwVRnQ3iLSf2PehA+J5KOIzZf4u+WheKQWw2oZ8oibqLDnkfUNom+dsNu3bESSKzQj6gNBXrkjBgGHOKso+Qv1zpf7XFLJtpGN6OLgmsy7OpHXtTuQ10YX80i5U128ZSZI5Rv5FAf7qFPf1bLLDpsrlOath9PSbH248onCUX6Wxy1J/bymKTkUk0CZopE3F40crzP5NiZxWm9bFou5bqri/yZJs90pWBtKDnPNAmyX1bxHWESjhLNfOWwoKQOw/JwVGPA2zWzpGfBFzgTQm+lf1pLHNFoqhc2UrHDEVf1EgGx/BTejVW+WHjahUpXbPkckqU4w1tMkb5ZCWOiL1ryQCXhD6zlIUWZfIpDKRCXSuJKn/BJW7eiuuYz75UjEKR57RQ1M3iuRrIgRQ8rmO0bWnWazzCVoXUBDTTw9wMu2Hyj9+2sJsHyBCDqLxeYoafSuyPL2kfdkYKfmeEsl3Gy3mRbyGuka1RtlSw/QFxkryKxzKmdoywSKXZf59UhpbLkTjqnkvUZR81ebcVxT+ZFOLcpcTx9hOZmyIdbigCIpqC4GYnWypgoRdQ5z3FI1XV0TazKJA8FKk5GsUrPk2omroZLeY7SmM99vc+H+g4is8YkuSolS5XNdBUu+rLI4dSEQS+9YIZny3hRyQg/6ZYmwflfNGWwQWkbyKFEomXqpEmDhIdWNZC9aS/NfTCtkiuseeoH61rI4UbYtkN2vbtanMZ7XHHyN+HInZ5YFJHEIL9vm2o7rp4SQSfYCIOII0VCKZ0B7SAoFFvTYOMRORTWmPX9kc34mSqJlEjMZSSiGRrqGdJNePap/T6XxNyVc0C/nDac4Zkm/no+MTyVeqJZxrCCWD+zgsrzmReZzWkZvo2YLmN4v6w+la4gSza7aaFDTNJvlhUmD2irA+j0oZijiyZlPoYW6omD+Gri2V1iqJ9iPoxYISgWD870LEPsRqxGTEYPpVUc3qd1b476rwn7dwm9vCakieMfRjj/z3+OohaoT7a1Iu+dwWUWOMuXBxT+AugguXfC6+evgv/31fVmose5sAAAAASUVORK5CYII="

  val codeStyle =
    """html, body, object, embed, iframe {
    |    margin:  0;
    |    padding: 0;
    |    height: 100%;
    |    width: 100%;
    |    background: white;
    |    color: white;
    |    box-sizing: border-box;
    |}
    |body {
    |    position: relative;
    |    font-size: 16px;
    |    color: #333;
    |    text-align: left;
    |    direction: ltr;
    |}
    |.sf-file {
    |    height: 100%;
    |    margin-bottom: 1em;
    |    font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
    |    border: 1px solid #ddd;
    |    border-bottom: 1px solid #ccc;
    |    border-radius: 3px;
    |    display: flex;
    |    flex-direction: column;
    |    box-sizing: border-box;
    |}
    |.sf-data {
    |    word-wrap: normal;
    |    background-color: #fff;
    |    flex: 1;
    |    overflow: auto;
    |}
    |.sf-data table {
    |    border-collapse: collapse;
    |    padding: 0;
    |    margin: 0;
    |    font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
    |    font-size: 12px;
    |    font-weight: normal;
    |    line-height: 1.4;
    |    color: #333;
    |    background: #fff;
    |    border: 0;
    |}
    |.sf-meta {
    |    padding: 5px;
    |    overflow: hidden;
    |    color: #586069;
    |    background-color: #f7f7f7;
    |    border-radius: 0 0 2px 2px;
    |    box-shadow: 0 2px 4px 0px rgba(199, 199, 222, 0.5);
    |    z-index: 1;
    |    flex-shrink: 1;
    |}
    |.sf-meta a {
    |    font-weight: 600;
    |    color: #666;
    |    text-decoration: none;
    |    border: 0;
    |}
    |.line-number {
    |    min-width: inherit;
    |    padding: 1px 10px !important;
    |    background: transparent;
    |    width: 1%;
    |    font-size: 12px;
    |    font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
    |    line-height: 20px;
    |    color: rgba(27,31,35,0.3);
    |    text-align: right;
    |    white-space: nowrap;
    |    vertical-align: top;
    |    cursor: pointer;
    |    -webkit-user-select: none;
    |    -moz-user-select: none;
    |    -ms-user-select: none;
    |    user-select: none;
    |}
    |.line-number::before {
    |    content: attr(data-line-number);
    |}
    |.line-code {
    |    padding: 1px 10px !important;
    |    text-align: left;
    |    background: transparent;
    |    border: 0;
    |    overflow: visible;
    |    font-family: "SFMono-Regular", Consolas, "Liberation Mono", Menlo, Courier, monospace;
    |    font-size: 12px;
    |    color: #24292e;
    |    word-wrap: normal;
    |    white-space: pre;
    |}
    |.hl-k {
    |    color: #e41d79;
    |}
    |.hl-n {
    |    color: #24292e;
    |}
    |.hl-s {
    |    color: #0eab5c;
    |}
    |.hl-c {
    |    color: #969896;
    |}
    |.hl-t {
    |    color: #815abb;
    |}
    |.hl-l {
    |    color: #00b1ec;
    |}
    """.stripMargin

  def process(fd: FiddleData, fiddleURL: String, fiddleId: String): String = {
    import scalatags.Text.all._
    import scalatags.Text.tags2.{style => styleTag, title => titleTag}

    val name = fd.name.replaceAll("\\s", "") match {
      case empty if empty.isEmpty => "Anonymous"
      case nonEmpty               => nonEmpty
    }
    val codeHtml = {
      // highlight source code
      val (pre, main, post) = extractCode(fd.sourceCode)
      // remove indentation
      val indentation =
        main.foldLeft(99)((ind, line) => if (line.isEmpty) ind else line.takeWhile(_ == ' ').length min ind)
      val highlighted = Highlighter
        .defaultHighlightIndices((pre ::: main.map(_.drop(indentation)) ::: post).mkString("\n").toCharArray)
        .slice(pre.size, pre.size + main.size)
      // generate HTML for code lines

      highlighted.zipWithIndex.map {
        case (line, lineNo) =>
          tr(
            td(cls := "line-number", data("line-number") := lineNo + 1),
            td(cls := "line-code")(line.map {
              case (content, highlight) =>
                span(cls := classFor(highlight))(content)
            })
          )
      }
    }
    // create the final HTML
    html(
      head(
        titleTag(s"ScalaFiddle - $name"),
        styleTag(raw(codeStyle)),
        base(target := "_blank")
      ),
      body(
        div(cls := "sf-file")(
          div(cls := "sf-meta")(
            a(href := fiddleURL, style := "float:right")(img(src := logo, height := 20, alt := "ScalaFiddle")),
            a(href := fiddleURL)(name)
          ),
          div(cls := "sf-data")(
            table(
              tbody(codeHtml)
            )
          )
        ),
        script(raw(
          s"""setTimeout(function() {
            |  var dataDiv = document.querySelector(".sf-data table");
            |  var metaDiv = document.querySelector(".sf-meta");
            |  var data = {height:Math.min(dataDiv.scrollHeight + metaDiv.scrollHeight + 2, 600), fiddleId:"$fiddleId"};
            |  window.parent.postMessage(["embedHeight", data], "*");
            |}, 10);
          """.stripMargin))
      )
    ).render
  }
}
