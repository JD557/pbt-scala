package eu.joaocosta.pbt.props

object Impls {

  def lettersOnlyMock(string: String): Boolean = false

  def lettersOnly(string: String): Boolean = {
    val letters = ('a' to 'z').toSet
    string.forall(c => letters.contains(c.toLower))
  }

  def lettersOnlyBug(string: String): Boolean = {
    val letters = ('a' until 'z').toSet // Oops, this discards the 'z'!
    string.forall(c => letters.contains(c.toLower))
  }
}
