package eu.joaocosta.pbt.props

import org.scalacheck._
import org.scalacheck.Prop._

import eu.joaocosta.pbt.props.Impls._

object ScalaCheckDefaultShrink extends App {

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
        Gen.choose(0, 100).map(_.toString)).map { case (c, n) => c + n }
    forAll(genMixedStr)(str => fun(str) == false).check()
  }

  test(lettersOnly)
  test(lettersOnlyBug)
}
