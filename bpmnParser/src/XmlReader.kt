import com.sun.org.apache.xpath.internal.operations.Bool
import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

open class XmlReader(private val xmlPath : String) {

    fun readXml(): Document {
        val xmlFile = File(xmlPath)
        val dbFactory = DocumentBuilderFactory.newInstance()
        val dBuilder = dbFactory.newDocumentBuilder()
        return dBuilder.parse(xmlFile)
    }

    fun getElementValuesByAttributeName(doc: Document, attributeName: String): NodeList {
        return doc.getElementsByTagName(attributeName)
    }

    fun getValueOfNode(node: Node, valueName: String) : String {
        return node.attributes.getNamedItem(valueName).nodeValue
    }

    fun getOutgoingLines(gateway: Node) : Int {
        val connectors = gateway.childNodes
        var outgoing = 0
        for(i in 0..connectors.length - 1) {
            if(connectors.item(i).nodeName == "bpmn2:outgoing") {
                outgoing++
            }
        }
        return outgoing
    }

    fun checkGateWayType(doc: Document, source: String) : Node? {
        val eventNodes = getElementValuesByAttributeName(doc, "bpmn2:eventBasedGateway")
        for(i in 0..eventNodes.length - 1) {
            val node = eventNodes.item(i)
            if(getValueOfNode(node, "id") == source) {
                return node
            }
        }
        val parallelNodes = getElementValuesByAttributeName(doc, "bpmn2:parallelGateway")
        for(i in 0..parallelNodes.length - 1) {
            val node = parallelNodes.item(i)
            if(getValueOfNode(node, "id") == source) {
                return node
            }
        }
        val exclusiveNodes = getElementValuesByAttributeName(doc, "bpmn2:exclusiveGateway")
        for(i in 0..exclusiveNodes.length - 1) {
            val node = exclusiveNodes.item(i)
            if(getValueOfNode(node, "id") == source) {
                return node
            }
        }
        return null
    }

    fun getPreviousTask(doc: Document, source: String) : String {
        val tasksNodes = getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
        for(i in 0..tasksNodes.length - 1) {
            val node = tasksNodes.item(i)
            if(getValueOfNode(node, "id") == source) {
                return "" + i
            }
        }
        return "0"
    }

    fun getTasksBeforeGateway(doc: Document, gatewayNode: Node) : MutableList<String> {
        val connectors = gatewayNode.childNodes
        var incomming = 0
        var outgoing = 0
        for(i in 0..connectors.length - 1) {
            if(connectors.item(i).nodeName == "bpmn2:incoming") {
                incomming++
            }
            if(connectors.item(i).nodeName == "bpmn2:outgoing") {
                outgoing++
            }
        }
        val childs = gatewayNode.childNodes
        val incommingId = mutableSetOf<String>()
        for (i in 0..childs.length - 1) {
            if (childs.item(i).nodeName == "bpmn2:incoming") {
                incommingId.add(childs.item(i).textContent)
            }
        }
        val prevIds = mutableListOf<String>()
        for(i in 0..incommingId.size - 1) {
            val sequenzNodes = getElementValuesByAttributeName(doc, "bpmn2:sequenceFlow")
            for (j in 0..sequenzNodes.length - 1) {
                val node = sequenzNodes.item(j)
                if (getValueOfNode(node, "id") == incommingId.elementAt(i)) {
                    val taskId = getValueOfNode(node, "sourceRef")
                    val taskNodes = getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
                    var foundone = false
                    for(k in 0..taskNodes.length - 1) {
                        val taskNode = taskNodes.item(k)
                        if(getValueOfNode(taskNode, "id") == taskId) {
                            prevIds.add("" + (k + 1) )
                            foundone = true
                        }
                    }
                    if(!foundone) {
                        val possibleGatewayNode = checkGateWayType(doc, taskId)
                        if(possibleGatewayNode == null) {
                            val startEvent = getElementValuesByAttributeName(doc, "bpmn2:startEvent")
                            for(k in 0..startEvent.length - 1) {
                                if(getValueOfNode(startEvent.item(k), "id") == taskId) {
                                    prevIds.add("" + 0)
                                }
                            }
                        } else {
                            val results = getTasksBeforeGateway(doc, possibleGatewayNode)
                            for(k in 0..results.size - 1) {
                                prevIds.add(results.elementAt(k))
                            }
                        }
                    }
                }
            }
        }
        return prevIds
    }

