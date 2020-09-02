// DO NOT MODIFY FOR BASIC SUBMISSION
// scalastyle:off

package generic

import java.io.{File, PrintWriter}

import engine.random.RandomGenerator
import org.scalatest.{BeforeAndAfterAll, FunSuiteLike, Matchers}
import generic.GenericRecord._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._
import scala.language.postfixOps
import generic.StringUtils._
import org.scalatest.concurrent.{Signaler, TimeLimitedTests}
import org.scalatest.time.{Seconds, Span}

/**   * Generic test infrastructure for Snake and Tetris.
  *
  * This supports a game with a grid, filled with some CellType
  * on which some actions can be done.
  *
  * An initial game can be constructed out of some InitialInfo
  * For testing we perform the actions and check is the grid
  * is the same as specified in the test.
  */
trait CellTypeInterface[T] {
  def conforms(rhs: T): Boolean

  def toChar: Char
}

trait GameLogicInterface[GameAction, CellType] {
  def nrRows: Int

  def nrColumns: Int

  def performAction(action: GameAction): Unit

  def getCell(col: Int, row: Int): CellType

  def isGameOver: Boolean
}

abstract class GenericRecord[
            GameAction,
            CellType <: CellTypeInterface[CellType],
            GameLogic <: GameLogicInterface[GameAction, CellType],
            InitialInfo]() {

  def charToGridType(ch: Char): CellType

  def makeGame(r: RandomGenerator, info: InitialInfo): GameLogic

  def gameLogicName: String

  sealed abstract class GameDisplay {
    def conforms(other: GameDisplay): Boolean
    def isError : Boolean
  }

  case class GridDisplay(grid: Seq[Seq[CellType]])
    extends GameDisplay {

    val nrRows: Int = grid.length
    val nrColumns: Int = grid.head.length

    override def toString: String =
      grid.map(_.map(_.toChar).mkString).mkString("\n")

    def gridConforms(otherGrid: Seq[Seq[CellType]]): Boolean = {

      val zippedCells: Seq[(CellType, CellType)] =
        for ((rowL, rowR) <- grid zip otherGrid; p <- rowL zip rowR) yield p

      val sameContent: Boolean =
        zippedCells.forall(pair => pair._1.conforms(pair._2))

      sameContent
    }

    def sameDimensions(rhs: GridDisplay): Boolean =
      nrRows == rhs.nrRows && nrColumns == rhs.nrColumns

    override def conforms(other: GameDisplay): Boolean =
      other match {
        case g: GridDisplay => sameDimensions(g) && gridConforms(g.grid)
        case _ => false
      }

    override def isError: Boolean = false
  }

  case class GameOverDisplay() extends GameDisplay {
    override def conforms(other: GameDisplay): Boolean = other match {
      case GameOverDisplay() => true
      case _ => false
    }
    override def isError: Boolean = false
  }

  case class LogicFailed(cause: Throwable) extends GameDisplay {
    override def conforms(other: GameDisplay): Boolean = other match {
      case LogicFailed(_) => true
      case _ => false
    }

    override def isError: Boolean = true

    override def toString: String =
      "LogicFailed(" + stackTraceAsString(cause) + ")"
  }

  case class FrameInput(randomNumber: Int, actions: Seq[GameAction])

  case class TestFrame(input: FrameInput, display: GameDisplay) {

    override def toString: String = {
      val actionsString =
        if (input.actions.isEmpty) ""
        else ", " + input.actions.toString
      "TestFrame(" + input.randomNumber + actionsString + ",\n" +
        asIndentendMultilineString(display.toString, 6)
    }

  }

  def stringToGridDisplay(s: String): GridDisplay = {
    GridDisplay(
      for (row <- s.stripMargin.linesIterator.toList)
        yield for (char <- row)
          yield charToGridType(char)
    )
  }

  def getDisplay(logic: GameLogic): GameDisplay = {
    def getGridDisplay(logic: GameLogic): GridDisplay = {
      GridDisplay(
        for (row <- 0 until logic.nrRows)
          yield for (col <- 0 until logic.nrColumns)
            yield logic.getCell(col, row)
      )
    }

    if (logic.isGameOver) GameOverDisplay()
    else getGridDisplay(logic)
  }


  def performActionsAndGetDisplay(random: TestRandomGen,
                                  logic: GameLogic,
                                  frameInput: FrameInput): GameDisplay = {
    random.curNumber = frameInput.randomNumber
    frameInput.actions.foreach(logic.performAction)
    getDisplay(logic)
  }

  def checkInterleave(testA: Test, testB: Test): Boolean = {
    val randomA = new TestRandomGen(testA.frames.head.input.randomNumber)
    val randomB = new TestRandomGen(testB.frames.head.input.randomNumber)

    val logicA = makeGame(randomA, testA.initialInfo)
    val logicB = makeGame(randomB, testB.initialInfo)
    val dispA = getDisplay(logicA)
    val dispB = getDisplay(logicB)
    if(!dispA.conforms(testA.frames.head.display) ||
       !dispB.conforms(testB.frames.head.display))
      return false

    for ((a, b) <- testA.frames.tail.zip(testB.frames.tail)) {
      val (da, db) = (performActionsAndGetDisplay(randomA, logicA, a.input),
        performActionsAndGetDisplay(randomB, logicB, b.input))
      val (successA, successB) = (da.conforms(a.display), db.conforms(b.display))
      if (!successA || !successB) return false
    }
    return true
  }

  case class Test(name: String, initialInfo: InitialInfo, frames: Seq[TestFrame]) {
    lazy val referenceDisplays: Seq[GameDisplay] = frames.map(_.display)

    lazy val implementationDisplays: Seq[GameDisplay] = {
        val random = new TestRandomGen(frames.head.input.randomNumber)
        def catchLogicError(compute : => GameDisplay) : GameDisplay = {
          try {
            return compute
          } catch {
            case e : Throwable => LogicFailed(e)
          }
        }
        try {
          val logic = makeGame(random, initialInfo)
          val displays = Seq.newBuilder[GameDisplay]
          val firstDisplay = catchLogicError{ getDisplay(logic) }
          displays.addOne(firstDisplay)
          var error = firstDisplay.isError
          val inputIterator = frames.tail.iterator
          while(!error && inputIterator.hasNext ) {
            val testFrame = inputIterator.next()
            val newDisplay = catchLogicError {performActionsAndGetDisplay(random, logic, testFrame.input) }
            displays.addOne(newDisplay)
            error = newDisplay.isError
          }
          displays.result()
        } catch {
          case e: Throwable => Seq(LogicFailed(e))
        }
    }

    lazy val passes: Boolean =
      checkSameRecording(referenceDisplays, implementationDisplays)
  }


  object TestFrame {

    def apply(rand: Int,
              actions: Seq[GameAction],
              grid: String): TestFrame = {
      TestFrame(FrameInput(rand, actions), stringToGridDisplay(grid))
    }

    def apply(rand: Int, grid: String): TestFrame = {
      apply(rand, List(), grid)
    }

    def apply(rand: Int,
              actions: Seq[GameAction],
              display: GameDisplay): TestFrame = {
      TestFrame(FrameInput(rand, actions), display)
    }

    def apply(rand: Int, display: GameDisplay): TestFrame = {
      TestFrame(FrameInput(rand, List()), display)
    }

  }



  def checkSameRecording(testRecord: Seq[GameDisplay],
                         actualRecord: Seq[GameDisplay]): Boolean = {
    testRecord.zip(actualRecord).forall(p => p._1.conforms(p._2))
  }

  case class TestWithPoints(test : Test, points : Double)
  case class InterleaveTest(name : String, testa : Test, testb : Test)
  case class InterleaveTestWithPoints(test : InterleaveTest, points : Double)

  type Points = Double

  class TestSuite extends FunSuiteLike with Matchers with TimeLimitedTests with BeforeAndAfterAll{

    // Testing via the via the test() method is done asynchronously
    // so we set these variables via side effects to give more statistics
    // such as the number of points obtained etc
    var nrTests : Int = 0
    var nrPassedTests : Int  = 0
    var scoredPoints : Double = 0
    var maxPoints  : Double = 0
    val filenameForPoints : Option[String] = None

    override def afterAll(): Unit = {
      super.afterAll()
      writePoints()
    }


    val InterleaveFailMsg =
      s"""
         |Assuming you passed the non-interleaved version of each test:
         |
         |You likely have some global state. Running two instances of $gameLogicName
         |and alternately doing steps between them results in some interference.
         |Running your game with a single  $gameLogicName instance works, but when
         |we have two  $gameLogicName instances running in 'parallel', the test fails.
    """

    def reportOnUniformlyScoredTests(testList: List[Test] = Nil,
                                     mainInterTestList: List[InterleaveTest] = Nil): Unit = {
        runUniformlyScoredTestsAndGetGrade(testList, mainInterTestList)
    }

    def runUniformlyScoredTestsAndGetGrade(tests: List[Test] = Nil,
                                           interTests: List[InterleaveTest] = Nil): Unit = {
      val nrTests = tests.length + interTests.length
      val scorePerTest: Double = FunctionalityPoints.toDouble / nrTests.toDouble

      val testsWithPoints: List[TestWithPoints] =
        tests.map(TestWithPoints(_, scorePerTest))

      val interTestsWithScore: List[InterleaveTestWithPoints] =
        interTests.map(InterleaveTestWithPoints(_, scorePerTest))

      runTestsAndGetGrade(testsWithPoints, interTestsWithScore)
    }

    def runTestsAndGetGrade(gts: List[TestWithPoints] = Nil,
                            gits: List[InterleaveTestWithPoints] = Nil): Unit = {
      gts.foreach(handleTest)
      gits.foreach(handleInterleaveTests)
    }



    def handleTest(t: TestWithPoints): Unit = {
      def actionsString(actions: Seq[GameAction]): String =
        "<" ++ actions.map(_.toString).mkString(", ") ++ ">"

      def printTraceFrame(frame: TestFrame, actual: GameDisplay, index: Int): Unit = {
        println(s"step=$index, rand=${frame.input.randomNumber}, actions=${actionsString(frame.input.actions)}")

        val frameIsCorrect = frame.display.conforms(actual)
        val frameString =
          if (frameIsCorrect) withHeader("Want & Got", frame.display.toString)
          else twoColumnTable("Want", "Got", frame.display.toString, actual.toString)

        println(frameString)
        println()
      }
      val (theTest, points) = (t.test,t.points)
      test(theTest.name) {

        val didPass = theTest.passes
        val ptsStr = if (didPass) f"+$points%.2f Points" else "No Points"
        val headerString = s"${theTest.name} : ${if (didPass) PassStr else FailStr} : $ptsStr"
        println(List.fill(headerString.length)("=").mkString + "\n" + headerString)

        if (!didPass) println("This is what went wrong:\n")
        else println("This is what we got & expected:\n")

        theTest.frames
          .lazyZip(theTest.implementationDisplays)
          .lazyZip(theTest.frames.indices).foreach(printTraceFrame)

        maxPoints += points
        nrTests += 1
        if(didPass) {
          nrPassedTests += 1
          scoredPoints += points
        }
        assert(didPass)

      }
    }

    def handleInterleaveTests(t: InterleaveTestWithPoints): Unit = {
      val (name, testA, testB, points) = (t.test.name,t.test.testa,t.test.testb,t.points)
      test(name) {
        val didPass = checkInterleave(testA, testB)
        val message = s"Interleave Test: ${testA.name}, ${testB.name} : " +
          s"${
            if (!didPass) FailStr + " : No Points\n" + InterleaveFailMsg.stripMargin
            else PassStr + f" : +$points%.2f Points"
          }"
        println("=" * StringUtils.widthOfMultilineString(message) + "\n" + message)

        maxPoints += points
        nrTests += 1
        if(didPass) {
          nrPassedTests += 1
          scoredPoints += points
        }
        assert(didPass)

      }
    }

    def writePoints(): Unit = {
      val fraction = scoredPoints / maxPoints
      val percentage = fraction * 100
      val resultStr = f"Total Functionality Points : $scoredPoints%.2f/$maxPoints%.2f [$percentage%.2f" + "%]"
      println(f"Passed $nrPassedTests%d/$nrTests%d tests")
      println(s"${"=" * resultStr.length}\n$resultStr\n${"=" * resultStr.length}")

      val initialCodeStyleGrade = fraction * CodeStylePoints
      println(f"(Initial code style points: $initialCodeStyleGrade%.2f)")
      filenameForPoints match {
        case Some(fn) =>
          val filename = s"grade_${fn.replace('.', '_')}.tmp"
          val pointStr = s"$scoredPoints"
          val writer = new PrintWriter(new File(filename))
          try {
            writer.println(pointStr)
          } finally {
            writer.close()
          }
        case None => ()
      }
    }
    override def timeLimit: Span = Span(1,Seconds)
    // this is need to actually stop when the buggy code contains an infinite loop
    override val defaultTestSignaler: Signaler = ReallyStopSignaler
  }


}

object GenericRecord {
  val PassStr = "[√] Pass"
  val FailStr = "[X] Fail"

  val DisplayPadWidth = 3
  val DisplayPadString: String = " " * DisplayPadWidth

  val FunctionalityPoints = 4
  val CodeStylePoints = 5

  val GameStepTimeout: FiniteDuration = 10 milliseconds
}


object ReallyStopSignaler extends Signaler {
  override def apply(testThread: Thread): Unit = {
    StopRunningNow.stopRunningNowUnsafe(testThread)
  }
}