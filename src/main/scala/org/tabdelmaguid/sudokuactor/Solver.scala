package org.tabdelmaguid.sudokuactor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable

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
  val groups: mutable.Map[String, mutable.Set[ActorRef]] = mutable.Map()

  def getGroups(index: Int) : Set[String] = {
    val rowGroup = index / 9
    val colGroup = index % 9
    val square = colGroup / 3 + 3 * (rowGroup / 3)
    Set(s"R$rowGroup", s"C$colGroup", s"S$square")
  }

  private def setupCells(): Unit = {
    assert(board.length == BOARD_SIZE)
    assert(board.forall((0 to GROUP_SIZE).contains(_)))

    (0 until BOARD_SIZE).foreach(index => {
      val cellGroups = getGroups(index)
      val cellActor = context.actorOf(Cell.props(index), s"cell-$index")
      cellGroups.foreach(group => {
        val cellGroup = groups.getOrElseUpdate(group, mutable.Set.empty)
        cellGroup.add(cellActor)
      })
    })
  }

  def receive: Receive = {
    case Solve =>
      println("hi")
      setupCells()
    //      context.stop(self)
  }
}