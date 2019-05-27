package com.example.test.contract

import com.example.contract.PizzaContract
import com.example.state.PizzaState
import net.corda.core.identity.CordaX500Name
import net.corda.testing.core.TestIdentity
import net.corda.testing.node.MockServices
import net.corda.testing.node.ledger
import org.junit.Test

class PizzaContractTests {
    private val ledgerServices = MockServices(listOf("com.example.contract", "com.example.flow"))
    private val megaCorp = TestIdentity(CordaX500Name("MegaCorp", "London", "GB"))
    private val miniCorp = TestIdentity(CordaX500Name("MiniCorp", "New York", "US"))
    private val pizzaValue = 4
    private val pizzaPrice = 55

    @Test
    fun `transaction must include Create command`() {
        ledgerServices.ledger {
            transaction {
                output(PizzaContract.ID, PizzaState(pizzaValue, pizzaPrice, miniCorp.party, megaCorp.party))
                fails()
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), PizzaContract.Commands.Create())
                verifies()
            }
        }
    }

    @Test
    fun `transaction must have no inputs`() {
        ledgerServices.ledger {
            transaction {
                input(PizzaContract.ID, PizzaState(pizzaValue, pizzaPrice,miniCorp.party, megaCorp.party))
                output(PizzaContract.ID, PizzaState(pizzaValue, pizzaPrice, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), PizzaContract.Commands.Create())
                `fails with`("No inputs should be consumed when issuing a pizza order.")
            }
        }
    }

    @Test
    fun `transaction must have one output`() {
        ledgerServices.ledger {
            transaction {
                output(PizzaContract.ID, PizzaState(pizzaValue, pizzaPrice, miniCorp.party, megaCorp.party))
                output(PizzaContract.ID, PizzaState(pizzaValue, pizzaPrice, miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), PizzaContract.Commands.Create())
                `fails with`("Only one output state should be created.")
            }
        }
    }

    @Test
    fun `lieferant must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(PizzaContract.ID, PizzaState(pizzaValue, pizzaPrice, miniCorp.party, megaCorp.party))
                command(miniCorp.publicKey, PizzaContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `kunde must sign transaction`() {
        ledgerServices.ledger {
            transaction {
                output(PizzaContract.ID, PizzaState(pizzaValue, pizzaPrice, miniCorp.party, megaCorp.party))
                command(megaCorp.publicKey, PizzaContract.Commands.Create())
                `fails with`("All of the participants must be signers.")
            }
        }
    }

    @Test
    fun `lieferant is not kunde`() {
        ledgerServices.ledger {
            transaction {
                output(PizzaContract.ID, PizzaState(pizzaValue, pizzaPrice, megaCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), PizzaContract.Commands.Create())
                `fails with`("The Lieferant and the Kunde cannot be the same entity.")
            }
        }
    }

    @Test
    fun `cannot create negative-amount orders`() {
        ledgerServices.ledger {
            transaction {
                output(PizzaContract.ID, PizzaState(-1, 20,miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), PizzaContract.Commands.Create())
                `fails with`("The Pizza's amount must be non-negative.")
            }
        }
    }

    @Test
    fun `cannot create negative-price orders`() {
        ledgerServices.ledger {
            transaction {
                output(PizzaContract.ID, PizzaState(20, -1,miniCorp.party, megaCorp.party))
                command(listOf(megaCorp.publicKey, miniCorp.publicKey), PizzaContract.Commands.Create())
                `fails with`("The Pizza's price must be non-negative.")
            }
        }
    }
}