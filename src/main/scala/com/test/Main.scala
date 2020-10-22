package com.test

import cats.effect._
import ScalatagsInstances._
import cats.data.{Kleisli, OptionT}
import cats.implicits.toSemigroupKOps
import org.http4s.{HttpRoutes, _}
import org.http4s.dsl.Http4sDsl
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.global
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.http4s.server.blaze._
import org.pac4j.http4s._
import scalatags.Text.all._
import scalatags.Text

import scala.collection.JavaConverters._

object Main extends IOApp {
  protected val dsl: Http4sDsl[IO] = new Http4sDsl[IO]{}
  import dsl._

  private val sessionConfig = SessionConfig(
    cookieName = "session",
    mkCookie = ResponseCookie(_, _, path = Some("/")),
    secret = "This is a secret",
    maxAge = 5.minutes
  )

  private val config = new DemoConfigFactory().build()

  val callbackService = new CallbackService(config)

  val localLogoutService = new LogoutService(config, Some("/?defaulturlafterlogout"), destroySession = true)
  val centralLogoutService = new LogoutService(config,
    defaultUrl = Some("http://localhost:8080/?defaulturlafterlogoutafteridp"),
    destroySession = true,
    logoutUrlPattern = Some("http://localhost:8080/.*"),
    localLogout = false,
    centralLogout = true)

  val root: HttpRoutes[IO] =
    HttpRoutes.of {
      case req @ GET -> Root =>
        Ok(html(
          body(
            h1("index"),
            a(href:="/login/facebook")("Protected url by Facebook: /facebook"), "use a real account", br(),
            a(href:="/login/saml2")("Protected url by SAML2: /saml2"), "use testpac4j at gmail.com / Pac4jtest", br(),
            a(href:="/login/oidc")("Protected url by OpenID Connect: /oidc"), "(use a real account)", br(),
            a(href:="/login/form")("Protected url by form authentication: /form"), "(use username same as password)", br(),
            p(),
            a(href:="/logout")("Local Logout"), br(),
            renderProfiles(getProfiles(req))
          )
        ))
      case GET -> Root / "loginForm" =>
        Ok(
          form(action:="http://localhost:8080/callback?client_name=FormClient", method:="POST")(
            input(`type`:="text", name:="username", value:="")(),
            p(),
            input(`type`:="password", name:="password", value:="")(),
            p(),
            input(`type`:="submit", name:="submit", value:="Submit")()
        ))
      case GET -> Root / "favicon.ico" =>
        NotFound()
      case req @ GET -> Root / "callback" =>
        callbackService.login(req)
      case req @ POST -> Root / "callback" =>
        callbackService.login(req)
      case req @ GET -> Root / "logout" =>
        localLogoutService.logout(req)
      case req @ GET -> Root / "centralLogout" =>
        centralLogoutService.logout(req)
  }

  private val trivial: HttpRoutes[IO] =
    Kleisli(_ => OptionT.liftF(MovedPermanently("Location: /")))

  val loginPages: HttpRoutes[IO] =
    Router(
      "form"     -> SecurityFilterMiddleware.securityFilter(config, Some("FormClient")) { trivial },
      "facebook" -> SecurityFilterMiddleware.securityFilter(config, Some("FacebookClient")) { trivial },
      "oidc"     -> SecurityFilterMiddleware.securityFilter(config, Some("OidcClient")) { trivial },
      "saml2"    -> SecurityFilterMiddleware.securityFilter(config, Some("SAML2Client")) { trivial }
    )

  val protectedPages: HttpRoutes[IO] =
    HttpRoutes.of {
      case req @ GET -> _ =>
        protectedPage(getProfiles(req))
    }

  def protectedPage(profiles: List[CommonProfile]): IO[Response[IO]] = {
    Ok(div()(
      h1()("Protected Page"),
      renderProfiles(profiles)
    ))
  }

  def getProfiles(request: Request[IO]): List[CommonProfile] = {
    val context = Http4sWebContext(request, config)
    val manager = new ProfileManager[CommonProfile](context)
    manager.getAll(true).asScala.toList
  }

  def renderProfiles(profiles: List[CommonProfile]): List[Text.TypedTag[String]] = {
    profiles.map { profile =>
      p()(
        b("Profile: "), profile.toString, br()
      )
    }
  }

  val authedProtectedPages =
    Session.sessionManagement(sessionConfig)
      .compose(SecurityFilterMiddleware.securityFilter(config))
      .apply(protectedPages)

  val routedHttpApp =
    Router(
      "/login" -> Session.sessionManagement(sessionConfig).apply(loginPages),
      "/protected" -> authedProtectedPages,
      "/" -> root
    ).orNotFound


  override def run(args: List[String]): IO[ExitCode] = {
    BlazeServerBuilder[IO](global)
      .bindHttp(8080, "localhost")
      .withHttpApp(routedHttpApp)
      .serve
  }.compile.lastOrError
}
