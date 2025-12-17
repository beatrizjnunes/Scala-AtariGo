ThisBuild / scalaVersion := "2.13.12"

lazy val root = (project in file("."))
  .settings(
    name := "AtariGo",
    version := "1.0",
    libraryDependencies ++= Seq(
      "org.openjfx" % "javafx-controls" % "17.0.2",
      "org.openjfx" % "javafx-fxml" % "17.0.2",
      "org.openjfx" % "javafx-base" % "17.0.2",
      "org.openjfx" % "javafx-graphics" % "17.0.2"
    )
  )

