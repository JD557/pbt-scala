package eu.joaocosta.pbt.stateful

import scala.collection.mutable

object Impls {
  trait MyHashMap[K, V] {
    def get(key: K): Option[V]
    def put(key: K, value: V): Unit
    def delete(key: K): Unit
    def toList(): List[(K, V)]
  }

  class MyMockHashMap[K, V]() extends MyHashMap[K, V] {
    def get(key: K): Option[V] = None
    def put(key: K, value: V): Unit = ()
    def delete(key: K): Unit = ()
    def toList(): List[(K, V)] = Nil
  }

  class MyNaiveHashMap[K, V](capacity: Int = 128) extends MyHashMap[K, V] {
    private val buffer: mutable.ArrayBuffer[List[(K, V)]] = mutable.ArrayBuffer.fill(capacity)(List.empty)
    def get(key: K): Option[V] = {
      val pos = Math.floorMod(key.##, capacity)
      buffer(pos).find(_._1 == key).map(_._2)
    }

    def put(key: K, value: V): Unit = {
      val pos = Math.floorMod(key.##, capacity)
      val newList = (key, value) :: buffer(pos).filter(_._1 != key)
      buffer(pos) = newList
    }

    def delete(key: K): Unit = {
      val pos = Math.floorMod(key.##, capacity)
      val newList = buffer(pos).filter(_._1 != key)
      buffer(pos) = newList
    }

    def toList(): List[(K, V)] = buffer.flatten.toList
  }

  class MyBuggyHashMap[K, V](capacity: Int = 128) extends MyHashMap[K, V] {
    private val buffer: mutable.ArrayBuffer[List[(K, V)]] = mutable.ArrayBuffer.fill(capacity)(List.empty)
    def get(key: K): Option[V] = {
      val pos = Math.floorMod(key.##, capacity)
      buffer(pos).find(_._1 == key).map(_._2)
    }

    def put(key: K, value: V): Unit = {
      val pos = Math.floorMod(key.##, capacity)
      val newList = (key, value) :: buffer(pos).filter(_._1 != key)
      buffer(pos) = newList
    }

    def delete(key: K): Unit = {
      val pos = Math.floorMod(key.##, capacity)
      buffer(pos) = Nil // Uh oh, we are not handling hash collisions!
    }

    def toList(): List[(K, V)] = buffer.flatten.toList
  }
}
