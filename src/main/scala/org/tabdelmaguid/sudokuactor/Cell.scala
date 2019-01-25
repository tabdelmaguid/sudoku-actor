package org.tabdelmaguid.sudokuactor

import akka.actor.{Actor, ActorLogging, Props}


object Cell {
  def props(id: Int): Props = Props(new Cell(id))
}


class Cell(id: Int) extends Actor with ActorLogging {

  override def receive: Receive = Actor.emptyBehavior
}
