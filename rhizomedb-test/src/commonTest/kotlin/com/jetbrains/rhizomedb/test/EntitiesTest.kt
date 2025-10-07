package com.jetbrains.rhizomedb.test

import com.jetbrains.rhizomedb.*
import com.jetbrains.rhizomedb.impl.entity
import com.jetbrains.rhizomedb.impl.generateSeed
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlin.test.*

class EntitiesTest {

  companion object {
    fun emptyDb(): DB =
      DB.empty()
  }

  data class Person(override val eid: EID) : Entity {
    companion object : EntityType<Person>(Person::class, ::Person) {
      val NameAttr = requiredValue("name", String.serializer())
      val SurnameAttr = optionalValue("surname", String.serializer())
    }

    val name by NameAttr
    val surname by SurnameAttr
  }

  @Serializable
  data class Email(val email: String)

  data class Social(override val eid: EID) : Entity {
    companion object : EntityType<Social>(Social::class, ::Social) {
      val FriendsAttr = manyRef<Social>("friends")
      val EmailsAttr = manyValues("emails", Email.serializer())
    }

    val friends by FriendsAttr
  }

  data class WithIndexedAttr(override val eid: EID) : Entity {
    companion object : EntityType<WithIndexedAttr>(WithIndexedAttr::class, ::WithIndexedAttr) {
      @Indexed
      val IdAttr = requiredValue("id", Int.serializer(), Indexing.INDEXED)
    }

    val id by IdAttr
  }

  data class EntityWithList(override val eid: EID) : Entity {
    companion object : EntityType<EntityWithList>(EntityWithList::class, ::EntityWithList) {
      val ListAttr = requiredValue("list", ListSerializer(Int.serializer()))
    }

    val list by ListAttr
  }

  data class WithRef(override val eid: EID) : Entity {
    companion object : EntityType<WithRef>(WithRef::class, ::WithRef) {
      val RefAttr = optionalRef<WithRef>("ref")
    }

    val ref by RefAttr
  }

  data class ValueBox(override val eid: EID) : Entity {
    companion object : EntityType<ValueBox>(ValueBox::class, ::ValueBox) {
      val ValueAttr = requiredValue("value", String.serializer())
    }

    val value by ValueAttr
  }

  @Test
  fun `transact primitive`() {
    var eid: EID? = null
    var db = emptyDb().change {
      register(ValueBox)
      eid = ValueBox.new {
        it[ValueBox.ValueAttr] = "morty"
      }.eid
    }.dbAfter
    val box = asOf(db) {
      val box = entity(eid!!) as ValueBox
      assertEquals("morty", box.value)
      box
    }

    db = db.change { box[ValueBox.ValueAttr] = "rick" }.dbAfter
    assertEquals("rick", asOf(db) { box.value })
  }

  @Test
  fun `lookup by indexed attr`() {
    val db = emptyDb().change {
      register(WithIndexedAttr)
      WithIndexedAttr.new {
        it[WithIndexedAttr.IdAttr] = 15
      }
    }.dbAfter
    asOf(db) {
      val x = entities(WithIndexedAttr.IdAttr, 15).single()
      assertEquals(15, x.id)
    }
  }

  @Test
  fun `setting null into optional attrs`() {
    var b: WithRef? = null
    val db = emptyDb().change {
      register(WithRef)
      val a = WithRef.new {}
      b = WithRef.new {
        it[WithRef.RefAttr] = a
      }
    }.dbAfter

    val db1 = db.change {
      b!![WithRef.RefAttr] = null
    }.dbAfter
    assertNull(asOf(db1) { b!!.ref })
  }

  @Test
  fun `lookup many by indexed attr`() {
    val db = emptyDb().change {
      register(WithIndexedAttr)
      WithIndexedAttr.new {
        it[WithIndexedAttr.IdAttr] = 15
      }
      WithIndexedAttr.new {
        it[WithIndexedAttr.IdAttr] = 15
      }
    }.dbAfter
    asOf(db) {
      val xs = entities(WithIndexedAttr.IdAttr, 15)
      assertTrue(xs.all { x -> x.id == 15 })
    }
  }