    fun checkIfIncommingNodeIsGateway(doc: Document, taskNode: Node) : String {
        val childs = taskNode.childNodes
        var incommingId = ""
        for(i in 0..childs.length - 1) {
            if(childs.item(i).nodeName == "bpmn2:incoming") {
                incommingId = childs.item(i).textContent
            }
        }
        val sequenzNodes = getElementValuesByAttributeName(doc, "bpmn2:sequenceFlow")
        for(i in 0..sequenzNodes.length - 1) {
            val node = sequenzNodes.item(i)
            if(getValueOfNode(node, "id") == incommingId) {
                val source = getValueOfNode(node, "sourceRef")
                val gatewayNode = checkGateWayType(doc, source)
                if(gatewayNode == null) {
                    return "t" + getPreviousTask(doc, source) + "%" + 1
                }
                val outgoing = getOutgoingLines(gatewayNode)
                if (gatewayNode.nodeName == "bpmn2:exclusiveGateway") {
                    val tasks = getTasksBeforeGateway(doc, gatewayNode)
                    var argumentString =  "e"
                    for(j in 0..tasks.size -1) {
                        argumentString += tasks.elementAt(j)
                        if(j < tasks.size - 1) {
                            argumentString += ","
                        }
                    }
                    return argumentString + "%" + outgoing
                }
                if (gatewayNode.nodeName == "bpmn2:eventBasedGateway") {
                    val tasks = getTasksBeforeGateway(doc, gatewayNode)
                    var argumentString =  "e"
                    for(j in 0..tasks.size -1) {
                        argumentString += tasks.elementAt(j)
                        if(j < tasks.size - 1) {
                            argumentString += ","
                        }
                    }
                    return argumentString + "%" + outgoing
                }
                if (gatewayNode.nodeName == "bpmn2:parallelGateway") {
                    val tasks = getTasksBeforeGateway(doc, gatewayNode)
                    var argumentString =  "p"
                    for(j in 0..tasks.size -1) {
                        argumentString += tasks.elementAt(j)
                        if(j < tasks.size - 1) {
                            argumentString += ","
                        }
                    }
                    return argumentString + "%" + outgoing
                }
            }
        }
        return ""
    }

    fun getInitParticipant(node: Node) : String {
        return getValueOfNode(node, "initiatingParticipantRef")
    }

    fun generateCamelCaseName(task: String) : String{
        val splits = task.split(" ")
        var camelCaseCommand = ""
        for (j in 0..splits.size - 1) {
            camelCaseCommand += splits[j].capitalize()
        }
        return camelCaseCommand
    }

    fun generateInputs(participants: MutableSet<String>) : String {
        var inputs = ""
        for(i in 0..participants.size-1){
           inputs += "\"\", "
        }
        return inputs
    }

    fun generateInputList(participants: MutableSet<String>, position: Int) : String {
        var list = ""
        for(i in 0..participants.size-1) {
            if(position != i) {
                list += "val otherParty" + i + ": Party"
                if(i < participants.size-1 && !(i + 1 == participants.size-1 && participants.size-1 == position)){
                    list += ", "
                }
            }
        }
        return list
    }

    fun generateRpcParams(participants: MutableSet<String>, position: Int) : String {
        var list = ""
        for(i in 0..participants.size-1) {
            if(position != i) {
                list += "otherParty" + i
                if(i < participants.size-1 && !(i + 1 == participants.size-1 && participants.size-1 == position)){
                    list += ", "
                }
            }
        }
        return list
    }

    fun generateQueryParsing(participants: MutableSet<String>, position: Int) : String {
        var parsing = ""
        for(i in 0..participants.size-1) {
            if (position != i) {
                parsing += "        val partyName" + i + " = request.getParameter(\"partyName" + i + "\")\n" +
                           "        if(partyName" + i + " == null){\n" +
                           "            return ResponseEntity.badRequest().body(\"Query parameter 'partyName" + i + "' must not be null.\\n\")\n" +
                           "        }\n" +
                           "        val partyX500Name" + i + " = CordaX500Name.parse(partyName" + i + ")\n" +
                           "        val otherParty" + i + " = proxy.wellKnownPartyFromX500Name(partyX500Name" + i + ") ?: return ResponseEntity.badRequest().body(\"Party named \$partyName" + i + " cannot be found.\\n\")\n"
            }
        }
        return parsing
    }

    fun generateOutputList(participants: MutableSet<String>, position: Int) : String {
        var list = ""
        for(i in 0..participants.size-1) {
            if(position != i) {
                list +="            val otherPartySession" + i + " = initiateFlow(otherParty" + i + ")\n"
            }
        }
        return list
    }

