package com.jetbrains.rhizomedb.test.rql

import com.jetbrains.rhizomedb.EID
import com.jetbrains.rhizomedb.Entity
import com.jetbrains.rhizomedb.EntityType
import com.jetbrains.rhizomedb.change
import com.jetbrains.rhizomedb.rql.executeQuery
import com.jetbrains.rhizomedb.rql.pretty
import com.jetbrains.rhizomedb.test.EntitiesTest.Companion.emptyDb
import kotlinx.serialization.builtins.serializer
import kotlin.test.Test

data class SimpleEntity(override val eid: EID) : Entity {
  val value: String by ValueAttr
  val other: Int by OtherAttr

  companion object : EntityType<SimpleEntity>(SimpleEntity::class, ::SimpleEntity) {
    val ValueAttr = requiredValue("value", String.serializer())
    val OtherAttr = requiredValue("other", Int.serializer())
  }
}

data class DocumentEntity(override val eid: EID) : Entity {
  val name: String by NameAttr

  companion object : EntityType<DocumentEntity>(DocumentEntity::class, ::DocumentEntity) {
    val NameAttr = requiredValue("name", String.serializer())
  }
}

data class DaemonEntity(override val eid: EID) : Entity {
  val document: DocumentEntity by DocumentAttr
  val backend: String by BackendAttr

  companion object : EntityType<DaemonEntity>(DaemonEntity::class, ::DaemonEntity) {
    val DocumentAttr = requiredRef<DocumentEntity>("document")
    val BackendAttr = requiredValue("backend", String.serializer())
  }
}

class RqlTest {

  @Test
  fun queryTest() {
    val db = emptyDb().change {
      register(SimpleEntity)
      SimpleEntity.new { it[SimpleEntity.ValueAttr] = "One"; it[SimpleEntity.OtherAttr] = 100 }
      SimpleEntity.new { it[SimpleEntity.ValueAttr] = "Two"; it[SimpleEntity.OtherAttr] = 200 }
      SimpleEntity.new { it[SimpleEntity.ValueAttr] = "Three"; it[SimpleEntity.OtherAttr] = 300 }
    }.dbAfter
    val result1 = executeQuery(db, "SELECT value, other FROM SimpleEntity")
    println(result1.pretty())
    val result2 = executeQuery(db, "SELECT * FROM SimpleEntity")
    println(result2.pretty())
    val result3 = executeQuery(db, "SELECT other AS Number FROM SimpleEntity")
    println(result3.pretty())
    val result4 = executeQuery(db, "SELECT other % 10 = 0 AS ratio FROM SimpleEntity")
    println(result4.pretty())
    val result5 = executeQuery(db, "SELECT other % 10 = 0 FROM SimpleEntity")
    println(result5.pretty())
    val result6 = executeQuery(db, "SELECT NULL, TRUE, FALSE FROM SimpleEntity")
    println(result6.pretty())
    val result7 = executeQuery(db, "SELECT value FROM SimpleEntity WHERE other >= 200")
    println(result7.pretty())
    val result8 = executeQuery(db, "SELECT 100 + 100 > 0")
    println(result8.pretty())
    val result9 = executeQuery(db, "SELECT upper(value) FROM SimpleEntity")
    println(result9.pretty())
//    val result10 = executeQuery(db, "SELECT 'xxxfooxxx' LIKE '%foo%'")
//    val result10 = executeQuery(db, "SELECT 'xxxfoo' LIKE '%bar'")
//    val result10 = executeQuery(db, "SELECT 'xxxfoo' LIKE '%foo'")
//    val result10 = executeQuery(db, "SELECT 'fooxxx' LIKE 'foo%'")
    val result10 = executeQuery(db, "SELECT 'barxxx' LIKE 'foo%'")
    println(result10.pretty())
    val result11 = executeQuery(db, "SELECT value FROM SimpleEntity WHERE other IN (100, 200)")
    println(result11.pretty())
    val result12 = executeQuery(db, "SELECT value FROM SimpleEntity WHERE other IN (SELECT other FROM SimpleEntity)")
    println(result12.pretty())
  }

  @Test
  fun relationTest() {
    val db = emptyDb().change {
      register(DocumentEntity)
      register(DaemonEntity)
      DocumentEntity.new { it[DocumentEntity.NameAttr] = "Bar.java" }
      val document = DocumentEntity.new { it[DocumentEntity.NameAttr] = "Foo.java" }
      DaemonEntity.new { it[DaemonEntity.DocumentAttr] = document; it[DaemonEntity.BackendAttr] = "lsp" }
      DaemonEntity.new { it[DaemonEntity.DocumentAttr] = document; it[DaemonEntity.BackendAttr] = "intellij" }
      DaemonEntity.new { it[DaemonEntity.DocumentAttr] = document; it[DaemonEntity.BackendAttr] = "other" }
    }.dbAfter
    val result = executeQuery(db, "SELECT * FROM DocumentEntity, DaemonEntity")
    println(result.pretty())
  }
}