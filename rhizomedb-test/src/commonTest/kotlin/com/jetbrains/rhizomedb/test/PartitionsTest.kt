package com.jetbrains.rhizomedb.test

import com.jetbrains.rhizomedb.*
import com.jetbrains.rhizomedb.Entity
import com.jetbrains.rhizomedb.entities
import kotlinx.serialization.builtins.SetSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals

private const val FirstPartKey = 1
private const val SecondPartKey = 2

data class FirstPart(override val eid: EID) : Entity {
  val foo: String by FooAttr
  val friend: SecondPart? by FriendAttr

  companion object : EntityType<FirstPart>(FirstPart::class, ::FirstPart) {
    val FooAttr = requiredValue("foo", String.serializer(), Indexing.INDEXED)
    val FriendAttr = optionalRef<SecondPart>("friend")
  }
}

data class SecondPart(override val eid: EID) : Entity {
  val bar: String by BarAttr

  companion object : EntityType<SecondPart>(SecondPart::class, ::SecondPart) {
    val BarAttr = requiredValue("bar", String.serializer(), Indexing.INDEXED)
  }
}

class PartitionsTest {

  private fun partitionedDB(): DB {
    return EntitiesTest.emptyDb()
  }

  @Test
  fun `transaction to partitioned db`() {
    val db = partitionedDB().change {
      register(FirstPart)
      register(SecondPart)
      withDefaultPart(FirstPartKey) {
        FirstPart.new {
          it[FirstPart.FooAttr] = "foo"
          it[FirstPart.FriendAttr] = withDefaultPart(SecondPartKey) {
            SecondPart.new {
              it[SecondPart.BarAttr] = "bar"
            }
          }
        }
      }
    }.dbAfter
    asOf(db) {
      val firstPart = entities(FirstPart.FooAttr, "foo").single()
      val secondPart = firstPart.friend
      assertEquals("bar", secondPart!!.bar)
    }
  }

  @Test
  fun `query across partitions`() {
    val db = partitionedDB().change {
      register(FirstPart)
      register(SecondPart)
      withDefaultPart(FirstPartKey) {
        FirstPart.new {
          it[FirstPart.FooAttr] = "foo"
          it[FirstPart.FriendAttr] = withDefaultPart(SecondPartKey) {
            SecondPart.new {
              it[SecondPart.BarAttr] = "bar"
            }
          }
        }
      }
    }.dbAfter

    asOf(db) {
      assertEquals(setOf("foo", "bar"), queryIndex(IndexQuery.All()).toList().map { (e, a, v) -> v }.filter { v ->
        v == "foo" || v == "bar"
      }.toSet())
    }
  }

  @Test
  fun `partitions are isolated`() {
    val db = partitionedDB().change {
      register(FirstPart)
      register(SecondPart)
      withDefaultPart(FirstPartKey) {
        FirstPart.new {
          it[FirstPart.FooAttr] = "foo"
          it[FirstPart.FriendAttr] = withDefaultPart(SecondPartKey) {
            SecondPart.new {
              it[SecondPart.BarAttr] = "bar"
            }
          }
        }
      }
    }.dbAfter
    val db1 = db.selectPartitions(setOf(FirstPartKey))
    val db2 = db.selectPartitions(setOf(SecondPartKey))

    asOf(db1) {
      entities(FirstPart.FooAttr, "foo").single()
      // here we try to bypass query cache to ensure that schema information is present in index
//      queryIndex(IndexQuery.Column(LegacySchema.Attr.kProperty)).first()
    }

    asOf(db2) {
      entities(SecondPart.BarAttr, "bar").single()
      // here we try to bypass query cache to ensure that schema information is present in index
//      queryIndex(IndexQuery.Column(LegacySchema.Attr.kProperty)).first()
    }
  }

  /*
    @Test
    fun `db is updated to reflect changes in merged partitions`() {
      val db = partitionedDB().tx {
        new(FirstPart::class) {
          foo = "foo"
          friend = new(SecondPart::class) {
            bar = "bar"
          }
        }
      }
  
      val dbPart = db.select(setOf(FirstPartKey))
  
      val dbPartPrime = dbPart.tx {
        new(FirstPart::class) {
          foo = "new foo"
        }
      }.after
  
      val dbPrime = db.tx {
        new(SecondPart::class) {
          bar = "new bar"
        }
      }.after
  
      val merged = dbPrime.merge(dbPartPrime).after
  
      asOf(merged) {
        assertEquals(setOf("foo", "new foo"), byTrait(FirstPart::class).map { fp -> fp.foo }.toSet())
        assertEquals(setOf("bar", "new bar"), byTrait(SecondPart::class).map { sp -> sp.bar }.toSet())
      }
    }
  */

  /*
    @Test
    fun `cache invalidated after merge`() {
      val db = partitionedDB().tx {
        new(SecondPart::class) {
          bar = "bar"
        }
  
        new(SecondPart::class) {
          bar = "bar2"
        }
      }.after as PartitionedDB
      asOf(db) {
        // we have hit both caches here: lookup will hit lookup-ref cache, .bar will hit entity cache
        assertEquals("bar", lookup(SecondPart::bar, "bar").single().bar)
        assertEquals("bar2", lookup(SecondPart::bar, "bar2").single().bar)
      }
      val db2 = db.select(setOf(SecondPartKey))
      val db2Prime = db2.tx {
        lookup(SecondPart::bar, "bar").single().bar = "updated bar"
        lookup(SecondPart::bar, "bar2").single().delete()
      }.after as PartitionedDB
      val dbPrime = db.merge(db2Prime).after
      asOf(dbPrime) {
        assertEquals(listOf<String>(), lookup(SecondPart::bar, "bar2").map { it.bar })
        assertEquals("updated bar", lookup(SecondPart::bar, "updated bar").single().bar)
      }
    }
  */

