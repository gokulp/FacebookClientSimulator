enablePlugins(JavaServerAppPackaging)

name := "Simulator"

version := "0.1"

//organization := "com.danielasfregola"

scalaVersion := "2.11.5"
//scalaVersion := "2.10.1"

resolvers ++= Seq("Typesafe Repository" at "http://repo.typesafe.com/typesafe/releases/",
  "Spray Repository"    at "http://repo.spray.io")

libraryDependencies ++= {
  val AkkaVersion       = "2.3.9"
  val SprayVersion      = "1.3.2"
  val Json4sVersion     = "3.2.11"
  Seq(
    "com.typesafe.akka" %% "akka-actor"      % AkkaVersion,
    "io.spray"          %% "spray-can"       % SprayVersion,
    "io.spray"          %% "spray-routing"   % SprayVersion,
    "io.spray"          %% "spray-client"   % SprayVersion,
    "io.spray"          %% "spray-json"      % "1.3.1",
    "com.typesafe.akka" %% "akka-slf4j"      % AkkaVersion,
    "ch.qos.logback"    %  "logback-classic" % "1.1.2",
    "org.json4s"        %% "json4s-native"   % Json4sVersion,
    "org.json4s"        %% "json4s-ext"      % Json4sVersion,
    "com.typesafe.akka" %% "akka-testkit"    % AkkaVersion  % "test",
    "io.spray"          %% "spray-testkit"   % SprayVersion % "test",
    "org.specs2"        %% "specs2"          % "2.3.13"     % "test"
    ,"org.apache.commons" % "commons-lang3" % "3.1",
    "commons-codec" % "commons-codec" % "1.9"
  )
}

//autoScalaLibrary := false

// Assembly settings
mainClass in Global := Some("Simulator.main")

jarName in assembly := "simulator.jar"
