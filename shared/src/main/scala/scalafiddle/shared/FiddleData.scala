package scalafiddle.shared

case class FiddleId(id: String, version: Int)

case class Library(name: String, group: String, artifact: String, version: String, compileTimeOnly: Boolean, scalaVersions: Seq[String])

object Library {
  def stringify(lib: Library) =
    if (lib.compileTimeOnly)
      s"${lib.group} %% ${lib.artifact} % ${lib.version}"
    else
      s"${lib.group} %%% ${lib.artifact} % ${lib.version}"
}

case class FiddleData(
  name: String,
  description: String,
  sourceCode: String,
  libraries: Seq[Library],
  forced: Seq[Library],
  available: Seq[Library]
)

