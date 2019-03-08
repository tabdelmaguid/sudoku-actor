package org.tabdelmaguid.sudoku_actor

import akka.actor.{ActorRef, ActorSystem}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor
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
  /** -->
    * solution
    * 162 857 493
    * 534 129 678
    * 789 643 521
    * 475 312 986
    * 913 586 742
    * 628 794 135
    * 356 478 219
    * 241 935 867
    * 897 261 354
    */

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
  val board3 = List[Byte](
    3, 0, 9,   0, 0, 7,   0, 6, 0,
    0, 5, 4,   0, 0, 1,   0, 0, 8,
    2, 7, 0,   0, 0, 0,   9, 0, 0,

    0, 0, 0,   0, 1, 0,   0, 4, 6,
    0, 0, 0,   6, 0, 3,   0, 0, 0,
    9, 4, 0,   0, 2, 0,   0, 0, 0,

    0, 0, 5,   0, 0, 0,   0, 8, 4,
    1, 0, 0,   4, 0, 0,   5, 2, 0,
    0, 6, 0,   5, 0, 0,   1, 0, 9
  )
  /** -->
    * Solution
    * 3 8 9  2 5 7  4 6 1
    * 6 5 4  3 9 1  2 7 8
    * 2 7 1  8 6 4  9 5 3
    *
    * 8 3 2  9 1 5  7 4 6
    * 5 1 7  6 4 3  8 9 2
    * 9 4 6  7 2 8  3 1 5
    *
    * 7 2 5  1 3 9  6 8 4
    * 1 9 3  4 8 6  5 2 7
    * 4 6 8  5 7 2  1 3 9
    */

  implicit val timout: Timeout = Timeout(60 seconds)
  implicit val executor: ExecutionContextExecutor = system.dispatcher

  val solver: ActorRef = system.actorOf(Solver.props(board1), "solverActor")

  solver ? Solve andThen {
    case _ => system.terminate()
  }
}
