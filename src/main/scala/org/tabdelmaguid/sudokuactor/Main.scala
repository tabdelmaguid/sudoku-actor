package org.tabdelmaguid.sudokuactor

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.Await
import scala.concurrent.duration._


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

  implicit  val timout: Timeout = Timeout(5 seconds)
  val doneSolving = solver ? Solve
  Await.result(doneSolving, timout.duration)
  system.terminate()

}
