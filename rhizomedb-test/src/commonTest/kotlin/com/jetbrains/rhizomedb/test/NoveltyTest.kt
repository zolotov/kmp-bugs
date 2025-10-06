package com.jetbrains.rhizomedb.test

import com.jetbrains.rhizomedb.*
import kotlin.collections.emptyList
import kotlin.test.Test
import kotlin.test.assertEquals

class NoveltyTest {
  val manyAttr = Attribute.fromEID<Int>(
    42,
    Schema(
      cardinality = Cardinality.Many,
      isRef = false,
      indexed = false,
      unique = false,
      cascadeDelete = false,
      cascadeDeleteBy = false,
      required = false
    )
  )

  val oneAttr = Attribute.fromEID<Int>(
    43,
    Schema(
      cardinality = Cardinality.One,
      isRef = false,
      indexed = false,
      unique = false,
      cascadeDelete = false,
      cascadeDeleteBy = false,
      required = false
    )
  )

  @Test
  fun `cardinality many works`() {
    val n = MutableNovelty()
    val l = setOf(Datom(1, manyAttr, 1, 1, true),
                  Datom(1, manyAttr, 2, 1, true))
    n.addAll(l)
    assertEquals(l, n.persistent().toSet())
  }
  
  @Test
  fun `only last write to the attribute is present in novelty`() {

    // asserted - retracted
    run {
      val n = MutableNovelty()
      n.addAll(listOf(Datom(1, oneAttr, 1, 1, true),
                      Datom(1, oneAttr, 1, 1, false)))
      assertEquals(emptyList(), n.persistent().toList())
    }

    // retracted - asserted
    run {
      val n = MutableNovelty()
      n.addAll(listOf(Datom(1, oneAttr, 1, 1, false),
                      Datom(1, oneAttr, 1, 1, true)))
      assertEquals(emptyList(), n.persistent().toList())
    }

    run {
      val n = MutableNovelty()
      n.addAll(listOf(Datom(1, oneAttr, 1, 1, false),
                      Datom(1, oneAttr, 2, 2, true)))
      
      assertEquals(listOf(Datom(1, oneAttr, 1, 1, false),
                          Datom(1, oneAttr, 2, 2, true)), n.persistent().toList())
    }

    // asserted - retracted - asserted:
    run {
      val n = MutableNovelty()
      n.addAll(listOf(Datom(1, oneAttr, 1, 1, true),
                      Datom(1, oneAttr, 1, 1, false),
                      Datom(1, oneAttr, 2, 2, true)))
      assertEquals(listOf(Datom(1, oneAttr, 2, 2, true)), n.persistent().toList())
    }
    // retracted - asserted - retracted
    run {
      val n = MutableNovelty()
      n.addAll(listOf(Datom(1, oneAttr, 1, 1, false),
                      Datom(1, oneAttr, 2, 2, true),
                      Datom(1, oneAttr, 2, 2, false)))
      assertEquals(listOf(Datom(1, oneAttr, 1, 1, false)), n.persistent().toList())
    }
  }
  
  @Test
  fun `deduplicate values`() {
    val n = MutableNovelty()
    n.addAll(listOf(Datom(1, oneAttr, 1, 1, false),
                    Datom(1, oneAttr, 1, 2, true)))
    assertEquals(emptyList(), n.persistent ().deduplicateValues())
  }
  
  /**
   * It is important, because [fleet.kernel.DeserializeAttrs] instruction changes values without changing TX  
   * */
  @Test
  fun `datom may change value without changing TX`() {
    run {
      val n = MutableNovelty()
      n.addAll(listOf(Datom(1, oneAttr, 1, 1, false),
                      Datom(1, oneAttr, 2, 1, true)))
      assertEquals(listOf(Datom(1, oneAttr, 1, 1, false),
                          Datom(1, oneAttr, 2, 1, true)), n.persistent().toList())
    }
    
  }
}