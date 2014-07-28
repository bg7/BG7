import AssemblyKeys._

Nice.javaProject

javaVersion := "1.8"

fatArtifactSettings

name := "bg7"

description := "bg7 project"

organization := "ohnosequences"

bucketSuffix := "era7.com"

libraryDependencies ++= Seq(
	"ohnosequences" % "bioinfo-util" % "1.4.0-SNAPSHOT",
	"com.novocode" % "junit-interface" % "0.9" % "test"
)

dependencyOverrides ++= Set(
  "commons-codec" % "commons-codec" % "1.7",
  "com.fasterxml.jackson.core" % "jackson-core" % "2.1.2",
  "com.fasterxml.jackson.core" % "jackson-databind" % "2.1.2",
  "commons-beanutils" % "commons-beanutils" % "1.8.3",
  "commons-beanutils" % "commons-beanutils-core" % "1.8.3"
)

// fat jar assembly settings
mainClass in assembly := Some("com.ohnosequences.bg7.BG7")

assemblyOption in assembly ~= { _.copy(includeScala = false) }