  @Test
  fun `entity with list`() {
    var eid: EID? = null
    val db = emptyDb().change {
      register(EntityWithList)
      eid = EntityWithList.new {
        it[EntityWithList.ListAttr] = listOf(13)
      }.eid
    }.dbAfter
    asOf(db) {
      assertEquals(13, (entity(eid!!) as EntityWithList).list.first())
    }
  }


  @Test
  fun `transaction without required fields should fail`() {
    assertFailsWith<TxValidationException> {
      emptyDb().change {
        register(ValueBox)
        ValueBox.new {}
      }
    }
  }

  @Test
  fun `retract of required attribute should fail`() {
    emptyDb().change {
      register(ValueBox)
      val v = ValueBox.new { it[ValueBox.ValueAttr] = "value" }
      assertFailsWith<TxValidationException> {
        mutate(RetractAttribute(eid = v.eid,
                                attribute = ValueBox.ValueAttr.attr,
                                seed = generateSeed()))
      }
    }
  }

  @Test
  fun `access field of non-existing entity`() {
    emptyDb().change {
      register(Person)
      val v = Person.new {
        it[Person.NameAttr] = "A"
      }
      v.delete()
      assertFailsWith<EntityDoesNotExistException> {
        v.name
      }
      assertFailsWith<EntityDoesNotExistException> {
        v.surname
      }
      assertFailsWith<EntityDoesNotExistException> {
        v[Person.NameAttr] = "new name"
      }
      assertFailsWith<EntityDoesNotExistException> {
        v[Person.SurnameAttr] = "new surname"
      }
    }
  }

  @Test
  fun `leak entity from transaction`() {
    var box: ValueBox? = null
    val db = emptyDb().change {
      register(ValueBox)
      box = ValueBox.new {
        it[ValueBox.ValueAttr] = "morty"
      }
    }.dbAfter
    assertEquals("morty", asOf(db) { box?.value })
  }


  @Test
  fun `referring to removed entity with many attribute`() {
    var refEntity: Social? = null
    var relatedEntity1: Social?
    var relatedEntity2: Social? = null
    var relatedEntity3: Social? = null
    var relatedEntity4: Social?
    val db = emptyDb().change {
      register(Social)
      relatedEntity1 = Social.new {}
      relatedEntity2 = Social.new {}
      relatedEntity3 = Social.new {}
      relatedEntity4 = Social.new {}
      refEntity = Social.new {
        it[Social.FriendsAttr] = setOf(relatedEntity1, relatedEntity2, relatedEntity3, relatedEntity4)
      }
    }.dbAfter

    val db1 = db.change { relatedEntity3!!.delete() }.dbAfter
    asOf(db1) {
      val remainingFriends = refEntity!!.friends
      assertEquals(3, remainingFriends.size)
      assertTrue(remainingFriends.contains(relatedEntity2))
    }
  }

  @Test
  fun `read on free entity outside of tx should fail`() {
    assertFailsWith<OutOfDbContext> {
      DbContext.clearThreadBoundDbContext()
      var box: ValueBox? = null
      emptyDb().change {
        register(ValueBox)
        box = ValueBox.new {
          it[ValueBox.ValueAttr] = "morty"
        }
      }
      require(DbContext.threadBoundOrNull == null) {
        "db context is not supposed to be bound here"
      }
      box?.value
    }
  }

  @Test
  fun `update primitive`() {
    var eid: EID? = null
    var db = emptyDb().change {
      register(ValueBox)
      eid = ValueBox.new {
        it[ValueBox.ValueAttr] = "morty"
      }.eid
    }.dbAfter
    val box = asOf(db) { entity(eid!!) as ValueBox }
    db = db.change {
      box[ValueBox.ValueAttr] = "rick"
    }.dbAfter
    asOf(db) {
      assertEquals("rick", box.value)
    }
  }

  @Test
  fun `read value updated in same tx`() {
    var box: ValueBox? = null
    val db = emptyDb().change {
      register(ValueBox)
      box = ValueBox.new {
        it[ValueBox.ValueAttr] = "morty"
      }
      assertEquals("morty", box.value)
      box[ValueBox.ValueAttr] = "rick"
      assertEquals("rick", box.value)
    }.dbAfter
    assertEquals("rick", asOf(db) { box?.value })
  }