  /*
    @Test
    fun `cache is invalidated when merging alternative history of a partition`() {
      val db1 = partitionedDB()
  
      val db2 = db1.tx {
        new(SecondPart::class) {
          bar = "bar"
        }
      }.after as PartitionedDB
  
      asOf(db2) {
        lookup(SecondPart::bar, "bar")
      }
  
      val db1Part = db1.select(setOf(SecondPartKey)).tx {
        new(SecondPart::class) {
          bar = "alternative bar"
        }
      }.after as PartitionedDB
  
      val merged = db2.merge(db1Part).after
      asOf(merged) {
        assertEquals(listOf<String>(), lookup(SecondPart::bar, "bar").map { it.bar })
      }
    }
  */
  
  data class DeletesFromFirstPart(override val eid: EID) : Entity {
    val firstPart: FirstPart by FirstPartAttr

    companion object : EntityType<DeletesFromFirstPart>(DeletesFromFirstPart::class, ::DeletesFromFirstPart) {
      val FirstPartAttr = requiredRef<FirstPart>("firstPart", RefFlags.CASCADE_DELETE)
    }
  }

  data class DeletedBySecondPart(override val eid: EID) : Entity {
    val friend: SecondPart by FriendAttr

    companion object : EntityType<DeletedBySecondPart>(DeletedBySecondPart::class, ::DeletedBySecondPart) {
      val FriendAttr = requiredRef<SecondPart>("friend", RefFlags.CASCADE_DELETE_BY)
    }
  }

  /*
    @Test
    fun `cascade delete happens after merge`() {
      val db = partitionedDB().tx {
        new(DeletesFromFirstPart::class) {
          firstPart = new(FirstPart::class) {
            foo = "foo"
          }
        }
        new(DeletedBySecondPart::class) {
          friend = new(SecondPart::class) {
            bar = "bar"
          }
        }
      }.after as PartitionedDB
      val db1 = db.select(setOf(SecondPartKey)).tx {
        val secondPart = lookup(SecondPart::bar, "bar").single()
        secondPart.delete()
        val deletesFromFirstPart = byTrait(DeletesFromFirstPart::class).single()
        deletesFromFirstPart.delete()
      }.after as PartitionedDB
  
      val merged = db.merge(db1).after
      asOf(merged) {
        assertEquals(listOf<String>(), byTrait(DeletedBySecondPart::class).map { it.friend.bar })
        assertEquals(listOf<String>(), byTrait(FirstPart::class).map { it.foo })
      }
    }
  */
  
  data class ComputedDependsOnSecondPart(override val eid: EID) : Entity {
    val computed: Set<String> by ComputedAttr

    companion object : EntityType<ComputedDependsOnSecondPart>(ComputedDependsOnSecondPart::class, ::ComputedDependsOnSecondPart) {
      val ComputedAttr = requiredValue("computed", SetSerializer(String.serializer()))
    }
  }

  /*
    @Test
    fun `computed attributes dependent on partition recomputed after merge`() {
      val db = partitionedDB().tx {
        new(SecondPart::class) {
          bar = "bar1"
        }
        new(ComputedDependsOnSecondPart::class) {
          addComputed(eid, ComputedDependsOnSecondPart::computed.schema(this@tx))
        }
      }.after as PartitionedDB
  
      asOf(db) {
        assertEquals(setOf("bar1"), byTrait(ComputedDependsOnSecondPart::class).single().computed)
      }
  
      val dbPart = db.select(setOf(SecondPartKey)).tx {
        new(SecondPart::class) {
          bar = "bar2"
        }
      }.after as PartitionedDB
  
      asOf(db.merge(dbPart).after) {
        assertEquals(setOf("bar1", "bar2"), byTrait(ComputedDependsOnSecondPart::class).single().computed)
      }
    }
  */

  /*
    @Test
    fun `pattern index of second part preserved by merge`() {
      val db = partitionedDB()
      val part = db.select(setOf(SecondPartKey)).tx {
        new(ComputedInSecondPart::class) {
          addComputed(eid, ComputedInSecondPart::computed.schema(this@tx))
        }
        new(SecondPart::class) {
          bar = "bar1"
        }
      }.after as PartitionedDB
      val merged = db.merge(part).after
      val mergedPrime = merged.tx {
        new(SecondPart::class) {
          bar = "bar2"
        }
      }.after
      asOf(mergedPrime) {
        assertEquals(setOf("bar1", "bar2"), byTrait(ComputedInSecondPart::class).single().computed)
      }
    }
  */

  /*
    @Test
    fun `schema of a trait is preserved in merged db`() {
      val db = partitionedDB()
      val part = db.select(setOf(SecondPartKey)).tx {
        new(SecondPart::class) {
          bar = "bar1"
        }
      }.after as PartitionedDB
      val merged = db.merge(part).after
      val mergedPrime = merged.tx {
        new(SecondPart::class) {
          bar = "bar2"
        }
      }.after
      asOf(mergedPrime) {
        assertEquals(setOf("bar1", "bar2"), byTrait(SecondPart::class).map { it.bar }.toSet())
      }
    }
  */
}