name := "Property Based Testing"

version := "1.0"

scalaVersion := "2.12.9"

resolvers += Resolver.url("bintray-scala-hedgehog",
    url("https://dl.bintray.com/hedgehogqa/scala-hedgehog")
  )(Resolver.ivyStylePatterns)

val hedgehogVersion = "06b22e95ca1a32a2569914824ffe6fc4cfd62c62"

libraryDependencies ++= List(
  "org.scalacheck" %% "scalacheck" % "1.14.1",

  "hedgehog" %% "hedgehog-core" % hedgehogVersion,
  "hedgehog" %% "hedgehog-runner" % hedgehogVersion,
  "hedgehog" %% "hedgehog-sbt" % hedgehogVersion
)