    fun generateSessionsList(participants: MutableSet<String>, position: Int) : String {
        var list = ""
        for(i in 0..participants.size-1) {
            if(position != i) {
                list +="otherPartySession" + i
                if(i < participants.size-1 && !(i + 1 == participants.size-1 && participants.size-1 == position)){
                    list += ", "
                }
            }
        }
        return list
    }

    fun getInitPosition(participants: MutableSet<String>, initPart: String) : Int {
        for(i in 0..participants.size -1) {
            if(initPart == participants.elementAt(i)) {
                return i
            }
        }
        return -1
    }

    fun generateWorkflowStateInput(participants: MutableSet<String>, position: Int) : String {
        var input = ""
        for(i in 0..participants.size - 1) {
            if(i == position) {
                input += "serviceHub.myInfo.legalIdentities.first(), "
            } else {
                input += "otherParty" + i + ", "
            }
        }
        return input
    }
    fun buildStateExchange(gatewayCheck: String, futureState: Int) : String {
        val mode = gatewayCheck.get(0)
        val checkString = gatewayCheck.drop(1).split("%")[0].split(",")
        val outgoing = gatewayCheck.drop(1).split("%")[1].toInt()
        var output = "            var position = -1\n" +
                     "            var newState = \"\"\n" +
                     "            var foundInvalidity = false\n" +
                     "            for(i in 0..subStates.size - 1) {\n" +
                     "                if( "
        for(i in 0..checkString.size - 1) {
            when (mode) {
                'e' -> {
                    output += "(subStates[i].split(\"_\")[0] == \"" + checkString[i] + "\")"
                    if(i < checkString.size - 1) {
                        output += " || "
                    }
                }
                'p' -> {
                    if(outgoing < 2) {
                        output += "subStates.contains(\"" + checkString[i] + "\")"
                        if (i < checkString.size - 1) {
                            output += " && "
                        }
                    } else {
                        output += "(subStates[i].split(\"_\")[0] == \"" + checkString[i] + "\")"
                        if(i < checkString.size - 1) {
                            output += " || "
                        }
                    }
                }
                't' -> {
                    output += "(subStates[i].split(\"_\")[0] == \"" + checkString[i] + "\")"
                    if(i < checkString.size - 1) {
                        output += " || "
                    }
                }
            }
        }
        if(mode == 'p' && outgoing < 2) {
            output += " && ("
            for(i in 0..checkString.size - 1) {
                output += "(subStates[i].split(\"_\")[0] == \"" + checkString[i] + "\")"
                if(i < checkString.size - 1) {
                    output += " || "
                }
            }
            output += ")) {\n"
        } else {
            output += ") {\n"
        }
        output += "                    position = i\n" +
                 "                }\n" +
                 "                if(subStates[i] == \"" + futureState + "\" || subStates[i].split(\"_\")[0] == \"" + futureState + "\") {\n" +
                 "                    foundInvalidity = true\n" +
                 "                }\n" +
                 "            }\n" +
                 "            if (position != -1) {\n" +
                 "                val checking = subStates[position].split(\"_\")\n" +
                 "                for(i in 1..checking.size - 1) {\n" +
                 "                    if(checking[i] == \"" + futureState + "\") {\n" +
                 "                        foundInvalidity = true\n" +
                 "                    }\n" +
                 "                }\n"
        if(mode == 'p') {
            output += "                subStates[position] += \"_" + futureState + "\"\n" +
                    "                newState += \"" + futureState + ",\"\n"

        } else {
            output += "                subStates[position] = \"" + futureState + "\"\n"
        }
        output += "                for(i in 0..subStates.size - 1) {\n" +
                  "                    newState += subStates[i]\n" +
                  "                    if(i < subStates.size -1) { \n" +
                  "                        newState += \",\"\n" +
                  "                    }\n" +
                  "                }\n" +
                  "            }\n"
        return  output
    }

