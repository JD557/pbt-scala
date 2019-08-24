# Property Based Tests

A [Velocidi](http://velocidi.com) tech talk by [João Costa](http://joaocosta.eu) / [@JD557](https://twitter.com/JD557)

---

## Overview

  - Software Testing
  - Testing Stateless Functions
    - Unit Tests
    - QuickCheck
    - Hedgehog
    - Tips
  - Testing Stateful Systems
    - Unit Tests
    - QuickCheck
    - Tips

---

## Sofware Testing

  From "Glenford J. Myers, *The Art of Software Testing* (1979)":

  - Testing is the process of executing a program with the intent of finding errors
  - A good test case is one that has a high probability of detecting an as yet undiscovered error
  - **A successful test case is one that detects an as yet undiscovered error**

---

### Typical Testing Procedure

 * Pick a small unit of code to implement
 * Write a set of test cases with known results
    - Make sure to test known edge cases (empty strings, zero,...)
    - Use mock dependencies (those are tested by other tests)
 * Check that the test fails if the feature is not implemented
 * Implement the feature
 * Run the tests
   - They should succeed
   - Check the code coverage and branch coverage

---

## Testing Stateless Functions

We want to test a function `f: A => B` that:

 * Is deterministic
 * Performs no side-effects
 * Doesn't access any global state 

---

### Example Problem

Implement `lettersOnly: String => Boolean`
 - Returns `true` if the string contains only letters (`[a-zA-z]`)
 - Returns`false` otherwise

---

#### Naive approach

##### Write a set of test cases with known results

* Test the base case:
  ```scala
  assert(lettersOnly("") == true)
  ```
* Test known `true` cases
  ```scala
    assert(lettersOnly("aefioafsio") == true) // Lowercase
    assert(lettersOnly("IHAU") == true) // Uppercase
    assert(lettersOnly("ApBoSiA") == true) // Lowercase and Uppercase
  ```
* Test known `false` cases
  ```scala
    assert(lettersOnly("0123") == false)
    assert(lettersOnly("   ") == false)
    assert(lettersOnly("01asd") == false)
  ```

---

##### Check that the test fails if the feature is not implemented

```
  def lettersOnly(string: String): Boolean = false
```

Tests fail `assert(lettersOnly("") == true)`

---

##### Implement the feature

```scala
  def lettersOnly(string: String): Boolean = {
    val letters = ('a' until 'z').toSet
    string.forall(c => letters.contains(c.toLower))
  }

```
---

##### Run the tests

- All tests pass
- Code coverage: 100%
- Branch coverage: 100%

---

This looks good, but our code is actually **WRONG**:
```scala
  def lettersOnly(string: String): Boolean = {
    val letters = ('a' until 'z').toSet
    string.forall(c => letters.contains(c.toLower))
  }

```
Can you spot the error?
How could we avoid this?

---

#### Problems with our approach

 * Tests only test a small sample of the total input space
 * Ideally, we would like to use write tests like:
    - $\forall s\in[a \textrm{--} zA\textrm{--}z]^* \ldotp lettersOnly(s)$
    - $\forall s\in[0 \textrm{--} 9]^+ \ldotp \neg{lettersOnly(s)}$
    - ...
 *  We could simply use random inputs in our tests
    - `Error: lettersOnly("qudihidqhidqwhuidwqhidzuqhiwuhsdioa") is false`
    - This helps, but it's hard to find out what's causing the bug

---

#### QuickCheck

- Created in 1999 by Koen Claessen and John Hughes, in Haskell
- Ported to multiple languages (e.g. ScalaCheck)
- Two main concepts:
  - `Gen[T]` - Generates an arbitrary value of type `T`
  - `Shrinks[T]` - Given a value, attempts to generate a simpler value
- For each property:
  - Generate a random input `x: T` using `Gen[T]`
  - If the test succeeds, repeat (N times)
  - If the test fails, repeatedly shrink the input with `Shrink[T]` until it the test succeeds or it can't be shrunk anymore.

---

#### Arbitrary vs Gen

QuickCheck contains an extra type `Arbitrary`, which is simply a typeclass that store `Gen`s.
In ScalaCheck, `Gen`s are explicit while `Arbitrary`s are used in implicit searches.

**Example**
- `Arbitrary.arbitrary[String]: Gen[String]` - Default `String` generator
- `Gen.alphaStr: Gen[String]` - Generates only `String`s in `[a-zA-Z]*`

Also, the included generators are usually biased towards known edge cases (empty strings, 0,...)

---

#### Properties

Our tests are now written as properties (`Prop`) of the form $\forall x \ldotp P(x)$

**Example**
  ```scala
    forAll((x: Int) => prop(x)) // Using Arbitrary[Int]
    forAll(Gen.alphaStr)(str => prop(str)) // Using a custom Gen[String]
    forAll((x: Int, str: String) => prop(x, str)) // Using multiple arguments
  ```

Properties can be combined (`&&`, `||`, `==>`,...) or tested (`check()`)

**Example**
  ```scala
    forAll((x: Int) => x != 0 ==> exists((y: Int) => prop(y / x))).check()
  ```

---

#### QuickCheck approach

We can write a test suite that:
- Tests known `true` cases
  ```scala
    forAll(Gen.alphaLowerStr)(str => lettersOnly(str) == true).check()
    forAll(Gen.alphaUpperStr)(str => lettersOnly(str) == true).check()
    forAll(Gen.alphaStr)(str => lettersOnly(str) == true).check()
  ```

---

- Tests known `false` cases
  ```scala
    // Create a new generator for strings with numbers
    // (Possibly with letters)
    val genMixedStr =
      Gen.zip(
        Gen.alphaStr, // Note that this can be empty
        Gen.choose(0, 1000)).map { case (s, n) => s + n }
    forAll(genMixedStr)(str => lettersOnly(str) == false).check()

    // Creates a generator for whitespace strings
    val genWhitespaceStr =
      Gen.nonEmptyListOf(Gen.oneOf(' ', '\t', '\r', '\n', '\u0000'))
        .map(_.mkString)
    forAll(genWhitespaceStr)(str => lettersOnly(str) == false).check()

  ```
---

- Write a `Shrink[String]` to shrink our examples:

```scala
  // Shrink our examples by removing one letter from the string
  implicit lazy val shrinkString: Shrink[String] = Shrink { s =>
    Set(s.drop(1), s.dropRight(1)).filter(_ != s).toStream
  }
```

---

**Test Results**

```
! Falsified after 6 passed tests.
> ARG_0: "z"
> ARG_0_ORIGINAL: "eivzh"
```

- `ARG_0_ORIGINAL`: Original tested value (e.g. `"eivzh"`)
- `ARG_0`: Shrunken value (e.g `"z"`)

---

It appears that `z` is an invalid input!

```scala
  def lettersOnly(string: String): Boolean = {
    val letters = ('a' until 'z').toSet
    string.forall(c => letters.contains(c.toLower))
  }
```

Should actually be:

```scala
  def lettersOnly(string: String): Boolean = {
    val letters = ('a' to 'z').toSet
    string.forall(c => letters.contains(c.toLower))
  }
```

---

#### Shrinking

We implemented our custom `Shrink[String]`, but ScalaCheck already comes with a default implementation.

Let's try it!

---

```
! Falsified after 4 passed tests.
> ARG_0: "\u0000"
> ARG_0_ORIGINAL: "ezg"
```

Something went wrong...
 * `lettersOnly("\u0000") == false`, as expected.
 *  Our test was `forAll(Gen.alphaLowerStr)(str => fun(str) == true)`
    * It failed the test because it used a `String` that is not an `alphaLowerStr`
 * **The Shrinker has no knowledge of the generator, so it can generate invalid values!!!**

---

#### Hedgehog

- Alternative to QuickCheck
- Created in 2017
- `Shrink` is now part of `Gen`
    - Instead of generating values, `Gen` generates a `Tree[A](value: A, children: LazyList[Tree[A]])` internally
  - This solves our problem
  - Creating a new `Gen` is slightly more cumbersome
- Less supported than QuickCheck

---

#### Hedgehog implementation

```scala
    // Helper function to avoid the hedgehog-runtime boilerplate
    def forAll(gen: GenT[String], expected: Boolean): Unit = ???

    // Manually define our generators
    val alphaLowerStr = Gen.string(Gen.lower, Range.linear(0, 10))
    val alphaUpperStr = Gen.string(Gen.upper, Range.linear(0, 10))
    val alphaStr = Gen.string(Gen.alpha, Range.linear(0, 10))

    forAll(alphaLowerStr, true)
    forAll(alphaUpperStr, true)
    forAll(alphaStr, true)

    // Create a new generator for numeric strings
    val numStr = Gen.string(Gen.digit, Range.linear(1, 10))
    forAll(numStr, false)

    // Create a new generator for mixed strings
    val genMixedStr: GenT[String] = for {
      alpha <- alphaStr
      num <- numStr
    } yield alpha + num
    forAll(genMixedStr, false)

```

---

#### Tips

- When a test fails, add it to your unit tests (can be a useful regression test)
- Avoid the use of `Gen.filter`
- Don't reimplement your logic
  - Imagine if we did `Gen.listOf(('a' until 'z'))`
  - Test properties using other unrelated methods
    - e.g. `forAll { list: List[Int] => list.lastOption == list.reverse.headOption }`
- You don't need to use property based tests everywhere
  - e.g. "`foo` should parse a gzipped csv"

---

## Testing Stateful Systems

We want to test a system that:

 * Is deterministic
 * Can perform side-effects
 * Has an internal state 
 
---

### Example Problem

We want to write a simple mutable Hash Map

```scala
  class HashMap[K, V](???) {
    def get(key: K): Option[V] = ???
    def put(key: K, value: V): Unit = ???
    def delete(key: K): Unit = ???
    def toList(): List[(K, V)] = ???
}
```

---

#### Naive approach

##### Write a set of test cases with known results

- Create a new HashMap
```scala
  val map = new HashMap[String, Int]()
```

- Check that the map starts empty
```scala
    assert(map.toList.isEmpty == true)
    assert(map.get("") == None)
    assert(map.get("asd") == None)
```

---

- Check that insertions only insert the specified value
```scala
    map.put("asd", 1)
    assert(map.get("") == None)
    assert(map.get("asd") == Some(1))
    assert(map.get("dsa") == None)
    map.put("dsa", 2)
    assert(map.get("") == None)
    assert(map.get("asd") == Some(1))
    assert(map.get("dsa") == Some(2))
```

---

- Check that updates only update the specified value
```scala
    map.put("asd", 3)
    assert(map.get("") == None)
    assert(map.get("asd") == Some(3))
    assert(map.get("dsa") == Some(2))
    assert(map.toList.sortBy(_._1) == List("asd" -> 3, "dsa" -> 2))
```

- Check that deletions remove the specified value
```scala
    map.delete("asd")
    assert(map.get("asd") == None)
    assert(map.get("dsa") == Some(2))
```

- Inspect the final state
```scala
    assert(map.toList.sortBy(_._1) == List("dsa" -> 2))
```

---

##### Check that the test fails if the feature is not implemented

```scala
  class HashMap[K, V]() {
    def get(key: K): Option[V] = None
    def put(key: K, value: V): Unit = ()
    def delete(key: K): Unit = ()
    def toList(): List[(K, V)] = Nil
}
```

Fails on `assert(map.get("asd") == Some(1))`

---

##### Implement the feature

```scala
  class HashMap[K, V](capacity: Int = 128) {
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
      buffer(pos) = Nil
    }

    def toList(): List[(K, V)] = buffer.flatten.toList
  }
```

---

##### Run the tests

- All tests pass
- Code coverage: 100%
- Branch coverage: 100%

However, again, our code is **WRONG**!

---

#### Problems with our approach

 * Tests only a sequence of state transitions
 * Ideally, we would like to test multiple sequences of state transitions

---

#### Stateful tests with QuickCheck

- ScalaCheck provides a `Commands` API with:
  - `Sut`: Mutable system under test
  - `State`: Immutable state that models our `Sut` (usually a simpler model)
  - `Command`: An operation to execute on our `Sut` that:
    - Has a pre-condition
    - Has a post-condition
    - Performs a `State` transition

---

#### QuickCeck Approach

We define our `State` and `Sut` and initial states:
```scala
type State = Map[String, Int]
type Sut = HashMap[String, Int]

// We can always create a new HashMap
// This is useful if, for example, our SUT calls a running container
def canCreateNewSut(
  newState: State,
  initSuts: Traversable[State],
  runningSuts: Traversable[Sut]): Boolean = true

def destroySut(sut: Sut): Unit = ()

def genInitialState: Gen[State] = Gen.const(Map.empty)

// Used to shrink the initial state
def initialPreCondition(state: State): Boolean = state.isEmpty
```

---

We define our Commands:
```scala
case class Get(key: String) extends Command {
  type Result = Option[Int]
  def run(sut: Sut): Result = sut.get(key)
  def nextState(state: State): State = state
  def preCondition(state: State): Boolean = true
  def postCondition(state: State, result: Try[Result]): Prop = result == Success(state.get(key))
}

case class Put(key: String, value: Int) extends UnitCommand {
  def run(sut: Sut): Unit = sut.put(key, value)
  def nextState(state: State): State = state + (key -> value)
  def preCondition(state: State): Boolean = true
  def postCondition(state: State, success: Boolean): Prop = success
}

case class Delete(key: String) extends UnitCommand {
  def run(sut: Sut): Result = sut.delete(key)
  def nextState(state: State): State = state - key
  def preCondition(state: State): Boolean = true
  def postCondition(state: State, success: Boolean): Prop = success
}

case object ToList extends Command {
  type Result = List[(String, Int)]
  def run(sut: Sut): Result = sut.toList()
  def nextState(state: State): State = state
  def preCondition(state: State): Boolean = true
  def postCondition(state: State, result: Try[Result]): Prop =
    result.map(_.sortBy(_._1)) == Success(state.toList.sortBy(_._1))
}
```

---

We define our `Gen[Command]`:

```scala
def genCommand(state: State): Gen[Command] = {
  // Generates a key that is present on the map
  val keyGen = Gen.oneOf[String]("default_key" :: state.keys.toList)
  Gen.oneOf(
    Gen.alphaLowerStr.map(str => Get(str)),
    keyGen.map(str => Get(str)),
    Gen.zip(Gen.alphaLowerStr, arbitrary[Int]).map { case (k, v) => Put(k, v) },
    Gen.zip(keyGen, arbitrary[Int]).map { case (k, v) => Put(k, v) },
    Gen.alphaLowerStr.map(str => Delete(str)),
    keyGen.map(str => Delete(str)),
    Gen.const(ToList))
}
```
---

And finally, we run our tests:

```
! Falsified after 64 passed tests.
> Labels of failing property:
initialstate = Map()
seqcmds = (Put(suyqjkjeehbqtlaqnxazyatmzbnxfjqvmkporafjpgtuxtluylwzhkinfm,0
  ); Delete(default_key); ToList => List())
> ARG_0: Actions(Map(),List(Put(suyqjkjeehbqtlaqnxazyatmzbnxfjqvmkporafjpgt
  uxtluylwzhkinfm,0), Delete(default_key), ToList),List())
> ARG_0_ORIGINAL: Actions(Map(),List(Get(cokxwqslowszddtkgncnlvclqdiuahsjzb
  ukerkgyhibjifhncndzatxkjomc), Delete(default_key), Put(default_key,-21474
  83648), Delete(ihmvdlplfwlckxiyutg), Get(yrylpvrjhaneqnmhnujyrvumtmxuilpu
  cbeqjwddemmxjbioaxymoyvpebr), Get(ykjetehimcu), Put(default_key,-21474836
  48), ToList, Get(mjylrfcfhhvghjvzrreifwqbydceuvpxuxqx), ToList, ToList, P
  ut(suyqjkjeehbqtlaqnxazyatmzbnxfjqvmkporafjpgtuxtluylwzhkinfm,0), Get(pxp
  oszdvqnkekigkkrtaxsmhywnrbxwkxtlncugtcknjrqhajbcmolxvupsfdz), Put(bxmquea
  fgojulofqmynoey,-1957587762), Get(tleuphfwc), Delete(bxmqueafgojulofqmyno
  ey), Get(default_key), Delete(default_key), ToList, Delete(default_key),
  ToList, Delete(oplfxpgkekfyeutounmnayx), Get(suyqjkjeehbqtlaqnxazyatmzbnx
  fjqvmkporafjpgtuxtluylwzhkinfm), Delete(suyqjkjeehbqtlaqnxazyatmzbnxfjqvm
  kporafjpgtuxtluylwzhkinfm), Put(twjsvt,-1691763425), Put(twjsvt,117511834
  3), Delete(wfcihiykssmeunkuyhwxwcdvfgyuijwgcadveoxxeqedkurrogfqmiisjiwdjp
  je), Get(lrmfsyyylnmolhalgzajyonco), ToList, Put(nsumljqwsqa,-706203824),
   Get(nsumljqwsqa), Put(yjtuzxjmseidjypombuyjolvtmquwqohpxqvzyckqllvrjlpxh
  agmc,-1), Put(zmidlhfuptraspkvlydwxwnexfobbwjhdlcgsvpk,1), Put(zmidlhfupt
  raspkvlydwxwnexfobbwjhdlcgsvpk,2147483647), Get(default_key), Delete(bnbj
  nbdyqvcot), Get(twjsvt), Put(jyjsiozaaoxdcvecgcjupbgpmufqqjpgkxeoqrbgtwnq
  ,-459846117), Put(fxklvxqvrvxmmaiiyimdoxcnlgwkxmdmoyoafiizbjmasxt,0), Del
  ete(yjtuzxjmseidjypombuyjolvtmquwqohpxqvzyckqllvrjlpxhagmc), Delete(mxdhd
  hcxgdkrnkpmatwcxlyejlcvypfdkcqelbomqsgumhbvjnpl), Get(default_key), Put(g
  zmvbjkvlabvfoxac,2081989555), Get(fmujwqjwuoadrpwcxiebjaxkkzkbpvvqvjw), D
  elete(rqnwjg), ToList, Delete(zqybwwzmdaqtuappuhon), Put(fxklvxqvrvxmmaii
  yimdoxcnlgwkxmdmoyoafiizbjmasxt,-2147483648), Delete(wzgovsegmodtugwqdahg
  piwfxswcwvnqrzrlnmsekfczeictkycflys), Delete(kmdisbyowaoqnrmurtbbhylwmrtk
  hdkl), Get(ztjaikdqvzjuvgloyflunvunrzsqoehojzxxuptlyrzony), Delete(zmidlh
  fuptraspkvlydwxwnexfobbwjhdlcgsvpk), Delete(cmksdudjpbiknumyhejatsobcnlcr
  ldlvdhlif), Delete(fccbggmlakbnwxnwavwrw), ToList, Put(ritukulpo,1), Put(
  fxklvxqvrvxmmaiiyimdoxcnlgwkxmdmoyoafiizbjmasxt,1), ToList, Put(ritukulpo
  ,-700753175), Put(apqcnwqwywwnsuclkiloxgiuubhnf,-751821836), Put(wlewoygr
  f,2147483647), Get(gzmvbjkvlabvfoxac), Get(twjsvt), Put(jyjsiozaaoxdcvecg
  cjupbgpmufqqjpgkxeoqrbgtwnq,1)),List())
```

---

- Those are a lot of commands... Let's look at the shrinked result
  * `Put(suyqjkjeehbqtlaqnxazyatmzbnxfjqvmkporafjpgtuxtluylwzhkinfm,0)`
  * `Delete(default_key)`
  * `ToList` => `List()`

* Wow, what happened here?
  * `Delete(default_key)` also deleted `suyqjkjeehbqtlaqnxazyatmzbnxfjqvmkporafjpgtuxtluylwzhkinfm`
  * `hash(default_key) == hash(suyqjkjeehbqtlaqnxazyatmzbnxfjqvmkporafjpgtuxtluylwzhkinfm)`
  * Our delete fails when there's an hash collision!

---

```scala
def delete(key: K): Unit = {
  val pos = Math.floorMod(key.##, capacity)
  buffer(pos) = Nil
}
```

should be

```scala
def delete(key: K): Unit = {
  val pos = Math.floorMod(key.##, capacity)
  val newList = buffer(pos).filter(_._1 != key)
  buffer(pos) = newList
}
```

---

#### Tips

- Your model should be a simpler version of your system
- Give some love to `genCommand`
  - e.g. use `Gen.frequency` to test interesting command sequences

---

## Regarding ScalaCheck problems

- Shrinking commands is currently not working with scala 2.12 [scalacheck/#468](https://github.com/typelevel/scalacheck/pull/468)
- Automatic shrinking might be removed when using `Gen` [scalacheck/#440](https://github.com/typelevel/scalacheck/pull/468)
- ScalaCheck has recently moved to Typelevel

---

## Some other projects

- [scalacheck-shapeless](https://github.com/alexarchambault/scalacheck-shapeless): Reduce the boilerplate necessary to generate case classes
- [discipline](https://github.com/typelevel/discipline): Helper library for typeclass hierarchy laws
  - Used by most libraries with huge typeclass hierarchies (cats, spire, scalaz...)
  - Useful test typeclass instances "for free" (e.g. `checkAll("MovieRating", spire.laws.GroupLaws[MovieRating].cMonoid)`)
- [zio-test](https://github.com/zio/zio)
  - Has the concept of `Sample[-R, +A](value: A, shrink: ZStream[R, Nothing, Sample[R, A]])`, which is a value and a stream that generates shrinked values
  - A `Gen[-R, +A]` is simply a stream of `Sample`s, so generation and shrinking also get coupled
  - Still experimental

---

## Questions
