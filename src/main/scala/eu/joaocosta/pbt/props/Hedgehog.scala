package eu.joaocosta.pbt.props

import hedgehog._
import hedgehog.core._

import eu.joaocosta.pbt.props.Impls._

object Hedgehog extends App {

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
