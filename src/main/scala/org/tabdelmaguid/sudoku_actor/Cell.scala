package org.tabdelmaguid.sudoku_actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable


class CellOptions(var options: Set[Byte])

object Cell {
  def props(id: Int): Props = Props(new Cell(id))
  // messages
  case class AddNeighbors(groupKey: String, neighbors: Set[ActorRef])
  case class SetValue(value: Byte)
  case class MyOptions(values: Set[Byte])
}


class Cell(id: Int) extends Actor with ActorLogging {
  import Cell._
  import Solver._

  var solver: ActorRef = _
  val neighborsOptions: mutable.Map[ActorRef, CellOptions] = mutable.Map()
  val groupOptions: mutable.Map[String, List[CellOptions]] = mutable.Map()
  var myOptions: Set[Byte] = ALL_SYMBOLS

  def calcNewOptions(): Set[Byte] = {
    val usedSymbols = groupOptions.values.flatten.map(_.options).filter(_.size == 1).reduce(_ ++ _)
    myOptions -- usedSymbols
  }

  private def myNeighbors= neighborsOptions.keys

  private def broadcastState(): Unit = {
    solver ! CellUpdate(id, myOptions)
    myNeighbors.foreach(_ ! MyOptions(myOptions))
  }

  private def updateState(newOptions: Set[Byte]): Unit = {
    if (newOptions != myOptions) {
      myOptions = newOptions
      broadcastState()
      if (newOptions.size == 1) context.stop(self)
    }
  }

  override def receive: Receive = {
    case AddNeighbors(groupKey, neighbors) =>
      solver = sender()
      neighbors.foreach { neighbor =>
        val options = new CellOptions(ALL_SYMBOLS)
        neighborsOptions(neighbor) = options
        groupOptions.initOrUpdate(groupKey, List(options), _ :+ options)
      }
    case SetValue(value) =>
      updateState(Set(value))
    case MyOptions(values) =>
      val neighbor = sender()
      neighborsOptions(neighbor).options = values
      updateState(calcNewOptions())
  }

}