    fun generateFlow(tasks: MutableSet<String>, doc: Document, nodeTasks: NodeList, contractId: String, participantsSet: MutableSet<String>) : String {
        var flow = ""
        val participants = getElementValuesByAttributeName(doc, "bpmn2:participant")
        flow += "    @InitiatingFlow\n" +
                "    @StartableByRPC\n" +
                "    class " + "Initiator" + "(" + generateInputList(participantsSet, 0) + ") : FlowLogic<SignedTransaction>() {\n"
        flow += "        companion object {\n" +
                "            object GENERATING_TRANSACTION : Step(\"Generating transaction based on new Input.\")\n" +
                "            object VERIFYING_TRANSACTION : Step(\"Verifying contract constraints.\")\n" +
                "            object SIGNING_TRANSACTION : Step(\"Signing transaction with our private key.\")\n" +
                "            object GATHERING_SIGS : Step(\"Gathering the counterparty's signature.\") {\n" +
                "                override fun childProgressTracker() = CollectSignaturesFlow.tracker()\n" +
                "            }\n" +
                "\n" +
                "            object FINALISING_TRANSACTION : Step(\"Obtaining notary signature and recording transaction.\") {\n" +
                "                override fun childProgressTracker() = FinalityFlow.tracker()\n" +
                "            }\n" +
                "\n" +
                "            fun tracker() = ProgressTracker(\n" +
                "                    GENERATING_TRANSACTION,\n" +
                "                    VERIFYING_TRANSACTION,\n" +
                "                    SIGNING_TRANSACTION,\n" +
                "                    GATHERING_SIGS,\n" +
                "                    FINALISING_TRANSACTION\n" +
                "            )\n" +
                "        }\n\n" +
                "        override val progressTracker = tracker()\n\n"
        flow += "        @Suspendable\n" +
                "        override fun call(): SignedTransaction {\n" +
                "            // Obtain a reference to the notary we want to use.\n" +
                "            val notary = serviceHub.networkMapCache.notaryIdentities[0]\n" +
                "\n" +
                "            // Stage 1.\n" +
                "            progressTracker.currentStep = GENERATING_TRANSACTION\n" +
                "            // Generate an unsigned transaction.\n" +
                "            val " + contractId + "State = Generated" + contractId + "State(" + generateWorkflowStateInput(participantsSet, 0) + "\"0\")\n" +
                "            val txCommand = Command(Generated" + contractId + "Contract.Commands." + "Create" + "(), " + contractId + "State.participants.map { it.owningKey })\n" +
                "            val txBuilder = TransactionBuilder(notary)\n" +
                "                    .addOutputState(" + contractId + "State, Generated" + contractId + "Contract.ID)\n" +
                "                    .addCommand(txCommand)\n" +
                "\n" +
                "            // Stage 2.\n" +
                "            progressTracker.currentStep = VERIFYING_TRANSACTION\n" +
                "            // Verify that the transaction is valid.\n" +
                "            txBuilder.verify(serviceHub)\n" +
                "\n" +
                "            // Stage 3.\n" +
                "            progressTracker.currentStep = SIGNING_TRANSACTION\n" +
                "            // Sign the transaction.\n" +
                "            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)\n" +
                "\n" +
                "            // Stage 4.\n" +
                "            progressTracker.currentStep = GATHERING_SIGS\n" +
                "            // Send the state to the counterparties, and receive it back with their signatures.\n" +
                generateOutputList(participantsSet, 0) +
                "            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(" + generateSessionsList(participantsSet, 0) + "), GATHERING_SIGS.childProgressTracker()))\n" +
                "\n" +
                "            // Stage 5.\n" +
                "            progressTracker.currentStep = FINALISING_TRANSACTION\n" +
                "            // Notarise and record the transaction in all parties' vaults.\n" +
                "            return subFlow(FinalityFlow(fullySignedTx, setOf(" + generateSessionsList(participantsSet, 0) + "), FINALISING_TRANSACTION.childProgressTracker()))\n" +
                "        }\n" +
                "    }\n"
        flow += "    @InitiatedBy(" + "Initiator"+ "::class)\n" +
                "    class " + "Acceptor" + "(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {\n" +
                "        @Suspendable\n" +
                "        override fun call(): SignedTransaction {\n" +
                "            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {\n" +
                "                override fun checkTransaction(stx: SignedTransaction) = requireThat {\n" +
                "                    val output = stx.tx.outputs.single().data\n" +
                "                    \"This must be a valid transaction.\" using (output is Generated" + contractId + "State)\n" +
                "                }\n" +
                "            }\n" +
                "            val txId = subFlow(signTransactionFlow).id\n" +
                "\n" +
                "            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))\n" +
                "        }\n" +
                "    }\n"
        for(i in 0..tasks.size - 1) {
            val correspondingNode = nodeTasks.item(i)
            val gatewayCheck = checkIfIncommingNodeIsGateway(doc, correspondingNode)
            val command = tasks.elementAt(i)
            val camelCaseCommand = generateCamelCaseName(command)
            val initFlow = getInitParticipant(correspondingNode) + camelCaseCommand.capitalize() + "Flow"
            val reactingFlow = "reaction" + camelCaseCommand.capitalize() + "Flow"
            val position = getInitPosition(participantsSet, getInitParticipant(correspondingNode))
            flow += "    @InitiatingFlow\n" +
                    "    @StartableByRPC\n" +
                    "    class " + initFlow + "(" + generateInputList(participantsSet, position) + ") : FlowLogic<SignedTransaction>() {\n"
            flow += "        companion object {\n" +
                    "            object GENERATING_TRANSACTION : Step(\"Generating transaction based on new Input.\")\n" +
                    "            object VERIFYING_TRANSACTION : Step(\"Verifying contract constraints.\")\n" +
                    "            object SIGNING_TRANSACTION : Step(\"Signing transaction with our private key.\")\n" +
                    "            object GATHERING_SIGS : Step(\"Gathering the counterparty's signature.\") {\n" +
                    "                override fun childProgressTracker() = CollectSignaturesFlow.tracker()\n" +
                    "            }\n" +
                    "\n" +
                    "            object FINALISING_TRANSACTION : Step(\"Obtaining notary signature and recording transaction.\") {\n" +
                    "                override fun childProgressTracker() = FinalityFlow.tracker()\n" +
                    "            }\n" +
                    "\n" +
                    "            fun tracker() = ProgressTracker(\n" +
                    "                    GENERATING_TRANSACTION,\n" +
                    "                    VERIFYING_TRANSACTION,\n" +
                    "                    SIGNING_TRANSACTION,\n" +
                    "                    GATHERING_SIGS,\n" +
                    "                    FINALISING_TRANSACTION\n" +
                    "            )\n" +
                    "        }\n\n" +
                    "        override val progressTracker = tracker()\n\n"
            flow += "        @Suspendable\n" +
                    "        override fun call(): SignedTransaction {\n" +
                    "            // Obtain a reference to the notary we want to use.\n" +
                    "            val notary = serviceHub.networkMapCache.notaryIdentities[0]\n" +
                    "\n" +
                    "            // Stage 1.\n" +
                    "            progressTracker.currentStep = GENERATING_TRANSACTION\n" +
                    "            // Generate an unsigned transaction.\n" +
                    "            val currentState = serviceHub.vaultService.queryBy<Generated" + contractId + "State>().states.last().state.data.stateEnum\n" +
                    "            val subStates = currentState.split(\",\").toMutableList()\n"+
                    buildStateExchange(gatewayCheck, i + 1)
            flow += "            val " + contractId + "State = Generated" + contractId + "State(" + generateWorkflowStateInput(participantsSet, position) + "newState)\n"
            flow += "            val txCommand = Command(Generated" + contractId + "Contract.Commands." + camelCaseCommand.capitalize() + "(), " + contractId + "State.participants.map { it.owningKey })\n" +
                    "            val txBuilder = TransactionBuilder(notary)\n" +
                    "                    .addOutputState(" + contractId + "State, Generated" + contractId + "Contract.ID)\n" +
                    "                    .addCommand(txCommand)\n" +
                    "\n" +
                    "            // Stage 2.\n" +
                    "            progressTracker.currentStep = VERIFYING_TRANSACTION\n" +
                    "            // Verify that the transaction is valid.\n" +
                    "            txBuilder.verify(serviceHub)\n" +
                    "            requireThat {\n" +
                    "                \"only " + getInitParticipant(correspondingNode).capitalize() + " can invoke this call\" using(serviceHub.myInfo.legalIdentities.first().name.organisation == \"" + getInitParticipant(correspondingNode) + "\")\n" +
                    "                \"not a reachable state\" using(position != - 1 && !foundInvalidity)\n" +
                    "            }\n" +
                    "\n" +
                    "            // Stage 3.\n" +
                    "            progressTracker.currentStep = SIGNING_TRANSACTION\n" +
                    "            // Sign the transaction.\n" +
                    "            val partSignedTx = serviceHub.signInitialTransaction(txBuilder)\n" +
                    "\n" +
                    "            // Stage 4.\n" +
                    "            progressTracker.currentStep = GATHERING_SIGS\n" +
                    "            // Send the state to the counterparties, and receive it back with their signatures.\n" +
                    generateOutputList(participantsSet, position) +
                    "            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(" + generateSessionsList(participantsSet, position) + "), GATHERING_SIGS.childProgressTracker()))\n" +
                    "\n" +
                    "            // Stage 5.\n" +
                    "            progressTracker.currentStep = FINALISING_TRANSACTION\n" +
                    "            // Notarise and record the transaction in all parties' vaults.\n" +
                    "            return subFlow(FinalityFlow(fullySignedTx, setOf(" + generateSessionsList(participantsSet, position) + "), FINALISING_TRANSACTION.childProgressTracker()))\n" +
                    "        }\n" +
                    "    }\n"
            flow += "    @InitiatedBy(" + initFlow + "::class)\n" +
                    "    class " + reactingFlow + "(val otherPartySession: FlowSession) : FlowLogic<SignedTransaction>() {\n" +
                    "        @Suspendable\n" +
                    "        override fun call(): SignedTransaction {\n" +
                    "            val signTransactionFlow = object : SignTransactionFlow(otherPartySession) {\n" +
                    "                override fun checkTransaction(stx: SignedTransaction) = requireThat {\n" +
                    "                    val output = stx.tx.outputs.single().data\n" +
                    "                    \"This must be a valid transaction.\" using (output is Generated" + contractId + "State)\n" +
                    "                }\n" +
                    "            }\n" +
                    "            val txId = subFlow(signTransactionFlow).id\n" +
                    "\n" +
                    "            return subFlow(ReceiveFinalityFlow(otherPartySession, expectedTxId = txId))\n" +
                    "        }\n" +
                    "    }\n"
        }
        return flow
    }

