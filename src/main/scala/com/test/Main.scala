package com.test

import org.http4s._
import org.http4s.dsl._

import scala.concurrent.duration._
import org.http4s.server.blaze._
import org.http4s.util.ProcessApp
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.http4s._
import scalaz.concurrent.Task
import scalaz.stream.Process
import scalatags.Text.all._
import org.pac4j.http4s.SessionSyntax._
import scalatags.Text

import scala.collection.JavaConverters._

object Main extends ProcessApp {
  import ScalatagsInstances._

  val sessionConfig = SessionConfig(
    cookieName = "session",
    mkCookie = Cookie(_, _, path = Some("/")),
    secret = "This is a secret",
    maxAge = 5.minutes
  )

  val config = new DemoConfigFactory().build()

  val callbackService = new CallbackService(config)

  val localLogoutService = new LogoutService(config, Some("/?defaulturlafterlogout"), destroySession = true)
  val centralLogoutService = new LogoutService(config,
    defaultUrl = Some("http://localhost:8080/?defaulturlafterlogoutafteridp"),
    destroySession = true,
    logoutUrlPattern = Some("http://localhost:8080/.*"),
    localLogout = false,
    centralLogout = true)

  val root = HttpService {
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

  val loginPages: HttpService = HttpService.lift {
    case req @ GET -> Root / "form" =>
      SecurityFilterMiddleware.securityFilter(config, Some("FormClient")).apply(protectedPages)(req)
    case req @ GET -> Root / "facebook" =>
      SecurityFilterMiddleware.securityFilter(config, Some("FacebookClient")).apply(protectedPages)(req)
    case req @ GET -> Root / "oidc" =>
      SecurityFilterMiddleware.securityFilter(config, Some("OidcClient")).apply(protectedPages)(req)
    case req @ GET -> Root / "saml2" =>
      SecurityFilterMiddleware.securityFilter(config, Some("SAML2Client")).apply(protectedPages)(req)
  }

  val protectedPages = HttpService {
    case req @ GET -> _ =>
      protectedPage(getProfiles(req))
  }

  def protectedPage(profiles: List[CommonProfile]): Task[Response] = {
    Ok(div()(
      h1()("Protected Page"),
      renderProfiles(profiles)
    ))
  }

  def getProfiles(request: Request): List[CommonProfile] = {
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

  override def process(args: List[String]): Process[Task, Nothing] = {
    BlazeBuilder
      .bindHttp(8080, "localhost")
      .mountService(authedProtectedPages, "/protected")
      .mountService(Session.sessionManagement(sessionConfig).apply(loginPages), "/login/")
      .mountService(Session.sessionManagement(sessionConfig).apply(root), "/")
      .serve
  }
}
