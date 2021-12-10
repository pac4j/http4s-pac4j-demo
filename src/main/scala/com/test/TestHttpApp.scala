package com.test

import cats.effect._
import ScalatagsInstances._
import cats.data.{Kleisli, OptionT}
import cats.effect.std.Dispatcher
import org.http4s._
import org.http4s.dsl.Http4sDsl
import org.http4s.headers.Location
import org.http4s.implicits.http4sKleisliResponseSyntaxOptionT
import org.http4s.server.Router
import org.http4s.syntax.all._
import org.pac4j.core.config.Config
import zio.interop.catz._

import scala.concurrent.duration._
import org.pac4j.core.profile.{CommonProfile, ProfileManager}
import org.pac4j.http4s.{Http4sWebContext, _}
import scalatags.Text.all._
import scalatags.Text
import zio._
import zio.blocking.Blocking
import zio.clock.Clock

import scala.jdk.CollectionConverters._

class TestHttpApp[F[_] <: AnyRef : Sync](contextBuilder: (Request[F], Config) => Http4sWebContext[F]) {
  protected val dsl: Http4sDsl[F] = new Http4sDsl[F]{}
  import dsl._

  private val sessionConfig = SessionConfig(
    cookieName = "session",
    mkCookie = ResponseCookie(_, _, path = Some("/")),
    secret = "This is a secret",
    maxAge = 5.minutes
  )

  private val config = new DemoConfigFactory[F]().build()

  val callbackService = new CallbackService[F](config, contextBuilder)

  val localLogoutService = new LogoutService[F](config, contextBuilder, Some("/?defaulturlafterlogout"), destroySession = true)
  val centralLogoutService = new LogoutService[F](config,
    contextBuilder,
    defaultUrl = Some("http://localhost:8080/?defaulturlafterlogoutafteridp"),
    logoutUrlPattern = Some("http://localhost:8080/.*"),
    localLogout = false,
    destroySession = true,
    centralLogout = true)

  val root: HttpRoutes[F] =
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
        callbackService.callback(req)
      case req @ POST -> Root / "callback" =>
        callbackService.callback(req)
      case req @ GET -> Root / "logout" =>
        localLogoutService.logout(req)
      case req @ GET -> Root / "centralLogout" =>
        centralLogoutService.logout(req)
    }

  private val authedTrivial: AuthedRoutes[List[CommonProfile], F] =
    Kleisli(_ => OptionT.liftF(Found(Location(uri"/"))))

  val loginPages: HttpRoutes[F] =
    Router(
      "form"     -> Session.sessionManagement[F](sessionConfig)
        .compose(SecurityFilterMiddleware.securityFilter[F](config, contextBuilder, Some("FormClient"))).apply(authedTrivial),
      "facebook" -> Session.sessionManagement[F](sessionConfig)
        .compose(SecurityFilterMiddleware.securityFilter[F](config, contextBuilder, Some("FacebookClient"))).apply(authedTrivial),
      "oidc"     -> Session.sessionManagement[F](sessionConfig)
        .compose(SecurityFilterMiddleware.securityFilter[F](config, contextBuilder, Some("OidcClient"))).apply(authedTrivial),
      "saml2"    -> Session.sessionManagement[F](sessionConfig)
        .compose(SecurityFilterMiddleware.securityFilter[F](config, contextBuilder, Some("SAML2Client"))).apply(authedTrivial)
    )

  val authProtectedPages: AuthedRoutes[List[CommonProfile], F] =
    AuthedRoutes.of {
      case ar @ GET -> _ as profiles =>
        //          protectedPage(getProfiles(ar.req))
        protectedPage(profiles)
    }

  def protectedPage(profiles: List[CommonProfile]): F[Response[F]] = {
    Ok(div()(
      h1()("Protected Page"),
      renderProfiles(profiles)
    ))
  }

  def getProfiles(request: Request[F]): List[CommonProfile] = {
    val context = contextBuilder(request, config)
    val manager = new ProfileManager(context, config.getSessionStore)
    manager.getProfiles.asScala.map(_.asInstanceOf[CommonProfile]).toList
  }

  def renderProfiles(profiles: List[CommonProfile]): List[Text.TypedTag[String]] = {
    profiles.map { profile =>
      p()(
        b("Profile: "), profile.toString, br()
      )
    }
  }
  val authedProtectedPages: HttpRoutes[F] =
    Session.sessionManagement[F](sessionConfig)
      .compose(SecurityFilterMiddleware.securityFilter[F](config, contextBuilder))
      { authProtectedPages }

  val routedHttpApp =
    Router(
      "/login" -> (Session.sessionManagement[F](sessionConfig) _){ loginPages },
      "/protected" -> authedProtectedPages,
      "/" -> (Session.sessionManagement[F](sessionConfig) _){ root }
    ).orNotFound
}

object TestHttpApp {
  class App[F[_] <: AnyRef : Sync]( dispatcher: Dispatcher[F] )
    extends TestHttpApp[F](Http4sWebContext.withDispatcherInstance(dispatcher))

  class ZioApp( implicit runtime: zio.Runtime[Clock & Blocking] ) extends TestHttpApp[Task](
    ( req, conf ) => new Http4sWebContext[Task]( req, conf.getSessionStore, runtime.unsafeRun( _ ) )
  )
}