    fun generateNodes(participants: MutableSet<String>) : String{
        var nodes = ""
        var ports = 10004
        for(i in 0..participants.size-1){
            nodes += "    node {\n" +
                     "        name \"O=" + participants.elementAt(i) + ",L=London,C=GB\"\n" +
                     "        p2pPort " + ports + "\n" +
                     "        rpcSettings {\n" +
                     "            address(\"localhost:" + (ports + 1) + "\")\n" +
                     "            adminAddress(\"localhost:" + (ports + 2) + "\")\n" +
                     "        }\n" +
                     "        rpcUsers = [[user: \"user1\", \"password\": \"test\", \"permissions\": [\"ALL\"]]]\n" +
                     "    }\n"
            ports += 3
        }
        return nodes
    }

    fun generateFlowImport(tasks: MutableSet<String>, doc: Document, nodeTasks: NodeList, contractId: String) : String {
        var imports = ""
        val participants = getElementValuesByAttributeName(doc, "bpmn2:participant")
        for(i in 0..tasks.size - 1) {
            val correspondingNode = nodeTasks.item(i)
            val command = tasks.elementAt(i)
            val camelCaseCommand = generateCamelCaseName(command)
            val initFlow = getInitParticipant(correspondingNode) + camelCaseCommand.capitalize() + "Flow"
            imports += "import com.generated" + contractId + ".flow.ExampleFlow." + initFlow + "\n"
        }
        return imports
    }

