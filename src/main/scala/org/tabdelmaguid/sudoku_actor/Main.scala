package org.tabdelmaguid.sudoku_actor

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration._


object Main extends App {
  import Solver._
  import TestBoards._

  val system: ActorSystem = ActorSystem("sudoku-actor")


  implicit val timout: Timeout = Timeout(60 seconds)
  implicit val executor: ExecutionContextExecutor = system.dispatcher

  val solver: ActorRef = system.actorOf(Solver.props(test1.board), "solverActor")

  solver ? Solve andThen {
    case _ => system.terminate()
  }
}
