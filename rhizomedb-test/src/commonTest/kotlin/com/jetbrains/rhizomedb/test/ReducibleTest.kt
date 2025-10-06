package com.jetbrains.rhizomedb.test


import fleet.util.reducible.reducible
import fleet.util.reducible.singleOrNull
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class ReducibleTest {
  @Test
  fun singleOrNull() {
    assertNull(emptyList<Int>().reducible().singleOrNull())
    assertEquals(1, listOf(1).reducible().singleOrNull())
    assertFailsWith<Throwable> {
      listOf(1, 2).reducible().singleOrNull()
    }
  }
}