    fun generateRPCConnection(tasks: MutableSet<String>, doc: Document, nodeTasks: NodeList, participantsSet: MutableSet<String>) : String {
        var rpcConnection = ""
        val participants = getElementValuesByAttributeName(doc, "bpmn2:participant")
        rpcConnection += "@PostMapping(value = [ \"CreateChoreographie\" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ \"Content-Type=application/x-www-form-urlencoded\" ])\n" +
                "    fun createChoreographie(request: HttpServletRequest): ResponseEntity<String> {\n" +
                generateQueryParsing(participantsSet, 0) +
                "\n" +
                "        return try {\n" +
                "            val signedTx = proxy.startTrackedFlow(::Initiator, " + generateRpcParams(participantsSet, 0) + ").returnValue.getOrThrow()\n" +
                "            ResponseEntity.status(HttpStatus.CREATED).body(\"Transaction id \${signedTx.id} committed to ledger.\\n\")\n" +
                "\n" +
                "        } catch (ex: Throwable) {\n" +
                "            logger.error(ex.message, ex)\n" +
                "            ResponseEntity.badRequest().body(ex.message!!)\n" +
                "        }\n" +
                "    }"
        for(i in 0..tasks.size - 1) {
            val correspondingNode = nodeTasks.item(i)
            val command = tasks.elementAt(i)
            val camelCaseCommand = generateCamelCaseName(command)
            val initFlow = getInitParticipant(correspondingNode) + camelCaseCommand.capitalize() + "Flow"
            val position = getInitPosition(participantsSet, getInitParticipant(correspondingNode))
            rpcConnection += "@PostMapping(value = [ \"" + camelCaseCommand + "\" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ \"Content-Type=application/x-www-form-urlencoded\" ])\n" +
                    "    fun " + camelCaseCommand + "(request: HttpServletRequest): ResponseEntity<String> {\n" +
                    generateQueryParsing(participantsSet, position) +
                    "\n" +
                    "        return try {\n" +
                    "            val signedTx = proxy.startTrackedFlow(::" + initFlow + ", " + generateRpcParams(participantsSet, position) + ").returnValue.getOrThrow()\n" +
                    "            ResponseEntity.status(HttpStatus.CREATED).body(\"Transaction id \${signedTx.id} committed to ledger.\\n\")\n" +
                    "\n" +
                    "        } catch (ex: Throwable) {\n" +
                    "            logger.error(ex.message, ex)\n" +
                    "            ResponseEntity.badRequest().body(ex.message!!)\n" +
                    "        }\n" +
                    "    }\n\n"
        }
        return rpcConnection
    }

