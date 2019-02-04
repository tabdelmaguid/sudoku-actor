package org.tabdelmaguid.sudokuactor

import java.util.concurrent.TimeUnit

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable
import scala.concurrent.duration.Duration

object Solver {
  val ALL_SYMBOLS: Set[Byte] = (1 to 9).map(_.toByte).toSet

  def props(board: List[Byte]): Props = Props(new Solver(board))
  // messages
  case object Solve
  case class CellUpdate(cellId: Int, value: Set[Byte])
  case object CheckQuiet
}


class Solver(board: List[Byte]) extends Actor with ActorLogging {
  import Cell._
  import Solver._

  val GROUP_EDGE = 3
  val GROUP_SIZE: Int = GROUP_EDGE * GROUP_EDGE
  val BOARD_SIZE: Int = GROUP_SIZE * GROUP_SIZE

  val cells: mutable.MutableList[ActorRef] = mutable.MutableList()
  val groups: mutable.Map[String, mutable.Set[ActorRef]] = mutable.Map()
  val cellsState: mutable.ArraySeq[Set[Byte]] = mutable.ArraySeq.fill(BOARD_SIZE)(ALL_SYMBOLS)
  var lastMessageAt: Long = System.currentTimeMillis()

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
    val firstLine =       "╔═══════╤═══════╤═══════╦═══════╤═══════╤═══════╦═══════╤═══════╤═══════╗\n"
    val lineSeparator = "\n╟───────┼───────┼───────╫───────┼───────┼───────╫───────┼───────┼───────╢\n"
    val laneSeparator = "\n╠═══════╪═══════╪═══════╬═══════╪═══════╪═══════╬═══════╪═══════╪═══════╣\n"
    val lastLine =      "\n╚═══════╧═══════╧═══════╩═══════╧═══════╧═══════╩═══════╧═══════╧═══════╝"

    val printBoard = Array.ofDim[Byte](27, 27)
    cellsState.zipWithIndex.foreach { case (cellOptions, index) =>
      val (cellXAnchor, cellYAnchor) = (col(index) * 3, row(index) * 3)
      if (cellOptions.size == 1) {
        printBoard(cellYAnchor + 1)(cellXAnchor + 1) = cellOptions.head
      } else for (cellValue <- cellOptions) {
        val (x, y) = ((cellValue - 1) % 3, (cellValue - 1) / 3)
        printBoard(cellYAnchor + y)(cellXAnchor + x) = cellValue
      }
    }

    def formatCellLine: Array[Byte] => String = // a 3-value line in one cell
      _.map(value => if (value == 0) " " else value).mkString(" ", " ", " ")
    def formatSection: Array[Byte] => String = // a 3 cell section of one line
      _.grouped(GROUP_EDGE).map(formatCellLine).mkString("│")
    def formatLine: Array[Byte] => String = // a line, representing a third of a cell row
      _.grouped(GROUP_SIZE).map(formatSection).mkString("║", "║", "║")
    def formatRow: Array[Array[Byte]] => String = _.map(formatLine).mkString("\n") // a cell row
    def formatLane: Array[Array[Byte]] => String = // a 3 row lane
      _.grouped(GROUP_EDGE).map(formatRow).mkString(lineSeparator)
    val boardStr = printBoard.grouped(GROUP_SIZE).map(formatLane).mkString(firstLine, laneSeparator, lastLine)

    println(boardStr)
  }

  private val toCancel = context.system.scheduler
    .schedule(Duration(100, TimeUnit.MILLISECONDS), Duration(100, TimeUnit.MILLISECONDS), self, CheckQuiet)(context.dispatcher)

  var requester: ActorRef = _

  def receive: Receive = {
    case Solve =>
      println("hi")
      setupCells()
      printBoard()
      requester = sender()
    case CellUpdate(cellId, values) =>
      cellsState(cellId) = values
      lastMessageAt = System.currentTimeMillis
    case CheckQuiet =>
      println(s"checking: lastAt = $lastMessageAt, now = ${System.currentTimeMillis()}")
      if (System.currentTimeMillis() - lastMessageAt > 100) {
        println("bye ...")
        printBoard()
        toCancel.cancel()
        context.stop(self)
        requester ! "Done!"
      }

  }
}