  data class AssortedValues(override val eid: EID) : Entity {
    companion object : EntityType<AssortedValues>(AssortedValues::class, ::AssortedValues) {
      val LongAttr = requiredValue("long", Long.serializer())
      val IntAttr = requiredValue("int", Int.serializer())
      val ShortAttr = requiredValue("short", Short.serializer())
      val CharAttr = requiredValue("char", Char.serializer())
      val ByteAttr = requiredValue("byte", Byte.serializer())
      val LongsAttr = requiredTransient<LongArray>("longs")
      val BytesAttr = requiredTransient<ByteArray>("bytes")
      val ObjAttr = requiredTransient<Any>("obj")
    }

    val long: Long by LongAttr
    val int: Int by IntAttr
    val short: Short by ShortAttr
    val char: Char by CharAttr
    val byte: Byte by ByteAttr
    val longs: LongArray by LongsAttr
    val bytes: ByteArray by BytesAttr
    val obj: Any by ObjAttr
  }

  @Test
  fun `primitive data types`() {
    var eid: EID? = null
    val theObject = Any()
    val db = emptyDb().change {
      register(AssortedValues)
      eid = AssortedValues.new {
        it[AssortedValues.LongAttr] = 1
        it[AssortedValues.IntAttr] = 2
        it[AssortedValues.ShortAttr] = 3
        it[AssortedValues.CharAttr] = 4.toChar()
        it[AssortedValues.ByteAttr] = 5
        it[AssortedValues.LongsAttr] = longArrayOf(1, 2, 3)
        it[AssortedValues.BytesAttr] = byteArrayOf(4, 5, 6)
        it[AssortedValues.ObjAttr] = theObject
      }.eid
    }.dbAfter
    asOf(db) {
      val c = (entity(eid!!) as AssortedValues?)!!
      assertEquals(1.toLong(), c.long)
      assertEquals(2, c.int)
      assertEquals(3.toShort(), c.short)
      assertEquals(4.toChar(), c.char)
      assertEquals(5.toByte(), c.byte)
      assertContentEquals(longArrayOf(1, 2, 3), c.longs)
      assertContentEquals(byteArrayOf(4, 5, 6), c.bytes)
      assertEquals(theObject, c.obj)
    }
  }

  data class OptionalPrimitive(override val eid: EID) : Entity {
    companion object : EntityType<OptionalPrimitive>(OptionalPrimitive::class, ::OptionalPrimitive) {
      val LongAttr = optionalValue("long", Long.serializer())
    }

    val long: Long? by LongAttr
  }

  @Test
  fun `optional primitive`() {
    var box: OptionalPrimitive? = null
    var db = emptyDb().change {
      register(OptionalPrimitive)
      box = OptionalPrimitive.new { }
    }.dbAfter
    assertEquals(null, asOf(db) { box!!.long })

    db = db.change { box!![OptionalPrimitive.LongAttr] = 42L }.dbAfter
    assertEquals(42, asOf(db) { box!!.long })

    db = db.change { box!![OptionalPrimitive.LongAttr] = null }.dbAfter
    assertEquals(null, asOf(db) { box!!.long })
  }

  data class Foo(override val eid: EID) : Entity {
    companion object : EntityType<Foo>(Foo::class, ::Foo) {
      val FooAttr = requiredValue("foo", String.serializer())
    }

    val foo: String by FooAttr
  }

  data class Bar(override val eid: EID) : Entity {
    companion object : EntityType<Bar>(Bar::class, ::Bar) {
      val BarAttr = requiredValue("bar", String.serializer())
    }

    val bar: String by BarAttr
  }

  @Test
  fun `free entities should compared by eid only`() {
    var b1: ValueBox? = null
    var b2: ValueBox? = null

    val db = emptyDb().change {
      register(ValueBox)
      b1 = ValueBox.new {
        it[ValueBox.ValueAttr] = "foo"
      }
      b2 = ValueBox.new {
        it[ValueBox.ValueAttr] = "foo"
      }
    }.dbAfter
    val b1Free = asOf(db) { entity(b1!!.eid) }
    assertEquals(b1Free, b1)
    assertTrue(b1Free != b2)
    assertTrue(b1 != b2)
  }

