package org.tabdelmaguid.sudokuactor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable

object Solver {
  val ALL_SYMBOLS: Set[Byte] = (1 to 9).map(_.toByte).toSet

  def props(board: List[Byte]): Props = Props(new Solver(board))
  case object Solve
  case class CellSolved(cellId: Int, value: Byte)
}


class Solver(board: List[Byte]) extends Actor with ActorLogging {
  import Solver._
  import Cell._

  val GROUP_EDGE = 3
  val GROUP_SIZE: Int = GROUP_EDGE * GROUP_EDGE
  val BOARD_SIZE: Int = GROUP_SIZE * GROUP_SIZE

  val cells: mutable.MutableList[ActorRef] = mutable.MutableList()
  val groups: mutable.Map[String, mutable.Set[ActorRef]] = mutable.Map()
  val solvedCells: mutable.ArraySeq[Byte] = mutable.ArraySeq.fill(BOARD_SIZE)(0)
  val cellsState: mutable.ArraySeq[Set[Byte]] = mutable.ArraySeq.fill(BOARD_SIZE)(ALL_SYMBOLS)

  private def row(index: Int) = index / 9
  private def col(index: Int) = index % 9

  def getGroups(index: Int) : Set[String] = {
    val cellRow = row(index)
    val cellCol = col(9)
    val square = cellCol / 3 + 3 * (cellRow / 3)
    Set(s"R$cellRow", s"C$cellCol", s"S$square")
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

  def printBoard(): Unit = {
    val firstLine =     "╔════╤════╤════╦════╤════╤════╦════╤════╤════╗\n"
    val lineSeparator = "╟────┼────┼────╫────┼────┼────╫────┼────┼────╢\n"
    val laneSeparator = "╠════╪════╪════╬════╪════╪════╬════╪════╪════╣\n"
    val lastLine =      "╚════╧════╧════╩════╧════╧════╩════╧════╧════╝"

    val printBoard = Array.ofDim[Byte](27, 27)
    cellsState.zipWithIndex.foreach { case (cellOptions, index) =>
      val cellXAnchor = col(index) * 3
      val cellYAnchor = row(index) * 3
      if (cellOptions.size == 1) {
        printBoard(cellYAnchor + 1)(cellXAnchor + 1) = cellOptions.head
      } else {
        for {
          x <- 0 to 2
          y <- 0 to 2
          cellValue <- List((x + 3 * y + 1).toByte)
          if cellOptions.contains(cellValue)
        } { printBoard(cellYAnchor + y)(cellXAnchor + x) = cellValue }
      }
    }
    printBoard.foreach(row => {
      row.foreach(value => print(value))
      println
    })

//    println(
//      cellsState
//        .grouped(9)
//        .grouped(3)
//        .map(lane => {
//          lane.map(row => {
//            row.grouped(3)
//              .map(section => {
//                section.map(_.formatted("%2d")).mkString(" │ ")
//              }).mkString("║ ", " ║ ", " ║\n")
//          }).mkString(lineSeparator)
//        }).mkString(firstLine, laneSeparator, lastLine)
//    )
  }

  def receive: Receive = {
    case Solve =>
      println("hi")
      setupCells()
      printBoard()
    case CellSolved(cellId, value) =>
      solvedCells(cellId) = value
//      log.info("Solved cells: {}", solvedCells)
  }
}