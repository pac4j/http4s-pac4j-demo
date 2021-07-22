scalaVersion := "2.13.6" // Also supports 2.11.x

val catsVersion = "2.6.1"
val catsEffectVersion = "2.1.3"
val circeVersion = "0.9.3"
val pac4jVersion = "5.1.2"
val http4sVersion = "0.22-129-24d065b"
val specs2Version = "3.8.9"

// Only necessary for SNAPSHOT releases
resolvers += Resolver.sonatypeRepo("snapshots")
resolvers += "opensaml Repository" at "https://build.shibboleth.net/nexus/content/repositories/releases"

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % catsVersion,
  "com.lihaoyi" %% "scalatags" % "0.9.4",
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
  "org.pac4j" % "pac4j-openid" % pac4jVersion,
  "org.pac4j" % "pac4j-saml" % pac4jVersion,
  "org.pac4j" %% "http4s-pac4j" % "2.0.1",
  "org.slf4j" % "slf4j-api" % "1.7.32",
  "org.http4s" %% "http4s-server" % http4sVersion

)

scalacOptions ++= Seq("-Ypartial-unification", "-language:implicitConversions", "-language:higherKinds")