  data class ManyValuesBox(override val eid: EID) : Entity {
    val values: Set<String> by ValuesAttr

    companion object : EntityType<ManyValuesBox>(ManyValuesBox::class, ::ManyValuesBox) {
      val ValuesAttr = manyValues("values", String.serializer())
    }
  }

  @Test
  fun `transact cardinality many values`() {
    var box: ManyValuesBox? = null
    var db = emptyDb().change {
      register(ManyValuesBox)
      box = ManyValuesBox.new {
        it[ManyValuesBox.ValuesAttr] = setOf("1", "2", "3")
      }
      assertEquals(setOf("1", "2", "3"), box.values)
      box.add(ManyValuesBox.ValuesAttr, "4")
      assertEquals(setOf("1", "2", "3", "4"), box.values)
    }.dbAfter
    asOf(db) {
      assertEquals(setOf("1", "2", "3", "4"), box!!.values)
      assertEquals(4, ManyValuesBox.ValuesAttr.all().size)
    }

    db = db.change {
      box!!.remove(ManyValuesBox.ValuesAttr, "4")
      box.add(ManyValuesBox.ValuesAttr, "5")
      assertEquals(setOf("1", "2", "3", "5"), box.values)
    }.dbAfter
    asOf(db) {
      assertEquals(setOf("1", "2", "3", "5"), box!!.values)
    }

    db = db.change {
      box!![ManyValuesBox.ValuesAttr] = setOf("6", "7")
      assertEquals(hashSetOf("6", "7"), box.values)
    }.dbAfter
    asOf(db) {
      assertEquals(hashSetOf("6", "7"), box!!.values)
    }
  }

  @Test
  fun `iterate Many values`() {
    emptyDb().change {
      register(ManyValuesBox)
      val box = ManyValuesBox.new {
        it[ManyValuesBox.ValuesAttr] = setOf("1", "2", "3")
      }
      val iter = box.values.iterator()
      while (iter.hasNext()) {
        val n = iter.next()
        if (n == "2") {
          box.remove(ManyValuesBox.ValuesAttr, n)
        }
      }
      assertEquals(hashSetOf("1", "3"), box.values.toSet())
    }
  }

  data class Relation(override val eid: EID) : Entity {
    val marker: String by MarkerAttr
    val relation: Relation? by RelationAttr

    companion object : EntityType<Relation>(Relation::class, ::Relation) {
      val MarkerAttr = requiredValue("marker", String.serializer())
      val RelationAttr = optionalRef<Relation>("relation")
    }
  }

  @Test
  fun `transact relation`() {
    var foo: Relation? = null
    val db = emptyDb().change {
      register(Relation)
      val bar = Relation.new {
        it[Relation.MarkerAttr] = "bar"
        it[Relation.RelationAttr] = Relation.new { nested ->
          nested[Relation.MarkerAttr] = "buzz"
        }
      }
      assertEquals("buzz", bar.relation?.marker)
      foo = Relation.new {
        it[Relation.MarkerAttr] = "foo"
        it[Relation.RelationAttr] = bar
      }
      assertEquals("bar", foo.relation?.marker)
      assertEquals("buzz", foo.relation?.relation?.marker)
    }.dbAfter
    asOf(db) {
      assertEquals("bar", foo!!.relation?.marker)
      assertEquals("buzz", foo.relation?.relation?.marker)
    }
  }

  @Test
  fun `optional relation`() {
    var box: Relation? = null
    var db = emptyDb().change {
      register(Relation)
      box = Relation.new {
        it[Relation.MarkerAttr] = "shmarker"
      }
    }.dbAfter
    assertEquals(null, asOf(db) { box!!.relation })

    var relEid: EID? = null
    db = db.change {
      val e = Relation.new {
        it[Relation.MarkerAttr] = "shmarker"
      }
      relEid = e.eid
      box!![Relation.RelationAttr] = e
    }.dbAfter
    assertEquals(relEid!!, asOf(db) { box!!.relation?.eid })

    db = db.change {
      box!![Relation.RelationAttr] = null
    }.dbAfter
    assertEquals(null, asOf(db) { box!!.relation })
  }

