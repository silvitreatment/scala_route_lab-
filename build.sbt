import scalapb.compiler.Version.scalapbVersion

// ============================================================
// Versions
// ============================================================
val scalaTestVersion  = "3.2.19"
val zioVersion        = "2.1.6"
val pekkoVersion      = "1.0.3"
val pekkoHttpVersion  = "1.0.1"
val grpcVersion       = "1.64.0"
val scalaPbVersion    = "0.11.17"
val doobieVersion     = "1.0.0-RC5"
val redis4catsVersion = "1.7.1"
val fs2KafkaVersion   = "3.5.1"
val circeVersion      = "0.14.9"
val log4catsVersion   = "2.7.0"
val mysqlVersion      = "8.0.33"

// ============================================================
// Common settings
// ============================================================
lazy val commonSettings = Seq(
  scalaVersion := "2.13.18",
  organization := "routelab",
  scalacOptions ++= Seq(
    "-deprecation",
    "-feature",
    "-unchecked",
    "-Xlint",
    "-Ywarn-unused:imports",
    // Нужно для Scalafix SemanticDB-правил (ExplicitResultTypes, OrganizeImports и др.)
    "-Yrangepos"
  ),
  // Включаем SemanticDB — требуется для семантических правил Scalafix
  semanticdbEnabled := true,
  semanticdbVersion := "4.16.1",
  // Scalafix: путь к конфигу
  scalafixConfig := Some(file(".scalafix.conf"))
)

// ============================================================
// Dependency groups
// ============================================================
lazy val zioDeps = Seq(
  "dev.zio" %% "zio"         % zioVersion,
  "dev.zio" %% "zio-streams" % zioVersion
)

lazy val circeDeps = Seq(
  "io.circe" %% "circe-core"    % circeVersion,
  "io.circe" %% "circe-generic" % circeVersion,
  "io.circe" %% "circe-parser"  % circeVersion
)

lazy val grpcDeps = Seq(
  "com.thesamet.scalapb" %% "scalapb-runtime-grpc" % scalaPbVersion,
  "io.grpc"               % "grpc-netty"            % grpcVersion,
  "io.grpc"               % "grpc-stub"             % grpcVersion
)

lazy val dbDeps = Seq(
  "org.tpolecat" %% "doobie-core"   % doobieVersion,
  "org.tpolecat" %% "doobie-hikari" % doobieVersion,
  "mysql"         % "mysql-connector-java" % mysqlVersion
)

lazy val redisDeps = Seq(
  "dev.profunktor" %% "redis4cats-effects"  % redis4catsVersion,
  "dev.profunktor" %% "redis4cats-log4cats" % redis4catsVersion,
  "org.typelevel"  %% "log4cats-slf4j"      % log4catsVersion
)

lazy val kafkaDeps = Seq(
  "com.github.fd4s" %% "fs2-kafka" % fs2KafkaVersion
)

lazy val pekkoDeps = Seq(
  "org.apache.pekko"    %% "pekko-actor-typed" % pekkoVersion,
  "org.apache.pekko"    %% "pekko-stream"      % pekkoVersion,
  "org.apache.pekko"    %% "pekko-http"        % pekkoHttpVersion,
  "com.github.pjfanning" %% "pekko-http-circe" % "3.9.1"
)

// ============================================================
// shared — общие domain types и утилиты
// ============================================================
lazy val shared = project
  .in(file("shared"))
  .settings(commonSettings)
  .settings(
    name := "shared",
    libraryDependencies ++= zioDeps ++ circeDeps ++ Seq(
      "org.scalatest" %% "scalatest" % scalaTestVersion % Test,
    ),
  )

// ============================================================
// proto — ScalaPB codegen из .proto файлов
// ============================================================
lazy val proto = project
  .in(file("proto-gen"))
  .settings(commonSettings)
  .settings(
    name := "proto",
    libraryDependencies ++= grpcDeps ++ Seq(
      "com.thesamet.scalapb" %% "scalapb-runtime" % scalaPbVersion % "protobuf"
    ),
    Compile / PB.protoSources := Seq(
      (ThisBuild / baseDirectory).value / "proto"
    ),
    Compile / PB.targets := Seq(
      scalapb.gen(grpc = true) -> (Compile / sourceManaged).value / "scalapb"
    )
  )

// ============================================================
// tenant-service
// ============================================================
lazy val tenantService = project
  .in(file("tenant-service"))
  .dependsOn(shared, proto)
  .settings(commonSettings)
  .settings(
    name := "tenant-service",
    libraryDependencies ++= zioDeps ++ grpcDeps ++ dbDeps ++ redisDeps ++ circeDeps
  )

// ============================================================
// transit-catalog-service
// ============================================================
lazy val transitCatalogService = project
  .in(file("transit-catalog-service"))
  .dependsOn(shared, proto)
  .settings(commonSettings)
  .settings(
    name := "transit-catalog-service",
    libraryDependencies ++= zioDeps ++ grpcDeps ++ dbDeps ++ circeDeps
  )

// ============================================================
// live-position-service
// ============================================================
lazy val livePositionService = project
  .in(file("live-position-service"))
  .dependsOn(shared, proto)
  .settings(commonSettings)
  .settings(
    name := "live-position-service",
    libraryDependencies ++= zioDeps ++ grpcDeps ++ redisDeps ++ kafkaDeps ++ circeDeps
  )

// ============================================================
// eta-service
// ============================================================
lazy val etaService = project
  .in(file("eta-service"))
  .dependsOn(shared, proto)
  .settings(commonSettings)
  .settings(
    name := "eta-service",
    libraryDependencies ++= zioDeps ++ grpcDeps ++ circeDeps
  )

// ============================================================
// routing-service
// ============================================================
lazy val routingService = project
  .in(file("routing-service"))
  .dependsOn(shared, proto)
  .settings(commonSettings)
  .settings(
    name := "routing-service",
    libraryDependencies ++= zioDeps ++ grpcDeps ++ circeDeps
  )

// ============================================================
// api-gateway
// ============================================================
lazy val apiGateway = project
  .in(file("api-gateway"))
  .dependsOn(shared, proto)
  .settings(commonSettings)
  .settings(
    name := "api-gateway",
    libraryDependencies ++= zioDeps ++ grpcDeps ++ redisDeps ++ pekkoDeps ++ circeDeps
  )

// ============================================================
// root — агрегирует все подпроекты
// ============================================================
lazy val root = project
  .in(file("."))
  .aggregate(
    shared,
    proto,
    tenantService,
    transitCatalogService,
    livePositionService,
    etaService,
    routingService,
    apiGateway
  )
  .settings(commonSettings)
  .settings(
    name := "routelab"
  )
