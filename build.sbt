name := "Property Based Testing"

version := "1.0"

scalaVersion := "2.12.8"

resolvers += Resolver.url("bintray-scala-hedgehog",
    url("https://dl.bintray.com/hedgehogqa/scala-hedgehog")
  )(Resolver.ivyStylePatterns)

val hedgehogVersion = "d74f5bb31f26d3e3b7f7d0198b6e768a1ed20669"

libraryDependencies ++= List(
  "org.scalacheck" %% "scalacheck" % "1.14.0",

  "hedgehog" %% "hedgehog-core" % hedgehogVersion,
  "hedgehog" %% "hedgehog-runner" % hedgehogVersion,
  "hedgehog" %% "hedgehog-sbt" % hedgehogVersion,

  "org.typelevel" %% "discipline" % "0.11.0"
)