  @Test
  fun `set attrs with cardinality many relation`() {
    var s1: Social? = null
    var s2: Social? = null
    val db = emptyDb().change {
      register(Social)
      s1 = Social.new {}
      s2 = Social.new {}
      s1[Social.FriendsAttr] = setOf(s1, s2)
      s2[Social.FriendsAttr] = setOf(s1, s2)
    }.dbAfter
    val db1 = db.change {
      s1!![Social.FriendsAttr] = setOf(s2!!)
      s2[Social.FriendsAttr] = setOf(s1)
    }.dbAfter
    assertEquals(s2!!.eid, asOf(db1) { s1!!.friends.single().eid })
    assertEquals(s1!!.eid, asOf(db1) { s2.friends.single().eid })
  }

  data class NullableBooleanBox(override val eid: EID) : Entity {
    val value: Boolean? by ValueAttr

    companion object : EntityType<NullableBooleanBox>(NullableBooleanBox::class, ::NullableBooleanBox) {
      val ValueAttr = optionalValue("value", Boolean.serializer())
    }
  }

  @Test
  fun `lost boolean bug`() {
    var box: NullableBooleanBox? = null
    val db = emptyDb().change {
      register(NullableBooleanBox)
      box = NullableBooleanBox.new {
        it[NullableBooleanBox.ValueAttr] = false
      }
    }.dbAfter
    asOf(db) {
      assertEquals(false, box!!.value)
    }
  }

  @Test
  fun `will receive null if entity does not have requested entity`() {
    var eid: EID? = null
    val db = emptyDb().change {
      register(Foo)
      eid = Foo.new {
        it[Foo.FooAttr] = "foo"
      }.eid
      assertNull(entity(eid) as? Bar)
    }.dbAfter
    asOf(db) {
      assertNull(entity(eid!!) as? Bar)
    }
  }

  @Test
  fun `query all datoms`() {
    val db = emptyDb().change {
      register(Foo)
      Foo.new {
        it[Foo.FooAttr] = "foo"
      }
      Foo.new {
        it[Foo.FooAttr] = "bar"
      }
    }.dbAfter
    val foos = db.queryIndex(IndexQuery.All())
      .filter { datom -> datom.attr == Foo.FooAttr.attr }
      .map { datom -> datom.value }
      .toCollection(HashSet())
    assertEquals(hashSetOf<Any>("foo", "bar"), foos)
  }

  @Test
  fun `lookup an entity stored in collection`() {
    emptyDb().change {
      register(Social)
      val e1 = Social.new {
      }

      val e2 = Social.new {
        it[Social.FriendsAttr] = setOf(e1)
      }

      assertEquals(e1.eid, e2.friends.single().eid)
      assertEquals(e2.eid, entities(Social.FriendsAttr, e1).single().eid)
    }
  }

  @Test
  fun `retract reference attributes`() {
    emptyDb().change {
      register(WithRef)
      val toDelete = WithRef.new()
      WithRef.new {
        it[WithRef.RefAttr] = toDelete
      }
      toDelete.delete()
      assertEquals(listOf(), WithRef.RefAttr.all().toList())
    }
  }

  data class CascadeDeleteRef(override val eid: EID) : Entity {
    val ref: CascadeDeleteRef? by RefAttr

    companion object : EntityType<CascadeDeleteRef>(CascadeDeleteRef::class, ::CascadeDeleteRef) {
      val RefAttr = optionalRef<CascadeDeleteRef>("ref", RefFlags.CASCADE_DELETE)
    }
  }

  @Test
  fun `cascade delete`() {
    emptyDb().change {
      register(CascadeDeleteRef)
      val toDelete = CascadeDeleteRef.new {
        it[CascadeDeleteRef.RefAttr] = CascadeDeleteRef.new {
          it[CascadeDeleteRef.RefAttr] = CascadeDeleteRef.new {
            it[CascadeDeleteRef.RefAttr] = CascadeDeleteRef.new {}
          }
        }
      }

      toDelete.delete()
      assertEquals(emptySet(), CascadeDeleteRef.RefAttr.all())
    }
  }

