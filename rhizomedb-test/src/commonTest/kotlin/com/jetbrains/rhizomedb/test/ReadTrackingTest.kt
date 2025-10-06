package com.jetbrains.rhizomedb.test

import com.jetbrains.rhizomedb.*
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertTrue

class CollectingReadTrackingContext: ReadTrackingContext {
  val patterns: MutableList<Pattern> = mutableListOf()

  override fun witness(pattern: Pattern) {
    patterns.add(pattern)
  }
}

class ReadTrackingTest {

  data class Foo(override val eid: EID) : Entity {
    val bar: String by BarAttr

    companion object : EntityType<Foo>(Foo::class, ::Foo) {
      val BarAttr = requiredValue("bar", String.serializer(), Indexing.UNIQUE)
    }
  }

  @Test
  fun `reads of non-registered attrs are tracked`() {
    val rt = CollectingReadTrackingContext()
    val db = EntitiesTest.emptyDb()
    val q = db.withReadTrackingContext(rt)
    asOf(q) {
      entity(Foo.BarAttr, "bar")
    }
    val mutableNovelty = MutableNovelty()
    db.change {
      context.alter(context.impl.collectingNovelty(mutableNovelty::add)) {
        requireChangeScope {
          register(Foo)
          Foo.new {
            it[Foo.BarAttr] = "bar"
          }
        }
      }
    }
    val datoms = mutableNovelty.persistent()
    val patternHashes = rt.patterns.map { p -> p.hash }
    val triggeredPatternHashes = datoms.flatMap { datom -> Pattern.patternHashes(datom.eid, datom.attr, datom.value).toList() }.toSet()
    assertTrue(patternHashes.intersect(triggeredPatternHashes).isNotEmpty())
  }
}