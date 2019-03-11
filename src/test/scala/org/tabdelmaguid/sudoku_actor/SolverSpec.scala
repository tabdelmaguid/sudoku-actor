package org.tabdelmaguid.sudoku_actor

import akka.actor.{ActorRef, ActorSystem}
import akka.testkit.{TestKit, TestProbe}
import org.scalatest.{BeforeAndAfterAll, WordSpecLike}
import org.tabdelmaguid.sudoku_actor.TestBoards._
import scala.concurrent.duration._


class SolverSpec(_system: ActorSystem) extends TestKit(_system)
  with WordSpecLike
  with BeforeAndAfterAll {

  import Solver._

  def this() = this(ActorSystem("SudokuActorSpec"))

  override def afterAll: Unit = {
    shutdown(system)
  }

  "Solver" should {
    "solve test board 1" in {
      val testProbe = TestProbe()
      val solver: ActorRef = system.actorOf(Solver.props(test1.board), "solverActor")
      solver.tell(Solve, testProbe.ref)
      testProbe.expectMsg(30 seconds, Solved(test1.solution))
    }
  }

}
