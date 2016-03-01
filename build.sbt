import com.typesafe.sbt.packager.docker.{Cmd, ExecCmd}

name := """codacy-engine-checkstyle"""

version := "1.0-SNAPSHOT"

val languageVersion = "2.11.7"

scalaVersion := languageVersion

resolvers ++= Seq(
  "Typesafe Repo" at "http://repo.typesafe.com/typesafe/releases/",
  "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/releases"
)

libraryDependencies ++= Seq(
  "com.typesafe.play" %% "play-json" % "2.3.8" withSources(),
  "org.scala-lang.modules" %% "scala-xml" % "1.0.4" withSources(),
  "com.codacy" %% "codacy-engine-scala-seed" % "2.6.31"
)

enablePlugins(JavaAppPackaging)

enablePlugins(DockerPlugin)

version in Docker := "1.0"

val installAll =
  s"""apk update && apk add bash""".stripMargin.replaceAll(System.lineSeparator(), " ")

mappings in Universal <++= (resourceDirectory in Compile) map { (resourceDir: File) =>
  val src = resourceDir / "docs"
  val dest = "/docs"

  for {
    path <- (src ***).get
    if !path.isDirectory
  } yield path -> path.toString.replaceFirst(src.toString, dest)
}

mappings in Universal <++= (baseDirectory in Compile) map { (directory: File) =>
  val src = directory / "jar"

  for {
    path <- (src ***).get
    if !path.isDirectory
  } yield path -> src.toPath.relativize(path.toPath).toString
}

val dockerUser = "docker"
val dockerGroup = "docker"

daemonUser in Docker := dockerUser

daemonGroup in Docker := dockerGroup

dockerBaseImage := "frolvlad/alpine-oraclejdk8"

dockerCommands := dockerCommands.value.flatMap {
  case cmd@Cmd("WORKDIR", _) => List(cmd,
    Cmd("RUN", installAll)
  )
  case cmd@(Cmd("ADD", "opt /opt")) => List(cmd,
    Cmd("RUN", "mv /opt/docker/docs /docs"),
    Cmd("RUN", "mv /opt/docker/checkstyle-6.16-all.jar /opt/docker/checkstyle.jar"),
    Cmd("RUN", "adduser -u 2004 -D docker"),
    ExecCmd("RUN", Seq("chown", "-R", s"$dockerUser:$dockerGroup", "/docs"): _*)
  )
  case other => List(other)
}