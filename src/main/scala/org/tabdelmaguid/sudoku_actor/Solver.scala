package org.tabdelmaguid.sudoku_actor

import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props}

import scala.collection.mutable

class MutableStack[T] {
  private var stack: mutable.MutableList[T] = mutable.MutableList.empty

  def push(value: T): Unit = value +=: stack
  def pop(): T = {
    val value = stack.head
    stack = stack.tail
    value
  }
  def peek: T = stack.head
  def size: Int = stack.size
  def isEmpty: Boolean = stack.isEmpty
  def nonEmpty: Boolean = stack.nonEmpty
}


object Solver {
  val ALL_SYMBOLS: Set[Byte] = (1 to 9).map(_.toByte).toSet

  def props(board: List[Byte]): Props = Props(new Solver(board))
  // messages
  case object Solve
  case class CellUpdate(cellId: Int, value: Set[Byte]) extends WithId
  case class Unsolvable() extends WithId
  case class Solved(solution: List[Byte])
}


class Solver(board: List[Byte]) extends Actor with ActorLogging with MessageTracking {
  import Cell._
  import Solver._

  val GROUP_EDGE = 3
  val GROUP_SIZE: Int = GROUP_EDGE * GROUP_EDGE
  val BOARD_SIZE: Int = GROUP_SIZE * GROUP_SIZE

  class SolutionStep(cellsOptions: Seq[Set[Byte]],
                     baseState: Seq[Set[Byte]],
                     cellToGuess: Int = -1,
                     val symbolsToTry: Set[Byte] = Set(),
                     val cellsState: mutable.ArraySeq[Set[Byte]] = mutable.ArraySeq.fill(BOARD_SIZE)(ALL_SYMBOLS)) {
    private var solvable = true
    def isUnsolvable: Boolean = !solvable
    def markUnsolvable(): Unit = solvable = false
    private val cells = setupCells(cellsOptions)

    def nextStep(initState: Seq[Set[Byte]], cellToGuess: Int, symbolsToTry: Set[Byte]): SolutionStep = {
      val newState = initState.updated(cellToGuess, Set(symbolsToTry.head))
      new SolutionStep(newState, initState, cellToGuess, symbolsToTry.tail)
    }
    def nextStep(): SolutionStep = {
      val nonSolvedCellsWithIndex = cellsState.zipWithIndex.filter(_._1.size != 1)
      val (minCellOptions, index) = findCellWithMinOptions(nonSolvedCellsWithIndex.toList)
      nextStep(cellsState, index, minCellOptions)
    }
    def nextOptionStep(): SolutionStep = nextStep(baseState, cellToGuess, symbolsToTry)
    def hasOptionsToTry: Boolean = symbolsToTry.nonEmpty
    def stopAll(): Unit = cells.foreach( _ ! PoisonPill )
    def solutionReached: Boolean = cellsState.forall(_.size == 1)
  }

  var solutionSteps = new MutableStack[SolutionStep]
  var stepCounter = 0

  private def row(index: Int) = index / 9
  private def col(index: Int) = index % 9

  def getGroups(index: Int) : Set[String] = {
    val cellRow = row(index)
    val cellCol = col(index)
    val square = cellCol / 3 + 3 * (cellRow / 3)
    Set(s"R$cellRow", s"C$cellCol", s"S$square")
  }

  private def createCellsAndSetGroups(): Seq[ActorRef] = {

    val cells: mutable.MutableList[ActorRef] = mutable.MutableList()
    val groups: mutable.Map[String, Set[ActorRef]] = mutable.Map()

    (0 until BOARD_SIZE).foreach(index => {
      val cell = context.actorOf(Cell.props(index), s"cell_$stepCounter-$index")
      cells += cell
      val cellGroups = getGroups(index)
      cellGroups.foreach(groupKey => {
        groups.initOrUpdate(groupKey, Set(cell), _ + cell)
      })
    })
    groups.foreach { case(groupKey, group) =>
      group.foreach(cell => {
        val otherCells = group - cell
        cell ! track(AddNeighbors(groupKey, otherCells))
      })
    }

    stepCounter += 1
    cells
  }

  private def setCellsOptions(cells: Seq[ActorRef], cellsOptions: Seq[Set[Byte]]): Unit =
    cells
      .zip(cellsOptions)
      .foreach { case (cell, options) => cell ! track(SetOptions(options)) }

  private def setupCells(cellsOptions: Seq[Set[Byte]]): Seq[ActorRef] = {
    val cells = createCellsAndSetGroups()
    onAllMessagesAcked(() => setCellsOptions(cells, cellsOptions))
    onAllMessagesAcked(assessSolutionState)
    cells
  }

  def printBoard(cellsState: Seq[Set[Byte]]): Unit = {
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

  var requester: ActorRef = _

  def terminate(message: Any): Unit = {
    println(s"bye: $message ...")
    requester ! message
    context.stop(self)
  }

  private def findCellWithMinOptions(minSoFar: (Set[Byte], Int), rest: Seq[(Set[Byte], Int)]): (Set[Byte], Int) =
    rest match {
      case Seq() => minSoFar
      case _ if minSoFar._1.size == 2 => minSoFar
      case head :: tail => findCellWithMinOptions(List(head, minSoFar).minBy(_._1.size), tail)
    }

  private def findCellWithMinOptions(list: Seq[(Set[Byte], Int)]): (Set[Byte], Int) = findCellWithMinOptions(list.head, list.tail)

  private def assessSolutionState(): Unit = {
    solutionSteps.peek.stopAll()
    if (solutionSteps.peek.isUnsolvable) {
      while (solutionSteps.nonEmpty && !solutionSteps.peek.hasOptionsToTry) solutionSteps.pop()
      if (solutionSteps.isEmpty) terminate(Unsolvable)
      else solutionSteps.push(solutionSteps.pop().nextOptionStep())
    } else if (solutionSteps.peek.solutionReached) {
      printBoard(solutionSteps.peek.cellsState)
      terminate(Solved(solutionSteps.peek.cellsState.map(_.head).toList))
    } else {
      val stepFun = if (solutionSteps.peek.hasOptionsToTry) () => solutionSteps.peek else () => solutionSteps.pop()
      solutionSteps.push(stepFun().nextStep())
    }
  }

  def receive: Receive = {
    case Solve =>
      println("hi")
      assert(board.length == BOARD_SIZE)
      val boardSymbols = ALL_SYMBOLS + 0
      assert(board.forall(boardSymbols.contains))
      val cellsOptions = board.map { symbol => if (symbol == 0) ALL_SYMBOLS else Set(symbol) }
      solutionSteps.push(new SolutionStep(cellsOptions , cellsOptions))
      requester = sender()
    case msg @ CellUpdate(cellId, values) =>
      ack(msg)
      val currentStep = solutionSteps.peek
      currentStep.cellsState(cellId) = values
    case msg: Unsolvable =>
      ack(msg)
      solutionSteps.peek.markUnsolvable()
    case Ack(messageId) => msgAcked(messageId)
  }

}