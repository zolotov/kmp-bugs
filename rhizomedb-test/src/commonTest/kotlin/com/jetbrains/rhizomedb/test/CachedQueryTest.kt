package com.jetbrains.rhizomedb.test

import com.jetbrains.rhizomedb.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CachedQueryTest {
  data class FooQ(val foo: EntitiesTest.Foo): CachedQuery<String> {
    override fun DbContext<Q>.query(): String {
      return foo.foo
    }
  }

  @Test
  fun `query is actually cached`() {
    EntitiesTest.emptyDb().change {
      register(EntitiesTest.Foo)
      val f = EntitiesTest.Foo.new {
        it[EntitiesTest.Foo.FooAttr] = "hello"
      }
      assertEquals("hello", cachedQuery(FooQ(f)))
      context.alter(object: Q by context.impl {
        override fun <T> queryIndex(indexQuery: IndexQuery<T>): T {
          throw AssertionError("cached query tries to access index")
        }
      }) {
        assertEquals("hello", cachedQuery(FooQ(f)))
      }
    }
  }

  @Test
  fun `cached query is invalidated`() {
    EntitiesTest.emptyDb().change {
      register(EntitiesTest.Foo)
      val f = EntitiesTest.Foo.new {
        it[EntitiesTest.Foo.FooAttr] = "hello"
      }
      assertEquals("hello", cachedQuery(FooQ(f)))
      f[EntitiesTest.Foo.FooAttr] = "bye"
      assertEquals("bye", cachedQuery(FooQ(f)))
    }
  }

  @Test
  fun `cached query is consistent with db`() {
    var f: EntitiesTest.Foo? = null
    val db1 = EntitiesTest.emptyDb().change {
      register(EntitiesTest.Foo)
      f = EntitiesTest.Foo.new {
        it[EntitiesTest.Foo.FooAttr] = "hello"
      }
      assertEquals("hello", cachedQuery(FooQ(f)))
    }.dbAfter

    val db2 = db1.change {
      f!![EntitiesTest.Foo.FooAttr] = "bye"
    }.dbAfter

    asOf(db2) {
      assertEquals("bye", cachedQuery(FooQ(f!!)))
    }

    asOf(db1) {
      assertEquals("hello", cachedQuery(FooQ(f!!)))
    }
  }

  @Test
  fun `cached query is reactive`() {
    var f: EntitiesTest.Foo? = null

    val db = EntitiesTest.emptyDb().change {
      register(EntitiesTest.Foo)
      f = EntitiesTest.Foo.new {
        it[EntitiesTest.Foo.FooAttr] = "hello"
      }
      assertEquals("hello", cachedQuery(FooQ(f)))
    }.dbAfter

    val ps = ArrayList<Pattern>()
    asOf(db.withReadTrackingContext { p -> ps.add(p) }) {
      cachedQuery(FooQ(f!!))
    }
    println(ps)
    assertTrue(ps.isNotEmpty(), "cached query didn't emit patterns")
  }
}