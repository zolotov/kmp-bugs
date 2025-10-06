package com.jetbrains.rhizomedb.test

import com.jetbrains.rhizomedb.*
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntityTypeTest {
  data class EntityToCreate(override val eid: EID) : Entity {
    companion object : EntityType<EntityToCreate>(EntityToCreate::class, ::EntityToCreate) {
      val RequiredInt = requiredValue("requiredInt", Int.serializer())
      val OptionalInt = optionalValue("optionalInt", Int.serializer())
      val ManyInt = manyValues("manyInt", Int.serializer())
      val OptionalRef = optionalRef<EntityToCreate>("ref")

      fun new(
        changeScope: ChangeScope,
        requiredInt: Int,
        optionalInt: Int? = null,
        manyInts: List<Int> = emptyList(),
        ref: EntityToCreate? = null
      ) = with(changeScope) {
        new { e ->
          e[RequiredInt] = requiredInt
          optionalInt?.let { e[OptionalInt] = it }
          e[ManyInt] = manyInts.toSet()
          ref?.let { e[OptionalRef] = it }
        }
      }
    }

    val requiredInt by RequiredInt
    val optionalInt by OptionalInt
    val manyInt by ManyInt
    val optionalRef by OptionalRef
  }

  @Test
  fun `create entity`() {
    DB.empty().change {
      register(EntityToCreate)
      val e = EntityToCreate.new(
        this,
        requiredInt = 1,
        optionalInt = 2,
        manyInts = listOf(3, 4, 5),
        ref = EntityToCreate.new {
          it[EntityToCreate.RequiredInt] = 6
        }
      )
      assertEquals(2, e[EntityToCreate.OptionalInt])
      assertEquals(setOf(3, 4, 5), e.manyInt)
      assertEquals(1, e.requiredInt)
      val ref = e.optionalRef!!
      assertEquals(6, ref.requiredInt)
      assertEquals(e, entities(EntityToCreate.OptionalRef, ref).single())
    }
  }

  interface TestMixin : Entity {
    companion object : Mixin<TestMixin>(TestMixin::class) {
      val Attr = requiredValue("attr", Int.serializer())
      val AttrWithDefault = requiredValue("attrWithDefault", Int.serializer()) { 42 }
    }
  }

  data class EntityWithMixin(override val eid: EID) : TestMixin {
    companion object : EntityType<EntityWithMixin>(EntityWithMixin::class, ::EntityWithMixin, TestMixin) {
      val Attr = requiredValue("attr", Int.serializer())

      fun new(
        changeScope: ChangeScope,
        mixinAttr: Int,
        attr: Int,
      ) = with(changeScope) {
        new {
          it[Attr] = attr
          it[TestMixin.Attr] = mixinAttr
        }
      }
    }
  }

  @Test
  fun mixin() {
    DB.empty().change {
      register(EntityWithMixin)
      val e = EntityWithMixin.new(this, attr = 2, mixinAttr = 1)
      assertEquals(1, e[TestMixin.Attr])
      assertEquals(2, e[EntityWithMixin.Attr])
      assertEquals(42, e[TestMixin.AttrWithDefault])
      assertEquals(setOf(e to 1), TestMixin.Attr.all())
      e.delete()
      assertTrue(TestMixin.Attr.all().isEmpty())
    }
  }
}
