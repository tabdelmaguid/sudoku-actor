package org.tabdelmaguid.sudokuactor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable

object Solver {
  def props(board: List[Byte]): Props = Props(new Solver(board))
  case object Solve
  case class CellSolved(cellId: Int, value: Byte)
}


class Solver(board: List[Byte]) extends Actor with ActorLogging {
  import Solver._
  import Cell._

  private val GROUP_EDGE = 3
  private val GROUP_SIZE = GROUP_EDGE * GROUP_EDGE
  private val BOARD_SIZE = GROUP_SIZE * GROUP_SIZE

  var cells: mutable.MutableList[ActorRef] = mutable.MutableList()
  val groups: mutable.Map[String, mutable.Set[ActorRef]] = mutable.Map()
  val solvedCells: mutable.ArraySeq[Byte] = mutable.ArraySeq.fill(BOARD_SIZE)(0)

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
      val cellActor = context.actorOf(Cell.props(index), s"cell-$index")
      cells += cellActor
      val cellGroups = getGroups(index)
      cellGroups.foreach(group => {
        val cellGroup = groups.getOrElseUpdate(group, mutable.Set.empty)
        cellGroup.add(cellActor)
      })
    })
    groups.foreach { case(groupKey, group) =>
      group.foreach(cell => {
        val otherCells = group - cell
        cell ! AddNeighbors(groupKey, otherCells.toSet)
      })
    }
    cells
      .zip(board)
      .filter { case (_, value) => value != 0 }
      .foreach { case (cell, value) => cell ! SetValue(value) }
  }

  def receive: Receive = {
    case Solve =>
      println("hi")
      setupCells()
    case CellSolved(cellId, value) =>
      solvedCells(cellId) = value
      log.info("Solved cells: {}", solvedCells)
  }
}