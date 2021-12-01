scalaVersion := "2.13.7"

val catsVersion = "2.6.1"
//val catsEffectVersion = "3.2.9"
val circeVersion = "0.14.1"
val pac4jVersion = "5.1.5"
val http4sVersion = "0.23.6"
//val specs2Version = "3.8.9"

// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "opensaml Repository" at "https://build.shibboleth.net/nexus/content/repositories/releases"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "com.lihaoyi" %% "scalatags" % "0.10.0",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sVersion,
//  "org.http4s" %% "http4s-scalatags" % http4sVersion,
  "org.pac4j" % "pac4j-core" % pac4jVersion,
  "org.pac4j" % "pac4j-cas" % pac4jVersion,
  "org.pac4j" % "pac4j-http" % pac4jVersion,
  "org.pac4j" % "pac4j-jwt" % pac4jVersion,
  "org.pac4j" % "pac4j-oauth" % pac4jVersion,
  "org.pac4j" % "pac4j-oidc" % pac4jVersion,
  "org.pac4j" % "pac4j-saml" % pac4jVersion,
  "org.pac4j" %% "http4s-pac4j" % "4.0.0-SNAPSHOT",
  "org.slf4j" % "slf4j-api" % "1.7.32",
  "org.http4s" %% "http4s-server" % http4sVersion,
  "ch.qos.logback" % "logback-classic" % "1.2.7",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.69",
  "org.bouncycastle" % "bcutil-jdk15on" % "1.69",
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.70",
  "dev.zio" %% "zio" % "1.0.12",
  "dev.zio" %% "zio-interop-cats" % "3.2.9.0"
)

scalacOptions ++= Seq("-language:implicitConversions", "-language:higherKinds", "-deprecation")
