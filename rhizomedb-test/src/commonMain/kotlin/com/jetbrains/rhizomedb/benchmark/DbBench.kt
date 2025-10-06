package com.jetbrains.rhizomedb.benchmark

import com.jetbrains.rhizomedb.*
import com.jetbrains.rhizomedb.impl.getAttributeValue
import kotlinx.benchmark.*
import kotlinx.serialization.builtins.serializer

data class MyEntity(override val eid: EID) : Entity {
    val s: String by SAttr

    companion object : EntityType<MyEntity>(MyEntity::class, ::MyEntity) {
        val SAttr = requiredValue("s", String.serializer())
    }
}

@State(Scope.Benchmark)
class DbBench {
    private var db: DB? = null
    private var e: MyEntity? = null

    private var attr: Attribute<*>? = null
    private var ctx: DbContext<Q>? = null

    @Setup
    fun prepare() {
        db = DB.empty().change {
            register(MyEntity)
            repeat(100000) { i ->
                MyEntity.new {
                    it[MyEntity.SAttr] = "$i"
                }
            }
            e = MyEntity.new {
                it[MyEntity.SAttr] = "hello"
            }
        }.dbAfter
        attr = MyEntity.SAttr.attr
        ctx = DbContext(db!!, null)
    }

    @Benchmark
    fun entityLookup(bh: Blackhole) {
        asOf(db!!) {
            bh.consume(e!!.s)
        }
    }

    @Benchmark
    fun entityLookupAvoidingThreadLocal(bh: Blackhole) {
        bh.consume(ctx!!.getAttributeValue(e!!, attr!!))
    }

    @Benchmark
    fun createOneEntity(bh: Blackhole) {
        bh.consume(db!!.change {
            MyEntity.new {
                it[MyEntity.SAttr] = "new entity"
            }
        }.dbAfter)
    }

    @Benchmark
    fun writeAttribute(bh: Blackhole) {
        bh.consume(db!!.change {
            e!![MyEntity.SAttr] = "bar"
        }.dbAfter)
    }
}