    fun serverBuild(participants: MutableSet<String>, contractId: String) : String {
        var build = ""
        var ports = 5
        for(i in 0..participants.size-1) {
            build += "task run" + participants.elementAt(i) + "Server(type: JavaExec, dependsOn: jar) {\n" +
                    "    classpath = sourceSets.main.runtimeClasspath\n" +
                    "    main = 'com.generated" + contractId + ".server.ServerKt'\n" +
                    "    args '--server.port=" + (ports + 50000) + "', '--config.rpc.host=localhost', '--config.rpc.port=" + (ports + 10000) + "', '--config.rpc.username=user1', '--config.rpc.password=test'\n" +
                    "}\n\n"
            ports += 3
        }
        return build
    }

    fun generateTables(participants: MutableSet<String>) : String {
        var tables = ""
        for(i in 0..participants.size-1) {
            tables += "            @Column(name = \"" + participants.elementAt(i) + "\")\n" +
                    "            var " + participants.elementAt(i) + ": String,\n\n"
        }
        tables += "            @Column(name = \"stateEnum\")\n" +
                "            var stateEnum: String,\n\n"
        return tables
    }

    fun generateCommands(tasks: MutableSet<String>) :String {
        val commands = mutableSetOf<String>()
        var commandText = "\n        class Create : Commands"
        for (i in 0..tasks.size - 1) {
            val command = tasks.elementAt(i)
            val camelCaseCommand = generateCamelCaseName(command)
            commands.add(camelCaseCommand)
            commandText = commandText + "\n        class " + camelCaseCommand + " : Commands"
        }
        return commandText;
    }

    fun generateStateList(participants: MutableSet<String>) : String {
        var list = ""
        for(i in 0..participants.size-1) {
            list += participants.elementAt(i)
            if(i < participants.size-1) {
                list += ", "
            }
        }
        return list
    }

    fun generateStateParams(participants: MutableSet<String>) : String {
        var params = ""
        for(i in 0..participants.size-1) {
            params += "val " + participants.elementAt(i) + ": Party,\n                               "
        }
        return params
    }

    fun generateStateSchema(participants: MutableSet<String>) : String {
        var schemaParams = ""
        for(i in 0..participants.size-1) {
            schemaParams += "this." + participants.elementAt(i) + ".name.toString(),\n                    "
        }
        return schemaParams
    }

