import sbt.Keys._
import sbt.Project.projectToRef

resolvers in ThisBuild += Resolver.jcenterRepo

// a special crossProject for configuring a JS/JVM/shared structure
lazy val shared = (crossProject.crossType(CrossType.Pure) in file("shared"))
  .settings(
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    libraryDependencies ++= Settings.sharedDependencies.value
  )
  // set up settings specific to the JS project
  .jsConfigure(_ enablePlugins ScalaJSWeb)

lazy val sharedJVM = shared.jvm.settings(name := "sharedJVM")

lazy val sharedJS = shared.js.settings(name := "sharedJS")

// instantiate the JS project for SBT with some additional settings
lazy val client = (project in file("client"))
  .settings(
    name := "client",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Settings.sharedDependencies.value ++ Settings.scalajsDependencies.value,
    jsDependencies ++= Settings.jsDependencies.value,
    jsEnv := new org.scalajs.jsenv.jsdomnodejs.JSDOMNodeJSEnv(),
    // yes, we want to package JS dependencies
    skip in packageJSDependencies := false,
    // use Scala.js provided launcher code to start the client app
    scalaJSUseMainModuleInitializer := true
  )
  .enablePlugins(ScalaJSPlugin, ScalaJSWeb)
  .dependsOn(sharedJS)

// Client projects (just one in this case)
lazy val clients = Seq(client)

// instantiate the JVM project for SBT with some additional settings
lazy val server = (project in file("server"))
  .settings(
    name := "server",
    version := Settings.version,
    scalaVersion := Settings.versions.scala,
    scalacOptions ++= Settings.scalacOptions,
    libraryDependencies ++= Settings.sharedDependencies.value ++ Settings.jvmDependencies.value ++ Seq(
      filters, guice, ehcache
    ),
    // connect to the client project
    scalaJSProjects := clients,
    pipelineStages in Assets := Seq(scalaJSPipeline),
    pipelineStages := Seq(digest, gzip),
    // compress CSS
    LessKeys.compress in Assets := true,
    scriptClasspath := Seq("../config/") ++ scriptClasspath.value,
    mappings in Universal := (mappings in Universal).value.filter {
      case (file, fileName) => !fileName.endsWith("local.conf")
    },
    mappings in (Compile, packageDoc) := Seq(),
    javaOptions in Universal ++= Seq(
      "-Dpidfile.path=/dev/null"
    ),
    dockerfile in docker := {
      val appDir: File = stage.value
      val targetDir    = "/app"

      new Dockerfile {
        from("openjdk:8")
        entryPoint(s"$targetDir/bin/${executableScriptName.value}")
        copy(appDir, targetDir)
        expose(8080)
      }
    },
    imageNames in docker := Seq(
      ImageName(
        namespace = Some("scalafiddle"),
        repository = "scalafiddle-editor",
        tag = Some("latest")
      ),
      ImageName(
        namespace = Some("scalafiddle"),
        repository = "scalafiddle-editor",
        tag = Some(version.value)
      )
    )
  )
  .enablePlugins(PlayScala, SbtWeb, sbtdocker.DockerPlugin)
  .disablePlugins(PlayLayoutPlugin) // use the standard directory layout instead of Play's custom
  .aggregate(clients.map(projectToRef): _*)
  .dependsOn(sharedJVM)

// loads the Play server project at sbt startup
onLoad in Global := (Command.process("project server", _: State)) compose (onLoad in Global).value
