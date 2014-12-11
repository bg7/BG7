import AssemblyKeys._

Nice.javaProject

javaVersion := "1.8"

fatArtifactSettings

name := "bg7"
description := "Bacterial genome annotation tool"
organization := "ohnosequences"

bucketSuffix := "era7.com"

libraryDependencies ++= Seq(
	"ohnosequences" % "bioinfo-util" % "1.4.2",
	"com.novocode" % "junit-interface" % "0.11" % "test"
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

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) => {
    case PathList("META-INF", "CHANGES.txt") => MergeStrategy.discard
    case PathList("META-INF", "LICENSES.txt") => MergeStrategy.discard
    case x => old(x)
  }
}
