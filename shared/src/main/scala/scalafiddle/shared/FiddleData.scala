package scalafiddle.shared

case class FiddleId(id: String, version: Int) {
  override def toString: String = s"$id/$version"
}

case class Library(name: String,
                   organization: String,
                   artifact: String,
                   version: String,
                   compileTimeOnly: Boolean,
                   scalaVersions: Seq[String],
                   extraDeps: Seq[String],
                   group: String,
                   docUrl: String)

object Library {
  def stringify(lib: Library) =
    if (lib.compileTimeOnly)
      s"${lib.organization} %% ${lib.artifact} % ${lib.version}"
    else
      s"${lib.organization} %%% ${lib.artifact} % ${lib.version}"
}

case class FiddleData(
    name: String,
    description: String,
    sourceCode: String,
    libraries: Seq[Library],
    forced: Seq[Library],
    available: Seq[Library],
    author: Option[UserInfo]
)
