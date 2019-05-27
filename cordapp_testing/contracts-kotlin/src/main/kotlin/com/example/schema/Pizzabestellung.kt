package com.example.schema

import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

/**
 * The family of schemas for IOUState.
 */
object Pizzabestellung

/**
 * An IOUState schema.
 */
object PizzabestellungV1 : MappedSchema(
        schemaFamily = Pizzabestellung.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentPizzabestellung::class.java)) {
    @Entity
    @Table(name = "iou_states")
    class PersistentPizzabestellung(
            @Column(name = "lieferant")
            var lieferant: String,

            @Column(name = "kunde")
            var kunde: String,

            @Column(name = "menge")
            var menge: Int,

            @Column(name = "preis")
            var preis: Int,

            @Column(name = "linear_id")
            var linearId: UUID
    ) : PersistentState() {
        // Default constructor required by hibernate.
        constructor(): this("", "", 1, 1, UUID.randomUUID())
    }
}