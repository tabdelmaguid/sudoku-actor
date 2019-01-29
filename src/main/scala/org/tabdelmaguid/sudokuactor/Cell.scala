package org.tabdelmaguid.sudokuactor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import org.tabdelmaguid.sudokuactor.Cell.AddNeighbors

import scala.collection.mutable


object Cell {
  def props(id: Int): Props = Props(new Cell(id))
  // messages
  case class AddNeighbors(groupId: String, neighbors: Set[ActorRef])
}


class Cell(id: Int) extends Actor with ActorLogging {

  val allNeighbors: mutable.Map[String, mutable.Set[ActorRef]] = mutable.Map()

  override def receive: Receive = {
    case AddNeighbors(groupId, neighbors) =>
      val groupNeighbors = allNeighbors.getOrElseUpdate(groupId, mutable.Set.empty)
      neighbors.foreach(groupNeighbors.add)
  }
}