  data class CascadeDeleteByParent(override val eid: EID) : Entity {
    val parent: CascadeDeleteByParent? by ParentAttr

    companion object : EntityType<CascadeDeleteByParent>(CascadeDeleteByParent::class, ::CascadeDeleteByParent) {
      val ParentAttr = optionalRef<CascadeDeleteByParent>("parent", RefFlags.CASCADE_DELETE_BY)
    }
  }

  @Test
  fun `cascade delete by`() {
    emptyDb().change {
      register(CascadeDeleteByParent)
      val toDelete = CascadeDeleteByParent.new {}
      CascadeDeleteByParent.new {
        it[CascadeDeleteByParent.ParentAttr] = CascadeDeleteByParent.new {
          it[CascadeDeleteByParent.ParentAttr] = CascadeDeleteByParent.new {
            it[CascadeDeleteByParent.ParentAttr] = toDelete
          }
        }
      }

      toDelete.delete()
      assertEquals(emptySet(), CascadeDeleteByParent.ParentAttr.all())
    }
  }

  @Test
  fun `cascade delete by cyclic`() {
    emptyDb().change {
      register(CascadeDeleteByParent)
      val toDelete = CascadeDeleteByParent.new {}
      val leaf = CascadeDeleteByParent.new {
        it[CascadeDeleteByParent.ParentAttr] = CascadeDeleteByParent.new {
          it[CascadeDeleteByParent.ParentAttr] = CascadeDeleteByParent.new {
            it[CascadeDeleteByParent.ParentAttr] = toDelete
          }
        }
      }
      toDelete[CascadeDeleteByParent.ParentAttr] = leaf

      toDelete.delete()
      assertEquals(emptySet(), CascadeDeleteByParent.ParentAttr.all())
    }
  }

  data class EntityWithRequiredRef(override val eid: EID) : Entity {
    companion object : EntityType<EntityWithRequiredRef>(EntityWithRequiredRef::class, ::EntityWithRequiredRef) {
      val RefAttr = requiredRef<Entity>("ref")
    }

    val ref: Entity by RefAttr
  }

  @Test
  fun `required attribute is treated as CascadeDeleteBy`() {
    emptyDb().change {
      register(Foo)
      register(EntityWithRequiredRef)
      val toDelete = Foo.new {
        it[Foo.FooAttr] = "foo"
      }
      val referer = EntityWithRequiredRef.new {
        it[EntityWithRequiredRef.RefAttr] = toDelete
      }

      toDelete.delete()
      assertFalse(referer.exists())
    }
  }

  interface NonEntityWithProperty {
    val foo: String
    val bar: String
  }

  data class OverridesNonEntityWithProperty(override val eid: EID) : NonEntityWithProperty, Entity {
    override val foo: String by FooAttr
    override val bar: String get() = "bar"

    companion object : EntityType<OverridesNonEntityWithProperty>(OverridesNonEntityWithProperty::class, ::OverridesNonEntityWithProperty) {
      val FooAttr = requiredValue("foo", String.serializer())
    }
  }

  @Test
  fun `entity overrides property from non-entity interface`() {
    emptyDb().change {
      register(OverridesNonEntityWithProperty)
      val x = OverridesNonEntityWithProperty.new {
        it[OverridesNonEntityWithProperty.FooAttr] = "foo"
      }
      assertEquals("foo", x.foo)
      assertEquals("bar", x.bar)
    }
  }

  data class HasUniqueField(override val eid: EID) : Entity {
    val unique: String by UniqueAttr

    companion object : EntityType<HasUniqueField>(HasUniqueField::class, ::HasUniqueField) {
      val UniqueAttr = requiredValue("unique", String.serializer(), Indexing.UNIQUE)
    }
  }

  @Test
  fun `cannot duplicate values for properties marked as unique`() {
    emptyDb().change {
      context.alter(context.impl.enforcingUniquenessConstraints(AllParts)) {
        register(HasUniqueField)
        HasUniqueField.new {
          it[HasUniqueField.UniqueAttr] = "pretty unique"
        }
        assertFailsWith<Throwable> {
          HasUniqueField.new {
            it[HasUniqueField.UniqueAttr] = "pretty unique"
          }
        }
      }
    }
  }

