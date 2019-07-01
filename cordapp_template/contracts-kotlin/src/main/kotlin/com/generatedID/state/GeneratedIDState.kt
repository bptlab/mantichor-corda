package com.generated_ID_.state

import com.generated_ID_.contract.Generated_ID_Contract
import com.generated_ID_.schema._ID_V1
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.AbstractParty
import net.corda.core.identity.Party
import net.corda.core.schemas.QueryableState
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState

@BelongsToContract(Generated_ID_Contract::class)
data class Generated_ID_State( PARTSINPUTval stateEnum: Int,
                               override val linearId: UniqueIdentifier = UniqueIdentifier()):
        LinearState, QueryableState {
    /** The public keys of the involved parties. */
    override val participants: List<AbstractParty> get() = listOf(PARTSLIST)
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        return when (schema) {
            is _ID_V1 -> _ID_V1.Persistent_ID_(
                    SCHEMAINPUTthis.stateEnum.toString(),
                    this.linearId.id
            )
            else -> throw IllegalArgumentException("Unrecognised schema $schema")
        }
    }
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(_ID_V1)
}