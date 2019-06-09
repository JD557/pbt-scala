package eu.joaocosta.pbt.stateful

import eu.joaocosta.pbt.stateful.Impls._

object UnitTests extends App {
  def test(map: MyHashMap[String, Int]) {
    // Check that the map starts empty
    assert(map.toList.isEmpty == true)
    assert(map.get("") == None)
    assert(map.get("asd") == None)
    // Check that insertions work
    map.put("asd", 1)
    assert(map.get("") == None)
    assert(map.get("asd") == Some(1))
    assert(map.get("dsa") == None)
    map.put("dsa", 2)
    assert(map.get("") == None)
    assert(map.get("asd") == Some(1))
    assert(map.get("dsa") == Some(2))
    // Check that updates work
    map.put("asd", 3)
    assert(map.get("") == None)
    assert(map.get("asd") == Some(3))
    assert(map.get("dsa") == Some(2))
    assert(map.toList.sortBy(_._1) == List("asd" -> 3, "dsa" -> 2))
    // Check that deletions work
    map.delete("asd")
    assert(map.get("asd") == None)
    assert(map.get("dsa") == Some(2))
    assert(map.toList.sortBy(_._1) == List("dsa" -> 2))
  }

  test(new MyNaiveHashMap())
  test(new MyBuggyHashMap())
  println("All tests passed!")
}
