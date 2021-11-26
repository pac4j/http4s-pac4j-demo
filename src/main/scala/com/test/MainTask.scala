package com.test

//import cats.effect.ExitCode
//import monix.eval.{Task, TaskApp}
//import monix.execution.Scheduler
//import org.http4s.blaze.server.BlazeServerBuilder

object MainTask /* extends TaskApp */ {

/*  implicit val s = Scheduler.global

  override def run(args: List[String]): Task[ExitCode] = {
      BlazeServerBuilder[Task]
        .bindHttp(8080, "localhost")
        .withMaxHeadersLength(2 * 1024 * 1024)
        .withHttpApp(new TestHttpApp.TaskApp(b).routedHttpApp)
        .serve.compile.lastOrError
  }

 */
}
