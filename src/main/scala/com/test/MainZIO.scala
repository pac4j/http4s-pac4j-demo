package com.test

import cats.effect.Async
import cats.effect.std.Dispatcher
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.blocking.Blocking
import zio.clock.Clock
import org.http4s.blaze.server.BlazeServerBuilder

object MainZIO extends App {

  def program(implicit runtime: zio.Runtime[Clock & Blocking], async: Async[Task]): Task[ExitCode] =
      Dispatcher[Task].use { d =>
        BlazeServerBuilder[Task]
          .bindHttp(8080, "localhost")
          .withMaxHeadersLength(2 * 1024 * 1024)
          .withHttpApp(new TestHttpApp.App[Task](d).routedHttpApp)
          .serve.compile.lastOrError
      }.map(e => ExitCode(e.code))

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    ZIO.runtime[ZEnv].flatMap { implicit rts =>
      program.foldM(
        _ => ZIO.succeed(ExitCode.failure)
        , _ => ZIO.succeed(ExitCode.success)
      )
    }
}
