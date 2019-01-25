package org.tabdelmaguid.sudokuactor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

object Solver {
  def props(board: List[Int]): Props = Props(new Solver(board))
  case object Solve
}


class Solver(board: List[Int]) extends Actor with ActorLogging {
  import Solver._

  private val GROUP_EDGE = 3
  private val GROUP_SIZE = GROUP_EDGE * GROUP_EDGE
  private val BOARD_SIZE = GROUP_SIZE * GROUP_SIZE

  var cells: List[ActorRef] = _

  private def setupCells(): Unit = {
    assert(board.length == BOARD_SIZE)
    assert(board.forall((0 to GROUP_SIZE).contains(_)))

    cells = (0 until BOARD_SIZE).toList.map(index =>
      context.actorOf(Cell.props(index), s"cell-$index")
    )
  }

  def receive: Receive = {
    case Solve =>
      println("hi")
      setupCells()
    //      context.stop(self)
  }
}