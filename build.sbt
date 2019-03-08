import de.heikoseeberger.sbtheader.FileType

lazy val root = project.in(file(".")).enablePlugins(AutomateHeaderPlugin)

name := "transmogrifai-demo"

scalaVersion := "2.11.12"
lazy val sparkDependencies = Seq(
  "org.apache.spark" %% "spark-core" % sparkVersion,
  "org.apache.spark" %% "spark-mllib" % sparkVersion,
  "org.apache.spark" %% "spark-streaming" % sparkVersion,
  "org.apache.spark" %% "spark-sql" % sparkVersion
)
val transmogrifaiVersion = "0.5.0"

resolvers += Resolver.bintrayRepo("salesforce", "maven")
val sparkVersion = "2.3.2"

libraryDependencies += "com.salesforce.transmogrifai" %% "transmogrifai-core" % transmogrifaiVersion
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.3"
libraryDependencies += "com.typesafe.akka" %% "akka-stream" % "2.5.3"
libraryDependencies += "com.typesafe.akka" %% "akka-http" % "10.0.9"
libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.21"


libraryDependencies ++= sparkDependencies//.map(_ % Provided)

libraryDependencies ++= sparkDependencies.map(_ % Test)

(version in avroConfig) := "1.7.7"

(stringType in avroConfig) := "String"

// license header stuff

organizationName := "softwarepassion.com"

startYear := Some(2019)

licenses += "BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause")

headerMappings += FileType("html") -> HeaderCommentStyle.twirlStyleBlockComment

headerLicense := Some(
  HeaderLicense.Custom(
    """|Copyright (c) 2018, softwarepassion.com
       |All rights reserved.
       |""".stripMargin
  )
)
