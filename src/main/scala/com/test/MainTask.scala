package com.test

import cats.effect.{Blocker, ExitCode}
import monix.eval.{Task, TaskApp}
import monix.execution.Scheduler
import org.http4s.blaze.server.BlazeServerBuilder

import scala.concurrent.ExecutionContext.global

object MainTask extends TaskApp {

  implicit val s = Scheduler.global

  override def run(args: List[String]): Task[ExitCode] = {
    Blocker[Task].use { b =>
      BlazeServerBuilder[Task](global)
        .bindHttp(8080, "localhost")
        .withMaxHeadersLength(2 * 1024 * 1024)
        .withHttpApp(new TestHttpApp.TaskApp(b).routedHttpApp)
        .serve.compile.lastOrError
    }
  }
}