  @Test
  fun `can transact exactly the same datom to unique attribute`() {
    emptyDb().change {
      register(HasUniqueField)
      val foo = HasUniqueField.new {
        it[HasUniqueField.UniqueAttr] = "pretty unique"
      }
      foo[HasUniqueField.UniqueAttr] = "pretty unique"
    }
  }

  @Test
  @Ignore
  fun `diamond property definition`() {
    /*interface Foo {
      val foo: String
    }

    interface JustTrait: Trait {
      var foo: String
    }

    interface FooTrait: Foo, JustTrait {
      override var foo: String
    }*/
    TODO()
  }

  data class TestFoo(override val eid: EID) : RetractableEntity, Entity {
    val result: ArrayList<String> by ResultAttr

    override fun onRetract(): RetractableEntity.Callback {
      result.add("Foo")
      return RetractableEntity.Callback {}
    }

    companion object : EntityType<TestFoo>(TestFoo::class, ::TestFoo) {
      val TestBarAttr = requiredRef<TestBar>("testBar", RefFlags.CASCADE_DELETE_BY)
      val ResultAttr = requiredTransient<ArrayList<String>>("result")
    }
  }

  data class TestBar(override val eid: EID) : RetractableEntity, Entity {
    val result: ArrayList<String> by ResultAttr

    override fun onRetract(): RetractableEntity.Callback {
      result.add("Bar")
      return RetractableEntity.Callback {}
    }

    companion object : EntityType<TestBar>(TestBar::class, ::TestBar) {
      val TestBazAttr = requiredRef<TestBaz>("testBaz", RefFlags.CASCADE_DELETE)
      val ResultAttr = requiredTransient<ArrayList<String>>("result")
    }
  }

  data class TestBaz(override val eid: EID) : RetractableEntity, Entity {
    val result: ArrayList<String> by ResultAttr

    override fun onRetract(): RetractableEntity.Callback {
      result.add("Baz")
      return RetractableEntity.Callback { }
    }

    companion object : EntityType<TestBaz>(TestBaz::class, ::TestBaz) {
      val ResultAttr = requiredTransient<ArrayList<String>>("result")
    }
  }

  @Test
  fun `disposable entities`() {
    val arrayList = ArrayList<String>()
    emptyDb().change {
      register(TestFoo)
      register(TestBar)
      register(TestBaz)
      val baz = TestBaz.new {
        it[TestBaz.ResultAttr] = arrayList
      }
      val bar = TestBar.new {
        it[TestBar.ResultAttr] = arrayList
        it[TestBar.TestBazAttr] = baz
      }
      TestFoo.new {
        it[TestFoo.ResultAttr] = arrayList
        it[TestFoo.TestBarAttr] = bar
      }
      TestBar.all().first().delete()
    }
    assertEquals(setOf("Bar", "Baz", "Foo"), arrayList.toSet())
  }

  data class LispList(override val eid: EID) : Entity {
    val cdr: LispList? by CdrAttr

    companion object : EntityType<LispList>(LispList::class, ::LispList) {
      val CarAttr = requiredValue("car", Int.serializer())
      val CdrAttr = optionalRef<LispList>("cdr", RefFlags.CASCADE_DELETE)
    }
  }

  @Test
  fun `no stack overflow on cascade delete`() {
    emptyDb().change {
      register(LispList)
      val list = LispList.new {
        it[LispList.CarAttr] = 0
      }
      var l: LispList? = list
      for (i in 1..1_000) {
        l!![LispList.CdrAttr] = LispList.new {
          it[LispList.CarAttr] = i
        }
        l = l.cdr
      }
      list.delete()
    }
  }

  data class RetractableParent(override val eid: EID) : RetractableEntity, Entity {
    val child: RetractableChild? by ChildAttr

    override fun onRetract(): RetractableEntity.Callback {
      val c = child
      return RetractableEntity.Callback {
        c!!.delete()
      }
    }

