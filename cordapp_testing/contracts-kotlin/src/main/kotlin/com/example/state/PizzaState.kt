package com.example.state

import com.example.contract.PizzaContract
import com.example.schema.PizzabestellungV1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState

/**
 * The state object recording IOU agreements between two parties.
 *
 * A state must implement [ContractState] or one of its descendants.
 *
 * @param preis the price of the pizzas.
 * @param menge the ammount of the pizzas
 * @param lieferant the party receiving and approving the IOU.
 * @param kunde the party issuing the IOU.
 */
@BelongsToContract(PizzaContract::class)
data class PizzaState(val menge: Int,
                    val preis: Int,
                    val lieferant: Party,
                    val kunde: Party,
                    override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(lieferant, kunde)

    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is PizzabestellungV1 -> PizzabestellungV1.PersistentPizzabestellung(
                    this.lieferant.name.toString(),
                    this.kunde.name.toString(),
                    this.menge,
                    this.preis,
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }

    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(PizzabestellungV1)
}