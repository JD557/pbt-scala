package eu.joaocosta.pbt.props

import eu.joaocosta.pbt.props.Impls._

object UnitTests extends App {
  def test(fun: String => Boolean): Unit = {
    val result = scala.util.Try {
      assert(fun("") == true)
      assert(fun("aefioafsio") == true)
      assert(fun("IHAU") == true)
      assert(fun("ApBoSiA") == true)
      assert(fun("0123") == false)
      assert(fun("   ") == false)
      assert(fun("01asd") == false)
    }.isSuccess
    println("Test status: " + (if (result) "SUCCESS" else "FAILURE"))
  }

  test(lettersOnlyMock)
  test(lettersOnly)
  test(lettersOnlyBug)
}