    companion object : EntityType<RetractableParent>(RetractableParent::class, ::RetractableParent) {
      val ChildAttr = optionalRef<RetractableChild>("child")
    }
  }

  data class RetractableChild(override val eid: EID) : RetractableEntity, Entity {
    val parent: RetractableParent? by ParentAttr

    override fun onRetract(): RetractableEntity.Callback {
      val p = parent
      return RetractableEntity.Callback {
        if (p != null) {
          p.delete()
        }
      }
    }

    companion object : EntityType<RetractableChild>(RetractableChild::class, ::RetractableChild) {
      val ParentAttr = optionalRef<RetractableParent>("parent")
    }
  }

  @Test
  fun `stack overflow on recursive retract`() {
    emptyDb().change {
      register(RetractableParent)
      register(RetractableChild)
      val parent = RetractableParent.new {}
      val child = RetractableChild.new {
        it[RetractableChild.ParentAttr] = parent
      }
      parent[RetractableParent.ChildAttr] = child
      parent.delete()
    }
  }

  data class Root(override val eid: EID) : Entity {
    companion object : EntityType<Root>(Root::class, ::Root)
  }

  data class CascadeDeleteByRootA(override val eid: EID) : Entity {
    val root: Root by RootAttr

    companion object : EntityType<CascadeDeleteByRootA>(CascadeDeleteByRootA::class, ::CascadeDeleteByRootA) {
      val RootAttr = requiredRef<Root>("root", RefFlags.CASCADE_DELETE_BY)
    }
  }

  data class CascadeDeleteByRootB(override val eid: EID) : Entity {
    val root: Root by RootAttr
    val a: CascadeDeleteByRootA by AAttr

    companion object : EntityType<CascadeDeleteByRootB>(CascadeDeleteByRootB::class, ::CascadeDeleteByRootB) {
      val RootAttr = requiredRef<Root>("root", RefFlags.CASCADE_DELETE_BY)
      val AAttr = requiredRef<CascadeDeleteByRootA>("a")
    }
  }

  @Test
  fun `two CascadeDeletedBy entities can reference each other`() {
    emptyDb().change {
      register(Root)
      register(CascadeDeleteByRootA)
      register(CascadeDeleteByRootB)
      val root = Root.new {}
      val a = CascadeDeleteByRootA.new {
        it[CascadeDeleteByRootA.RootAttr] = root
      }
      CascadeDeleteByRootB.new {
        it[CascadeDeleteByRootB.RootAttr] = root
        it[CascadeDeleteByRootB.AAttr] = a
      }
      root.delete()
    }
  }

  data class EntityWithChild(override val eid: EID) : Entity {
    companion object : EntityType<EntityWithChild>(EntityWithChild::class, ::EntityWithChild) {
      val ChildAttr = requiredRef<Entity>("child")
    }
  }

  @Test
  fun `refs are updated correctly after displacement`() {
    emptyDb().change {
      register(Root)
      register(EntityWithChild)
      val a = Root.new {}
      val b = Root.new {}
      val c = EntityWithChild.new {
        it[EntityWithChild.ChildAttr] = a
      }
      c[EntityWithChild.ChildAttr] = b
      assertEquals(emptyList(), context.queryIndex(IndexQuery.RefsTo(a.eid)))
    }
  }

  @Test
  fun `establishing relation on deleted entity is forbidden`() {
    // creation of an entity and setting an attribute are two different ways to establish a relation 
    emptyDb().change {
      register(Relation)
      val a = Relation.new {
        it[Relation.MarkerAttr] = "a"
      }
      a.delete()
      assertFailsWith<EntityDoesNotExistException> {
        Relation.new {
          it[Relation.MarkerAttr] = "b"
          it[Relation.RelationAttr] = a
        }
      }
    }
    emptyDb().change {
      register(Relation)
      val a = Relation.new {
        it[Relation.MarkerAttr] = "a"
      }
      val b = Relation.new {
        it[Relation.MarkerAttr] = "b"
      }
      a.delete()
      assertFailsWith<EntityDoesNotExistException> {
        b[Relation.RelationAttr] = a
      }
    }
  }
}