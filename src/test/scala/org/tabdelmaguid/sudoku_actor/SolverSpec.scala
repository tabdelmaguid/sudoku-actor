package org.tabdelmaguid.sudoku_actor

import akka.actor.ActorSystem
import akka.testkit.TestKit
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}

class SolverSpec(_system: ActorSystem) extends TestKit(_system)
  with WordSpecLike
  with BeforeAndAfterAll {

  def this() = this(ActorSystem("SudokuActorSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "Solver" should {
    "solve test board 1" in {

    }
  }

}
