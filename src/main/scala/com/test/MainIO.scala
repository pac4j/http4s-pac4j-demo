package com.test

import cats.effect._
import cats.effect.std.Dispatcher

object MainIO extends IOApp {

  override def run(args: List[String]): IO[ExitCode] = {
    Dispatcher[IO].use( d =>
      BlazeServer[IO].run(new TestHttpApp.App[IO](d).routedHttpApp)
    )
  }
}
