package eu.joaocosta.pbt

import eu.joaocosta.pbt.Helpers._

object UnitTestsStateful extends App {
  def test(map: MyHashMap[String, Int]) {
    map.get("") == None
    map.get("asd") == None
    map.put("asd", 1)
    map.get("") == None
    map.get("asd") == Some(1)
    map.put("dsa", 2)
    map.get("") == None
    map.get("asd") == Some(1)
    map.get("dsa") == Some(2)
    map.put("asd", 3)
    map.get("") == None
    map.get("asd") == Some(3)
    map.get("dsa") == Some(2)
    map.toList.sortBy(_._1) == List("asd" -> 3, "dsa" -> 2)
    map.delete("asd")
    map.get("asd") == None
    map.get("dsa") == Some(2)
    map.toList.sortBy(_._1) == List("dsa" -> 2)
  }

  test(new MyNaiveHashMap())
  test(new MyBuggyHashMap())
  println("All tests passed!")
}

object ScalaCheckStateful extends App {
  import org.scalacheck._
  import org.scalacheck.Arbitrary._
  import org.scalacheck.commands._
  import scala.util.{Try, Success}

  trait Spec extends Commands {
    type State = Map[String, Int]
    type Sut = MyHashMap[String, Int]

    // We can always create a new HashMap
    // This is useful if, for example, our SUT calls a running container
    def canCreateNewSut(newState: State,
                        initSuts: Traversable[State],
                        runningSuts: Traversable[Sut]): Boolean = true

    def initialPreCondition(state: State): Boolean = state.isEmpty

    def destroySut(sut: Sut): Unit = ()

    def genInitialState: Gen[State] = Gen.const(Map.empty)

    case class Get(key: String) extends Command {
      type Result = Option[Int]
      def run(sut: Sut): Result = sut.get(key)
      def nextState(state: State): State = state
      def preCondition(state: State): Boolean = true
      def postCondition(state: State, result: Try[Result]): Prop = result == Success(state.get(key))
    }

    case class Put(key: String, value: Int) extends UnitCommand {
      def run(sut: Sut): Unit = sut.put(key, value)
      def nextState(state: State): State = state + (key -> value)
      def preCondition(state: State): Boolean = true
      def postCondition(state: State, success: Boolean): Prop = success
    }

    case class Delete(key: String) extends UnitCommand {
      def run(sut: Sut): Result = sut.delete(key)
      def nextState(state: State): State = state - key
      def preCondition(state: State): Boolean = true
      def postCondition(state: State, success: Boolean): Prop = success
    }

    case object ToList extends Command {
      type Result = List[(String, Int)]
      def run(sut: Sut): Result = sut.toList()
      def nextState(state: State): State = state
      def preCondition(state: State): Boolean = true
      def postCondition(state: State, result: Try[Result]): Prop =
        result.map(_.sortBy(_._1)) == Success(state.toList.sortBy(_._1))
    }

    def genCommand(state: State): Gen[Command] = {
      // Generates a key that is present on the map
      val keyGen = Gen.oneOf[String]("default_key" :: state.keys.toList)
      Gen.oneOf(
        Gen.alphaLowerStr.map(str => Get(str)),
        keyGen.map(str => Get(str)),
        Gen.zip(Gen.alphaLowerStr, arbitrary[Int]).map{ case (k, v) => Put(k, v) },
        Gen.zip(keyGen, arbitrary[Int]).map{ case (k, v) => Put(k, v) },
        Gen.alphaLowerStr.map(str => Delete(str)),
        keyGen.map(str => Delete(str)),
        Gen.const(ToList)
      )
    }
  }

  object NaiveSpec extends Spec {
    def newSut(state: State): Sut = new MyNaiveHashMap()
  }
  object BuggySpec extends Spec {
    def newSut(state: State): Sut = new MyBuggyHashMap()
  }

  NaiveSpec.property().check()
  BuggySpec.property().check()
}
