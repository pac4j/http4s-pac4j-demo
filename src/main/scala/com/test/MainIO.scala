package com.test

import cats.effect._

import scala.concurrent.ExecutionContext.global
import org.http4s.blaze.server.BlazeServerBuilder


object MainIO extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    Blocker[IO].use { b =>
      BlazeServerBuilder[IO](global)
        .bindHttp(8080, "localhost")
        .withMaxHeadersLength(2 * 1024 * 1024)
        .withHttpApp(new TestHttpApp.IOApp(b).routedHttpApp)
        .serve.compile.lastOrError
    }
  }
}
