package com.test

import cats.effect.Blocker
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._
import zio.blocking.Blocking
import zio.clock.Clock
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext.global

object MainZIO extends App {

  def program(implicit runtime: zio.Runtime[Clock & Blocking]): Task[ExitCode] =
      Blocker[Task].use { b =>
        BlazeServerBuilder[Task](global)
          .bindHttp(8080, "localhost")
          .withMaxHeadersLength(2 * 1024 * 1024)
          .withHttpApp(new TestHttpApp.ZIOApp(b).routedHttpApp)
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
