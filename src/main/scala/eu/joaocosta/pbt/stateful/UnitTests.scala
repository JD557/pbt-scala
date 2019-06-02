package eu.joaocosta.pbt.stateful

import eu.joaocosta.pbt.stateful.Impls._

object UnitTests extends App {
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
