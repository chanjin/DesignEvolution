name := "DesignEvolution"

version := "1.0"

scalaVersion := "2.11.6"

libraryDependencies ++= Seq("org.specs2" %% "specs2-core" % "3.0.1" % "test")

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

scalacOptions in Test ++= Seq("-Yrangepos")

libraryDependencies += "joda-time" % "joda-time" % "2.7"

libraryDependencies += "org.scala-lang.modules" % "scala-parser-combinators_2.11" % "1.0.2"

libraryDependencies += "org.apache.bcel" % "bcel" % "5.2"
