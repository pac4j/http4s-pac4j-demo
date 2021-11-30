package com.test

import cats.effect._
import cats.effect.std.Dispatcher
import org.http4s.blaze.server.BlazeServerBuilder

object MainIO extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    Dispatcher[IO].use( d =>
    BlazeServerBuilder[IO]
      .bindHttp(8080, "localhost")
      .withMaxHeadersLength(2 * 1024 * 1024)
      .withHttpApp(new TestHttpApp.App[IO](d).routedHttpApp)
      .serve.compile.lastOrError
    )
  }
}
