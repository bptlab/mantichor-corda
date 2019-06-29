package com.generated_ID_.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object _ID_

object _ID_V1 : MappedSchema(
        schemaFamily = _ID_.javaClass,
        version = 1,
        mappedTypes = listOf(_ID_V1.Persistent_ID_::class.java)) {
    @Entity
    @Table(name = "_ID__states")
    class Persistent_ID_(
            tables

            @Column(name = "linear_id")
    var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this(_INPUTS_"", UUID.randomUUID())
    }
}