package org.tabdelmaguid.sudokuactor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable


object Cell {
  def props(id: Int): Props = Props(new Cell(id))
  // messages
  case class AddNeighbors(groupKey: String, neighbors: Set[ActorRef])
  case class SetValue(value: Byte)
}


class Cell(id: Int) extends Actor with ActorLogging {
  import Cell._
  import Solver._

  // groupKey -> ( cellId -> Set(value options) )
  val allNeighbors: mutable.Map[String, mutable.Map[ActorRef, Set[Byte]]] = mutable.Map()

  override def receive: Receive = {
    case AddNeighbors(groupKey, neighbors) =>
      val groupMap = allNeighbors.getOrElseUpdate(groupKey, mutable.Map.empty)
      neighbors.foreach(groupMap(_) = ALL_SYMBOLS)
    case SetValue(value) =>
      sender() ! CellSolved(id, value)
      context.stop(self)
  }

}
