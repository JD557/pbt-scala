package eu.joaocosta.pbt

import eu.joaocosta.pbt.Helpers._

object UnitTests extends App {
  def test(fun: String => Boolean) {
    assert(fun("") == true)
    assert(fun("asd") == true)
    assert(fun("Asd") == true)
    assert(fun("0123") == false)
    assert(fun("   ") == false)
    assert(fun("01asd") == false)
  }

  test(lettersOnly)
  test(lettersOnlyBug)
  println("All tests passed!")
}

object ScalaCheckTests extends App {
  import org.scalacheck._
  import org.scalacheck.Prop._

  // Shrink our examples by removing one letter from the string
  implicit lazy val shrinkString: Shrink[String] = Shrink { s =>
    //s.tails.toStream.reverse.filter(_ != s)
    Set(s.drop(1), s.dropRight(1)).filter(_ != s).toStream 
  }

  // Note:
  //  Gen[T] => Generator for type T
  //  Arbitrary[T] => Wraps a Gen[T] that is used in implicit searches
  def test(fun: String => Boolean) {
    // Use the included string generators
    forAll(Gen.alphaLowerStr)(str => fun(str) == true).check()
    forAll(Gen.alphaUpperStr)(str => fun(str) == true).check()
    forAll(Gen.alphaStr)(str => fun(str) == true).check()

    // Create a new generator for numeric strings
    val genNumStr = Gen.choose(0, 100).map(_.toString)
    forAll(genNumStr)(str => fun(str) == false).check()

    // Create a new generator for mixed strings
    val genMixedStr = 
      Gen.zip(
        Gen.alphaChar.map(_.toString),
        Gen.choose(0, 100).map(_.toString)
      ).map { case (c, n) => c + n }
    forAll(genMixedStr)(str => fun(str) == false).check()
  }

  test(lettersOnly)
  test(lettersOnlyBug)
}

object ScalaCheckDefaultShrink extends App {
  import org.scalacheck._
  import org.scalacheck.Prop._

  // Note:
  //  Gen[T] => Generator for type T
  //  Arbitrary[T] => Wraps a Gen[T] that is used in implicit searches
  def test(fun: String => Boolean) {
    // Use the included string generators
    forAll(Gen.alphaLowerStr)(str => fun(str) == true).check()
    forAll(Gen.alphaUpperStr)(str => fun(str) == true).check()
    forAll(Gen.alphaStr)(str => fun(str) == true).check()

    // Create a new generator for numeric strings
    val genNumStr = Gen.choose(0, 100).map(_.toString)
    forAll(genNumStr)(str => fun(str) == false).check()

    // Create a new generator for mixed strings
    val genMixedStr = 
      Gen.zip(
        Gen.alphaChar.map(_.toString),
        Gen.choose(0, 100).map(_.toString)
      ).map { case (c, n) => c + n }
    forAll(genMixedStr)(str => fun(str) == false).check()
  }

  test(lettersOnly)
  test(lettersOnlyBug)
}

object HedgehogTests extends App {
  import hedgehog._
  import hedgehog.core._

  def test(fun: String => Boolean) {

		def check(gen: GenT[String], expected: Boolean): Unit = {
			println(Property.checkRandom(
				PropertyConfig.default, gen.forAll.map(str => fun(str) ==== expected)
			))
		}

    // Manually define our generators
    val alphaLowerStr = Gen.string(Gen.lower, Range.linear(0, 10))
    val alphaUpperStr = Gen.string(Gen.upper, Range.linear(0, 10))
    val alphaStr = Gen.string(Gen.alpha, Range.linear(0, 10))

		check(alphaLowerStr, true)
		check(alphaUpperStr, true)
		check(alphaStr, true)

    // Create a new generator for numeric strings
    val numStr = Gen.string(Gen.digit, Range.linear(1, 10))
		check(numStr, false)

    // Create a new generator for mixed strings
    val genMixedStr: GenT[String] = for {
      alpha <- alphaStr
      num <- numStr
    } yield alpha + num
    check(genMixedStr, false)
  }

  test(lettersOnly)
  test(lettersOnlyBug)
}
