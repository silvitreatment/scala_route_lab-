addSbtPlugin("com.thesamet" % "sbt-protoc" % "1.0.7")

libraryDependencies += "com.thesamet.scalapb" %% "compilerplugin" % "0.11.17"

// Formatter
addSbtPlugin("org.scalameta" % "sbt-scalafmt" % "2.5.2")

// Linter / rewrite rules
addSbtPlugin("ch.epfl.scala" % "sbt-scalafix" % "0.12.1")