    fun generateContractFile(doc: Document, participants: MutableSet<String>, tasks: MutableSet<String>) {
        var contractId = getValueOfNode(getElementValuesByAttributeName(doc, "bpmn2:choreography").item(0), "id")
        contractId = contractId.replace("-", "")
        val testing_files = File("../cordapp_template")
        val directory = "../cordapp_" + contractId.capitalize()
        testing_files.copyRecursively(File(directory))
        val contractsFile = File(directory + "/contracts-kotlin/src/main/kotlin/com/generatedID")
        contractsFile.renameTo(File(directory + "/contracts-kotlin/src/main/kotlin/com/generated" + contractId))
        val workflowFile = File(directory + "/workflows-kotlin/src/main/kotlin/com/generatedID")
        workflowFile.renameTo(File(directory + "/workflows-kotlin/src/main/kotlin/com/generated" + contractId))
        val serverFile = File(directory + "/clients/src/main/kotlin/com/generatedID")
        serverFile.renameTo(File(directory + "/clients/src/main/kotlin/com/generated" + contractId))
        val contractsDir = directory + "/contracts-kotlin/src/main/kotlin/com/generated" + contractId + "/"
        val workflowDir = directory + "/workflows-kotlin/src/main/kotlin/com/generated" + contractId + "/flow"
        val serverDir = directory + "/clients/src/main/kotlin/com/generated" + contractId + "/server"
        val files = arrayOf(contractsDir + "contract/GeneratedIDContract.kt",
                                         contractsDir + "state/GeneratedIDState.kt",
                                         contractsDir + "schema/ID.kt",
                                         workflowDir + "/ExampleFlow.kt",
                                         directory + "/workflows-kotlin/build.gradle",
                                         directory + "/contracts-kotlin/build.gradle",
                                         serverDir + "/MainController.kt",
                                         serverDir + "/NodeRPCConnection.kt",
                                         serverDir + "/Server.kt",
                                         directory + "/clients/build.gradle",
                                         directory + "/gradle.properties")
        val taskNodes = getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
        for (file in files) {
            val f = File(file)
            var text = f.readText()
            text = text.replace("_ID_", contractId)
            text = text.replace("ParticipantA", participants.elementAt(0))
            text = text.replace("ParticipantB",participants.elementAt(1))
            var generatingFile = File("ERROR")
            //TO-DO: Refactor to switch case
            if(file == contractsDir + "contract/GeneratedIDContract.kt"){
                text = text.replace("\n        class AdditionalCommands : Commands", generateCommands(tasks))
                generatingFile = File(contractsDir + "contract/GeneratedIDContract.kt")
            } else if(file == contractsDir + "state/GeneratedIDState.kt"){
                text = text.replace("PARTSINPUT", generateStateParams(participants))
                text = text.replace("PARTSLIST", generateStateList(participants))
                text = text.replace("SCHEMAINPUT", generateStateSchema(participants))
                generatingFile = File(contractsDir + "state/GeneratedIDState.kt")
            } else if(file == contractsDir + "schema/ID.kt"){
                text = text.replace("_INPUTS_", generateInputs(participants))
                text = text.replace("tables", generateTables(participants))
                generatingFile = File(contractsDir + "schema/ID.kt")
            } else if(file == workflowDir + "/ExampleFlow.kt"){
                text = text.replace("    _CHOREOTASKS_", generateFlow(tasks, doc, taskNodes, contractId, participants))
                generatingFile = File(workflowDir + "/ExampleFlow.kt")
            } else if(file == directory + "/workflows-kotlin/build.gradle") {
                text = text.replace("partsNodes", generateNodes(participants))
                generatingFile = File(directory + "/workflows-kotlin/build.gradle")
            } else if(file == directory + "/contracts-kotlin/build.gradle") {
                generatingFile = File(directory + "/contracts-kotlin/build.gradle")
            } else if(file == serverDir + "/MainController.kt"){
                text = text.replace("_AdditionalFunctions_", generateRPCConnection(tasks, doc, taskNodes, participants))
                text = text.replace("_FLOWS_", generateFlowImport(tasks, doc, taskNodes, contractId))
                generatingFile = File(serverDir + "/MainController.kt")
            } else if(file == serverDir + "/NodeRPCConnection.kt"){
                generatingFile = File(serverDir + "/NodeRPCConnection.kt")
            } else if(file == serverDir + "/Server.kt"){
                generatingFile = File(serverDir + "/Server.kt")
            } else if(file == directory + "/clients/build.gradle") {
                text = text.replace("_runSever_", serverBuild(participants, contractId))
                generatingFile = File(directory + "/clients/build.gradle")
            } else if(file == directory + "/gradle.properties") {
                generatingFile = File(directory + "/gradle.properties")
            }
            generatingFile.writeText(text)
            if(file == contractsDir + "contract/GeneratedIDContract.kt"){
                generatingFile.renameTo(File(contractsDir + "contract/Generated" + contractId + "Contract.kt"))
            }   else if(file == contractsDir + "state/GeneratedIDState.kt"){
                generatingFile.renameTo(File(contractsDir + "state/Generated" + contractId + "State.kt"))
            }   else if(file == contractsDir + "schema/ID.kt"){
                generatingFile.renameTo(File(contractsDir + "schema/" + contractId + ".kt"))
            }
        }
    }

}

fun main(args: Array<String>) {
    val xmlReader = XmlReader("choreo.bpmn")
    val doc = xmlReader.readXml()
    val participantNodes = xmlReader.getElementValuesByAttributeName(doc, "bpmn2:participant")
    val participants = mutableSetOf<String>()
    for(i in 0..participantNodes.length - 1) {
        val node = participantNodes.item(i)
        participants.add(xmlReader.getValueOfNode(node, "id"))
    }
    var server = ""
    participants.forEach { e -> server += "run" + e.capitalize() + "Server\n"}
    server.trim()
    val deployServer = File("deployServer.txt")
    deployServer.writeText(server)

    val taskNodes = xmlReader.getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
    val tasks = mutableSetOf<String>()
    for(i in 0..taskNodes.length - 1) {
        val node = taskNodes.item(i)
        tasks.add(xmlReader.getValueOfNode(node, "name"))
    }
    xmlReader.generateContractFile(doc, participants, tasks)
}
