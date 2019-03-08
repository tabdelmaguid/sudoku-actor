package org.tabdelmaguid.sudoku_actor

import java.util.concurrent.atomic.AtomicLong

import akka.actor.ActorRef

import scala.collection.mutable

object WithId {
  private val counter = new AtomicLong(0)
  def nextId(): Long = counter.incrementAndGet()
}

trait WithId { val id: Long = WithId.nextId() }

case class Ack(messageId: Long)

object Ack {
  def apply(message: WithId): Ack = Ack(message.id)
}

trait MessageTracking {

  private type MsgSource = (Long, ActorRef)
  private val msgSourceToDerived: mutable.Map[MsgSource, mutable.Set[Long]] = mutable.Map()
  private val msgToSource: mutable.Map[Long, Option[MsgSource]] = mutable.Map()
  private var whenAllMessagesAcked: List[() => Unit] = List()


  def sender(): ActorRef

  def track[T <: WithId](message: T): T = {
    msgToSource(message.id) = None
    message
  }

  def ack(sender: ActorRef, msgId: Long): Unit = {
    sender ! Ack(msgId)
  }
  def ack(sender: ActorRef, msg: WithId): Unit = ack(sender, msg.id)
  def ack(msg: WithId): Unit = ack(sender(), msg.id)

  def msgAcked(messageId: Long): Unit = {
    if (msgToSource.get(messageId).isEmpty) {
    } else {
      msgToSource(messageId) match {
        case Some(source) =>
          val messages = msgSourceToDerived(source) -= messageId
          if (messages.isEmpty) {
            msgSourceToDerived -= source
            val (sourceMessageId, sender) = source
            ack(sender, sourceMessageId)
          }
        case None =>
      }

      msgToSource -= messageId
      if (msgToSource.isEmpty && whenAllMessagesAcked.nonEmpty) {
        val fun = whenAllMessagesAcked.head
        whenAllMessagesAcked = whenAllMessagesAcked.tail
        fun()
      }
    }
  }

  def onAllMessagesAcked(fun: () => Unit): Unit = whenAllMessagesAcked = whenAllMessagesAcked :+ fun

  def trackForSenderAck[T <: WithId](sourceMessageId: Long, message: T): T = {
    val messageSource = (sourceMessageId, sender())
    msgSourceToDerived.initOrUpdate(messageSource, mutable.Set(message.id), _ + message.id)
    msgToSource(message.id) = Some(messageSource)
    message
  }

}
