package com.test

import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

object MainZIO extends App {

  def program: Task[cats.effect.ExitCode] =
    BlazeServer[Task].run(new (TestHttpApp.ZioApp).routedHttpApp)

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    ZIO.runtime[ZEnv].flatMap { implicit rts =>
      program.foldM(
        _ => ZIO.succeed(ExitCode.failure)
        , _ => ZIO.succeed(ExitCode.success)
      )
    }
}
