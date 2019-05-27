package com.example

import net.corda.core.identity.CordaX500Name
import net.corda.core.utilities.getOrThrow
import net.corda.testing.core.TestIdentity
import net.corda.testing.driver.DriverParameters
import net.corda.testing.driver.driver
import org.junit.Test
import kotlin.test.assertEquals

class DriverBasedTests {
    val lieferantA = TestIdentity(CordaX500Name("LieferantA", "", "GB"))
    val lieferantB = TestIdentity(CordaX500Name("LieferantB", "", "US"))

    @Test
    fun `node test`() {
        driver(DriverParameters(isDebug = true, startNodesInProcess = true)) {
            // This starts two nodes simultaneously with startNode, which returns a future that completes when the node
            // has completed startup. Then these are all resolved with getOrThrow which returns the NodeHandle list.
            val (partyAHandle, partyBHandle) = listOf(
                    startNode(providedName = lieferantA.name),
                    startNode(providedName = lieferantB.name)
            ).map { it.getOrThrow() }

            // This test makes an RPC call to retrieve another node's name from the network map, to verify that the
            // nodes have started and can communicate. This is a very basic test, in practice tests would be starting
            // flows, and verifying the states in the vault and other important metrics to ensure that your CorDapp is
            // working as intended.
            assertEquals(partyAHandle.rpc.wellKnownPartyFromX500Name(lieferantB.name)!!.name, lieferantB.name)
            assertEquals(partyBHandle.rpc.wellKnownPartyFromX500Name(lieferantA.name)!!.name, lieferantA.name)
        }
    }
}
