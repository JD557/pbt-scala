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

### Typical Testing procedure

 * Pick a unit of code to test
    - Mock dependencies (tested by other tests)
 * Write a set of test cases with known results
    - Make sure to test known edge cases (empty strings, zero,...)
 * Check the code coverage and branch coverage


---

## Testing Stateless Functions


We want to test a function `f: A => B` that:

 * Is deterministic
 * Performs no side-effects
 * Doesn't access any global state 

---

### Example Problem

We want to write a function `lettersOnly: String => Boolean` that returns `true` if the string contains only letters (`[a-zA-z]`), and `false` otherwise.

---

#### Naive approach

We can write a test suite that:

* Tests the base case:
  ```scala
  assert(fun("") == true)
  ```
* Tests known `true` cases
  ```scala
    assert(fun("aefioafsio") == true) // Lowercase
    assert(fun("IHAU") == true) // Uppercase
    assert(fun("ApBoSiA") == true) // Lowercase and Uppercase
  ```
* Tests known `false` cases
  ```scala
    assert(fun("0123") == false)
    assert(fun("   ") == false)
    assert(fun("01asd") == false)
  ```

---

We then write an implementation:
```scala
  def lettersOnly(string: String): Boolean = {
    val letters = ('a' until 'z').toSet
    string.forall(c => letters.contains(c.toLower))
  }

```

And run the tests:
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

#### ScalaCheck notes

ScalaCheck contains an extra type `Arbitrary`, which is simply a wrapper that store `Gen`s to be used in implicit searches.

**Example**
- `Arbitrary.arbitrary[String]: Gen[String]` - Default `String` generator
- `Gen.alphaStr: Gen[String]` - Generates only `String`s in `[a-zA-Z]*`

Also, the included generators are usually biased towards known edge cases (empty strings, 0,...)

---

#### QuickCheck approach

We can write a test suite that:
- Tests known `true` cases
  ```scala
    forAll(Gen.alphaLowerStr)(str => fun(str) == true).check()
    forAll(Gen.alphaUpperStr)(str => fun(str) == true).check()
    forAll(Gen.alphaStr)(str => fun(str) == true).check()
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
    forAll(genMixedStr)(str => fun(str) == false).check()

    // Creates a generator for whitespace strings
    val genWhitespaceStr =
      Gen.nonEmptyListOf(Gen.oneOf(' ', '\t', '\r', '\n', '\u0000'))
        .map(_.mkString)
    forAll(genWhitespaceStr)(str => fun(str) == false).check()

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
