package com.genrated_ID_.contract

import com.generated_ID_.state.Generated_ID_State
import net.corda.core.contracts.CommandData
import net.corda.core.contracts.Contract
import net.corda.core.contracts.requireSingleCommand
import net.corda.core.contracts.requireThat
import net.corda.core.transactions.LedgerTransaction

class _ID_Contract : Contract {

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands.Create>()
        when(command.value) {
            HANDLECOMMANDS
        }
    }

    interface Commands : CommandData {
        class AdditionalCommands : Commands
    }
}