package com.test

import zio._
import zio.interop.catz._

object MainZIO extends ZIOAppDefault {

  def program(implicit runtime: Runtime[Clock]): Task[cats.effect.ExitCode] =
    BlazeServer[Task].run(new (TestHttpApp.ZioApp).routedHttpApp)

  override def run: ZIO[Environment with ZIOAppArgs, Any, Any] = {
    ZIO.runtime[Clock].flatMap { implicit rts =>
      program.foldZIO(
        _ => ZIO.succeed(ExitCode.failure)
        , _ => ZIO.succeed(ExitCode.success)
      )
    }.provideLayer(ZLayer(ZIO.clock))
  }
}
