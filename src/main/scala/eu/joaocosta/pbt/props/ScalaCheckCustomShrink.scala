package eu.joaocosta.pbt.props

import org.scalacheck._
import org.scalacheck.Prop._

import eu.joaocosta.pbt.props.Impls._

object ScalaCheckCustomShrink extends App {

  // Shrink our examples by removing one letter from the string
  implicit lazy val shrinkString: Shrink[String] = Shrink { s =>
    Set(s.drop(1), s.dropRight(1)).filter(_ != s).toStream
  }

  // Note:
  //  Gen[T] => Generator for type T
  //  Arbitrary[T] => Wraps a Gen[T] that is used in implicit searches
  def test(fun: String => Boolean): Unit = {
    // Use the included string generators
    forAll(Gen.alphaLowerStr)(str => fun(str) == true).check()
    forAll(Gen.alphaUpperStr)(str => fun(str) == true).check()
    forAll(Gen.alphaStr)(str => fun(str) == true).check()

    // Create a new generator for strings with numbers
    // (Possibly with letters)
    val genMixedStr =
      Gen.zip(
        Gen.alphaStr, // Note that this can be empty
        Gen.choose(0, 1000)).map { case (s, n) => s + n }
    forAll(genMixedStr)(str => fun(str) == false).check()

    // Creates a generator for whitespace strings
    val genWhitespaceStr =
      Gen.nonEmptyListOf(Gen.oneOf(' ', '\t', '\r', '\n', '\u0000'))
        .map(_.mkString)
    forAll(genWhitespaceStr)(str => fun(str) == false).check()
  }

  test(lettersOnly)
  test(lettersOnlyBug)
}
