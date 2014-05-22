import AssemblyKeys._

Nice.javaProject

fatArtifactSettings

name := "bg7"

description := "bg7 project"

organization := "ohnosequences"

bucketSuffix := "era7.com"

resolvers += "Era7 maven snapshots" at "http://snapshots.era7.com.s3.amazonaws.com"

libraryDependencies += "ohnosequences" % "bioinfo-util" % "1.4.0-SNAPSHOT"

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

mergeStrategy in assembly ~= { old => {
    case PathList("META-INF", "CHANGES.txt")                     => MergeStrategy.rename
    case PathList("META-INF", "LICENSES.txt")                    => MergeStrategy.rename
    case PathList("org", "apache", "commons", "collections", _*) => MergeStrategy.first
    case x                                                       => old(x)
  }
}

