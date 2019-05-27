package com.example.test.flow

import com.example.flow.ExampleFlow
import com.example.state.PizzaState
import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.node.services.queryBy
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.singleIdentity
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class PizzaFlowTests {
    private lateinit var network: MockNetwork
    private lateinit var a: StartedMockNode
    private lateinit var b: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
                TestCordapp.findCordapp("com.example.contract"),
                TestCordapp.findCordapp("com.example.flow")
        )))
        a = network.createPartyNode()
        b = network.createPartyNode()
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        listOf(a, b).forEach { it.registerInitiatedFlow(ExampleFlow.Acceptor::class.java) }
        network.runNetwork()
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    @Test
    fun `flow rejects invalid Orders`() {
        val flow = ExampleFlow.Initiator(-1, -1, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        // The PizzaContract specifies that Orders cannot have negative Menge oder Preis.
        assertFailsWith<TransactionVerificationException> { future.getOrThrow() }
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the initiator`() {
        val flow = ExampleFlow.Initiator(10, 80, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(b.info.singleIdentity().owningKey)
    }

    @Test
    fun `SignedTransaction returned by the flow is signed by the acceptor`() {
        val flow = ExampleFlow.Initiator(10, 80, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()

        val signedTx = future.getOrThrow()
        signedTx.verifySignaturesExcept(a.info.singleIdentity().owningKey)
    }

    @Test
    fun `flow records a transaction in both parties' transaction storages`() {
        val flow = ExampleFlow.Initiator(10, 80, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both transaction storages.
        for (node in listOf(a, b)) {
            assertEquals(signedTx, node.services.validatedTransactions.getTransaction(signedTx.id))
        }
    }

    @Test
    fun `recorded transaction has no inputs and a single output, the input Order`() {
        val pizzaMenge = 10
        val pizzaPrice = 80
        val flow = ExampleFlow.Initiator(pizzaMenge, pizzaPrice, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        val signedTx = future.getOrThrow()

        // We check the recorded transaction in both vaults.
        for (node in listOf(a, b)) {
            val recordedTx = node.services.validatedTransactions.getTransaction(signedTx.id)
            val txOutputs = recordedTx!!.tx.outputs
            assert(txOutputs.size == 1)

            val recordedState = txOutputs[0].data as PizzaState
            assertEquals(recordedState.menge, pizzaMenge)
            assertEquals(recordedState.preis, pizzaPrice)
            assertEquals(recordedState.lieferant, a.info.singleIdentity())
            assertEquals(recordedState.kunde, b.info.singleIdentity())
        }
    }

    @Test
    fun `flow records the correct Order in both parties' vaults`() {
        val pizzaMenge = 10
        val pizzaPreis = 80
        val flow = ExampleFlow.Initiator(10, 80, b.info.singleIdentity())
        val future = a.startFlow(flow)
        network.runNetwork()
        future.getOrThrow()

        // We check the recorded order in both vaults.
        for (node in listOf(a, b)) {
            node.transaction {
                val orders = node.services.vaultService.queryBy<PizzaState>().states
                assertEquals(1, orders.size)
                val recordedState = orders.single().state.data
                assertEquals(recordedState.menge, pizzaMenge)
                assertEquals(recordedState.preis, pizzaPreis)
                assertEquals(recordedState.lieferant, a.info.singleIdentity())
                assertEquals(recordedState.kunde, b.info.singleIdentity())
            }
        }
    }
}