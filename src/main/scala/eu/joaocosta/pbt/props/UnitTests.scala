package eu.joaocosta.pbt.props

import eu.joaocosta.pbt.props.Impls._

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
