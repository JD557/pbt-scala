# Property Based Tests

A [Velocidi](http://velocidi.com) tech talk by [João Costa](http://joaocosta.eu) / [\@JD557](https://twitter.com/JD557)

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

  - Pick a unit of code to test
    - Mock dependencies (tested by other tests)
  - Write a set of test cases with known results
    - Make sure to test known edge cases (empty strings, zero,...)
  - Check the code coverage and branch coverage


