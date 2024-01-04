package com.test

import cats.effect._
import cats.syntax.apply._
import cats.syntax.functor._
import com.comcast.ip4s.IpLiteralSyntax
import fs2.concurrent.{Signal, SignallingRef}
import fs2.io.net.Network
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.slf4j.LoggerFactory

object BlazeServer {

  def apply[F[_]: Async: Network]: Apply[F] = new Apply[F]

  class Apply[F[_]: Async: Network] {
    def run(
      app: HttpApp[F],
      exitRef: Ref[F, ExitCode] = Ref.unsafe( ExitCode.Success )
    ): F[ExitCode] = mkStream( app, exitRef ).compile.lastOrError
  }

  private val logger = LoggerFactory.getLogger(this.getClass)

  private def mkStream[F[_]: Async: Network](
    app: HttpApp[F],
    exitRef: Ref[F, ExitCode]
  ): fs2.Stream[F, ExitCode] =
    fs2.Stream.eval( mkSignal[F] ).flatMap( mkStreamWithSignal( app, exitRef, _ ) )

  private def mkStreamWithSignal[F[_]: Network](
    app: HttpApp[F],
    exitRef: Ref[F, ExitCode],
    stopSignal: SignallingRef[F, Boolean]
  )( implicit F: Async[F]): fs2.Stream[F, ExitCode] = {
    val tcpServer  = tcpShutdownServer( stopSignal )
    val httpServer = httpAppServer[F]( app, stopSignal, exitRef )

    fs2.Stream.exec( F.pure(logger.info( "HTTP server starting" )) ) ++
      tcpServer.merge( httpServer ) ++
      fs2.Stream.exec( F.pure( logger.info( "HTTP server stopped" ) ) )
  }

  private def tcpShutdownServer[F[_]: Network](
    signalOut: SignallingRef[F, Boolean]
  )( implicit F: Async[F] ): fs2.Stream[F, Nothing] = {
    def stream( ownSignal: SignallingRef[F, Boolean] ): fs2.Stream[F, Nothing] =
      Network[F]
        .server( address = Some( ipv4"127.0.0.1" ), port = Some( port"8081" ) )
        .evalMap(
          _ =>
            F.pure(logger.info( "HTTP server shutdown requested" ))
              *> signalOut.set( true ) // emit signal to stop
              *> ownSignal.set( true ) // stop the tcp server itself
        )
        .drain
        .interruptWhen( ownSignal )

    fs2.Stream.exec(
      F.pure( logger.info( s"Listening on shutdown port 8081, to stop the server connect to http://localhost:8081" ) )
    ) ++
      fs2.Stream.force( mkSignal[F].map( stream ) )
  }

  private def httpAppServer[F[_]: Async](
    app: HttpApp[F],
    signal: Signal[F, Boolean],
    exitRef: Ref[F, ExitCode]
  ): fs2.Stream[F, ExitCode] =
    BlazeServerBuilder[F]
      .withHttpApp( app )
      .bindHttp(8080, "localhost")
      .withMaxHeadersLength(2 * 1024 * 1024)
      .withoutBanner
      .serveWhile( signal, exitRef )

  private def mkSignal[F[_]: Concurrent]: F[SignallingRef[F, Boolean]] = SignallingRef[F, Boolean]( false )
}
