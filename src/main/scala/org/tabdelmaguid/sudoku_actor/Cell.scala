package org.tabdelmaguid.sudoku_actor

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.collection.mutable


class CellOptions(var options: Set[Byte])

object Cell {
  def props(id: Int): Props = Props(new Cell(id))
  // messages
  case class AddNeighbors(groupKey: String, neighbors: Set[ActorRef]) extends WithId
  case class SetOptions(options: Set[Byte]) extends WithId
  case class MyOptions(values: Set[Byte]) extends WithId
}


class Cell(id: Int) extends Actor with ActorLogging with MessageTracking {
  import Cell._
  import Solver._

  var solver: ActorRef = _
  val neighborsOptions: mutable.Map[ActorRef, CellOptions] = mutable.Map()
  val groupOptions: mutable.Map[String, List[CellOptions]] = mutable.Map()
  var myOptions: Set[Byte] = ALL_SYMBOLS

  /** iterate over a list, applying a function. If the result satisfies a condition, return the result
      if not, keep going. If non match, return None */
  def iterateUntil[E, T](list: List[E], cond: T => Boolean, fun: E => T): Option[T] = list match {
      case Nil => None
      case head :: tail =>
        val calculation = fun(head)
        if (cond(calculation)) Some(calculation) else iterateUntil(tail, cond, fun)
  }


  def calcNewOptions(): Set[Byte] = {
    val singleOptions = groupOptions.values.flatten.map(_.options).filter(_.size == 1)
    val usedSymbols = singleOptions match {
      case List() => Set()
      case _ => singleOptions.reduce(_ ++ _)
    }
    val unusedSymbols = myOptions -- usedSymbols

    if (unusedSymbols.isEmpty) Set()
    else {
      val noShows = iterateUntil[List[CellOptions], Set[Byte]](groupOptions.values.toList, _.size == 1, { group =>
        group.foldLeft(unusedSymbols) { (acc, cell) => acc -- cell.options }
      })

      if (noShows.isEmpty || noShows.get.size > 1) unusedSymbols
      else noShows.get
    }
  }

  private def myNeighbors= neighborsOptions.keys

  private def tellAndTrack(sourceMessageId: Long, to: ActorRef, message: WithId): Unit = {
    to ! trackForSenderAck(sourceMessageId, message)
  }

  private def broadcastState(sourceMessageId: Long): Unit = {
    tellAndTrack(sourceMessageId, solver, CellUpdate(id, myOptions))
    myNeighbors.foreach(tellAndTrack(sourceMessageId, _, MyOptions(myOptions)))
  }

  private def updateState(sourceMessageId: Long, newOptions: Set[Byte]): Unit =
    if (newOptions.isEmpty) {
      tellAndTrack(sourceMessageId, solver, Unsolvable())
    } else if (newOptions != myOptions) {
      myOptions = newOptions
      broadcastState(sourceMessageId)
    } else {
      ack(sender(), sourceMessageId)
    }

  override def receive: Receive = {
    case msg @ AddNeighbors(groupKey, neighbors) =>
      solver = sender()
      neighbors.foreach { neighbor =>
        val options = new CellOptions(ALL_SYMBOLS)
        neighborsOptions(neighbor) = options
        groupOptions.initOrUpdate(groupKey, List(options), _ :+ options)
      }
      ack(solver, msg)
    case msg: SetOptions =>
      updateState(msg.id, msg.options)
    case msg @ MyOptions(values) =>
      val neighbor = sender()
      neighborsOptions(neighbor).options = values
      updateState(msg.id, calcNewOptions())
    case Ack(messageId) => msgAcked(messageId)
  }

  override def toString: String = s"Cell#$id"

}
