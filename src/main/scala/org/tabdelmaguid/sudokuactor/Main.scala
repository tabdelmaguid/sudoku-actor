package org.tabdelmaguid.sudokuactor

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem}

import scala.concurrent.ExecutionContextExecutor
import scala.concurrent.duration.FiniteDuration


object Main extends App {
  import Solver._

  val system: ActorSystem = ActorSystem("sudoku-actor")

  val board1 = List[Byte](
    1, 0, 0,   0, 0, 7,   0, 9, 0,
    0, 3, 0,   0, 2, 0,   0, 0, 8,
    0, 0, 9,   6, 0, 0,   5, 0, 0,

    0, 0, 5,   3, 0, 0,   9, 0, 0,
    0, 1, 0,   0, 8, 0,   0, 0, 2,
    6, 0, 0,   0, 0, 4,   0, 0, 0,

    3, 0, 0,   0, 0, 0,   0, 1, 0,
    0, 4, 0,   0, 0, 0,   0, 0, 7,
    0, 0, 7,   0, 0, 0,   3, 0, 0)

  val board2 = List[Byte](
    2, 0, 0,   0, 0, 0,   0, 6, 0,
    0, 0, 0,   0, 7, 5,   0, 3, 0,
    0, 4, 8,   0, 9, 0,   1, 0, 0,

    0, 0, 0,   3, 0, 0,   0, 0, 0,
    3, 0, 0,   0, 1, 0,   0, 0, 9,
    0, 0, 0,   0, 0, 8,   0, 0, 0,

    0, 0, 1,   0, 2, 0,   5, 7, 0,
    0, 8, 0,   7, 3, 0,   0, 0, 0,
    0, 9, 0,   0, 0, 0,   0, 0, 4
  )
  /** -->
    * Solution
    * 273 481 965
    * 916 275 438
    * 548 693 127
    *
    * 859 347 612
    * 367 512 849
    * 124 968 753
    *
    * 431 829 576
    * 685 734 291
    * 792 156 384
    */

  val solver: ActorRef = system.actorOf(Solver.props(board1), "solverActor")

  solver ! Solve

  implicit val executor: ExecutionContextExecutor = system.dispatcher

  system.scheduler.scheduleOnce(FiniteDuration(2, TimeUnit.SECONDS)) {
    println("bye ...")
    system.terminate()
  }

}
