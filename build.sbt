scalaVersion := "2.13.18"

val catsVersion = "2.13.0"
//val catsEffectVersion = "3.2.9"
val circeVersion = "0.14.1"
val pac4jVersion = "6.3.1"
val http4sVersion = "0.23.33"
val http4sBlazeVersion = "0.23.17"
//val specs2Version = "3.8.9"

// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "opensaml Repository" at "https://build.shibboleth.net/nexus/content/repositories/releases"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "com.lihaoyi" %% "scalatags" % "0.13.1",
  "org.http4s" %% "http4s-dsl" % http4sVersion,
  "org.http4s" %% "http4s-blaze-server" % http4sBlazeVersion,
  "org.http4s" %% "http4s-blaze-client" % http4sBlazeVersion,
//  "org.http4s" %% "http4s-scalatags" % http4sVersion,
  "org.pac4j" % "pac4j-core" % pac4jVersion,
  "org.pac4j" % "pac4j-cas" % pac4jVersion,
  "org.pac4j" % "pac4j-http" % pac4jVersion,
  "org.pac4j" % "pac4j-jwt" % pac4jVersion,
  "org.pac4j" % "pac4j-oauth" % pac4jVersion,
  "org.pac4j" % "pac4j-oidc" % pac4jVersion,
  "org.pac4j" % "pac4j-saml" % pac4jVersion,
  "org.pac4j" %% "http4s-pac4j" % "5.0.0",
  "org.slf4j" % "slf4j-api" % "2.0.17",
  "org.http4s" %% "http4s-server" % http4sVersion,
  "ch.qos.logback" % "logback-classic" % "1.5.29",
  "org.bouncycastle" % "bcprov-jdk15on" % "1.70",
  "org.bouncycastle" % "bcutil-jdk15on" % "1.69",
  "org.bouncycastle" % "bcpkix-jdk15on" % "1.70",
  "dev.zio" %% "zio" % "2.1.24",
  "dev.zio" %% "zio-interop-cats" % "23.1.0.13"
)

scalacOptions ++= Seq("-language:implicitConversions", "-language:higherKinds", "-deprecation")
