package com.example.contract

import com.example.state.PizzaState
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class PizzaContract : Contract {
    companion object {
        @JvmStatic
        val ID = "com.example.contract.PizzaContract"
    }

    /**
     * The verify() function of all the states' contracts must not throw an exception for a transaction to be
     * considered valid.
     */
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        requireThat {
            // Generic constraints around the Pizza transaction.
            "No inputs should be consumed when issuing a pizza order." using (tx.inputs.isEmpty())
            "Only one output state should be created." using (tx.outputs.size == 1)
            val out = tx.outputsOfType<PizzaState>().single()
            "The Lieferant and the Kunde cannot be the same entity." using (out.lieferant != out.kunde)
            "All of the participants must be signers." using (command.signers.containsAll(out.participants.map { it.owningKey }))

            // IOU-specific constraints.
            "The Pizza's amount must be non-negative." using (out.menge > 0)
            "The Pizza's price must be non-negative." using (out.preis > 0)
        }
    }

    /**
     * This contract only implements one command, Create.
     */
    interface Commands : CommandData {
        class Create : Commands
    }
}