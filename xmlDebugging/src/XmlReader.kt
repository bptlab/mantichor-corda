
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

    fun getNodeById(list: NodeList, id: String) : Node? {
        for(i in 0..list.length-1) {
            val node = list.item(i)
            if(getValueOfNode(node, "id") == id){
                return node
            }
        }
        return null
    }

    fun getChildNodeByName(node: Node, name: String) : Node? {
        val childs = node.childNodes
        for(i in 0..childs.length -1) {
            if(childs.item(i).nodeName == name) {
                return childs.item(i)
            }
        }
        return null
    }

    fun generateChoreoTaskOrder(doc: Document) : ArrayList<Node> {
        val choreo = ArrayList<Node>()
        val taskNodes = getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
        val sequenceFlows = getElementValuesByAttributeName(doc, "bpmn2:sequenceFlow")
        val endNodes = getElementValuesByAttributeName(doc, "bpmn2:endEvent")
        val startNode = getElementValuesByAttributeName(doc, "bpmn2:startEvent").item(0)
        var outgoingNode = getChildNodeByName(startNode, "bpmn2:outgoing")
        choreo.add(startNode)
        var sequenceFlow = getNodeById(sequenceFlows, outgoingNode!!.textContent)
        while(getNodeById(taskNodes, getValueOfNode(sequenceFlow!!, "targetRef")) != null) {
            val nextNode = getNodeById(taskNodes, getValueOfNode(sequenceFlow, "targetRef"))
            choreo.add(nextNode!!)
            println(getValueOfNode(nextNode, "name"))
            outgoingNode = getChildNodeByName(nextNode, "bpmn2:outgoing")
            sequenceFlow = getNodeById(sequenceFlows, outgoingNode!!.textContent)
        }
        if(getNodeById(endNodes, getValueOfNode(sequenceFlow, "targetRef")) != null){
            choreo.add(getNodeById(endNodes, getValueOfNode(sequenceFlow, "targetRef"))!!)
        }
        return choreo
    }

    fun getInitParticipantForTask(taskNode: Node, participants: NodeList) : Node?{
        for(i in 0..participants.length - 1){
            if(getValueOfNode(participants.item(i), "id") == getValueOfNode(taskNode, "initiatingParticipantRef")){
                return participants.item(i)
            }
        }
        return null
    }

    fun getInitParticipantForTasks(doc: Document, choreoTasks: ArrayList<Node>) : ArrayList<Node> {
        val participantNodes = getElementValuesByAttributeName(doc, "bpmn2:participant")
        val initParticipants = ArrayList<Node>()
        for(i in 1..choreoTasks.size - 2){
            val participant = getInitParticipantForTask(choreoTasks.get(i), participantNodes)
            initParticipants.add(participant!!)
            println(getValueOfNode(participant, "name"))
        }
        return initParticipants
    }

    fun generateStateHandling(commands: MutableSet<String>) : String {
        val indention = "            "
        var handleString = ""
        for(i in 0..commands.size - 1) {
            handleString += indention + "is Commands." + commands.elementAt(i) + " -> {\n"
            handleString += indention + "    val input = tx.inputs.single()\n"
            handleString += indention + "    requireThat {\n"
            handleString += indention + "        \"the state is propagated\" using (tx.outputs.size == 1)\n"
            handleString += indention + "    }\n"
            handleString += indention + "}\n"
        }
        return  handleString
    }

    fun generateFunctions(tasks: MutableSet<String>, doc: Document, nodeTasks: NodeList) : String {
        var functions = ""
        /*val participants = getElementValuesByAttributeName(doc, "participant")
        val indention = "        "

        for(i in 0..tasks.size - 1) {
            val correspondingNode = nodeTasks.item(i)
            val parts = crawlChilds(correspondingNode, participants)
            val command = tasks.elementAt(i)
            val camelCaseCommand = generateCamelCaseName(command)
            functions += "    fun generate" + camelCaseCommand.capitalize() + " ("
            for(j in 0..parts.size - 1) {
                functions += parts.elementAt(j) + ": PartyAndReference"
                if(j < parts.size - 1) {
                    functions += ", "
                }
            }
            functions += ") : TransactionBuilder {\n"
            functions += indention + "val state = State("
                    for(j in 0..parts.size - 1) {
                        functions += parts.elementAt(j) + ", " +
                        parts.elementAt(j) + ".party"
                if(j < parts.size - 1) {
                    functions += ", "
                }
            }
            functions += ")\n" +
                    indention + "val stateAndContract = StateAndContract(state, CP_PROGRAM_ID)\n" +
                    indention + "return TransactionBuilder(notary = notary)" +
                    ".withItems(stateAndContract, Command(Commands." + camelCaseCommand + "(), "
            for(j in 0..parts.size - 1) {
                functions += parts.elementAt(j) + ".party.owningKey"
                if(j < parts.size - 1) {
                    functions += ", "
                }
            }
            functions += "))\n    }\n\n"
        }*/
        return functions
    }

    fun crawlChilds(node: Node, participants: NodeList) : MutableSet<String>{
        val partsIDs = mutableSetOf<String>()
        val childs = node.childNodes

        for(i in 0..childs.length -1) {
            if(childs.item(i).nodeName == "bpmn2:participantRef") {
                partsIDs.add(childs.item(i).textContent)
            }
        }
        val parts = mutableSetOf<String>()
        for(i in 0..participants.length - 1) {
            for(j in 0..partsIDs.size - 1){
                if(getValueOfNode(participants.item(i), "id") == partsIDs.elementAt(j)){
                    parts.add(getValueOfNode(participants.item(i), "name"))
                }
            }
        }
        return parts
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

    fun generateFlow(tasks: MutableSet<String>, doc: Document, nodeTasks: NodeList, contractId: String) : String {
        var flow = ""
        val participants = getElementValuesByAttributeName(doc, "bpmn2:participant")
        flow += "    @InitiatingFlow\n" +
                "    @StartableByRPC\n" +
                "    class " + "Initiator" + "(val otherParty: Party) : FlowLogic<SignedTransaction>() {\n"
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
                "        }\n"
        flow += "        @Suspendable\n" +
                "        override fun call(): SignedTransaction {\n" +
                "            // Obtain a reference to the notary we want to use.\n" +
                "            val notary = serviceHub.networkMapCache.notaryIdentities[0]\n" +
                "\n" +
                "            // Stage 1.\n" +
                "            progressTracker.currentStep = GENERATING_TRANSACTION\n" +
                "            // Generate an unsigned transaction.\n" +
                "            val " + contractId + "State = Generated" + contractId + "State(serviceHub.myInfo.legalIdentities.first(), otherParty, 0)\n" +
                "            val txCommand = Command(Generated" + contractId + "Contract.Commands." + "Create" + "(), " + contractId + "State.participants.map { it.owningKey })\n" +
                "            val txBuilder = TransactionBuilder(notary)\n" +
                "                    .addOutputState(pizzaState, Generated" + contractId + "Contract.ID)\n" +
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
                "            // Send the state to the counterparty, and receive it back with their signature.\n" +
                "            val otherPartySession = initiateFlow(otherParty)\n" +
                "            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), GATHERING_SIGS.childProgressTracker()))\n" +
                "\n" +
                "            // Stage 5.\n" +
                "            progressTracker.currentStep = FINALISING_TRANSACTION\n" +
                "            // Notarise and record the transaction in both parties' vaults.\n" +
                "            return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))\n" +
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
            val parts = crawlChilds(correspondingNode, participants)
            val command = tasks.elementAt(i)
            val camelCaseCommand = generateCamelCaseName(command)
            val initFlow = parts.elementAt(0) + camelCaseCommand.capitalize() + "Flow"
            val reactingFlow = parts.elementAt(1) + camelCaseCommand.capitalize() + "Flow"
            flow += "    @InitiatingFlow\n" +
                    "    @StartableByRPC\n" +
                    "    class " + initFlow + "(val otherParty: Party) : FlowLogic<SignedTransaction>() {\n"
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
                    "        }\n"
            flow += "        @Suspendable\n" +
                    "        override fun call(): SignedTransaction {\n" +
                    "            // Obtain a reference to the notary we want to use.\n" +
                    "            val notary = serviceHub.networkMapCache.notaryIdentities[0]\n" +
                    "\n" +
                    "            // Stage 1.\n" +
                    "            progressTracker.currentStep = GENERATING_TRANSACTION\n" +
                    "            // Generate an unsigned transaction.\n" +
                    "            val " + contractId + "State = Generated" + contractId + "State(serviceHub.myInfo.legalIdentities.first(), otherParty, " + (i + 1) + ")\n" +
                    "            val txCommand = Command(Generated" + contractId + "Contract.Commands." + camelCaseCommand.capitalize() + "(), " + contractId + "State.participants.map { it.owningKey })\n" +
                    "            val txBuilder = TransactionBuilder(notary)\n" +
                    "                    .addOutputState(pizzaState, Generated" + contractId + "Contract.ID)\n" +
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
                    "            // Send the state to the counterparty, and receive it back with their signature.\n" +
                    "            val otherPartySession = initiateFlow(otherParty)\n" +
                    "            val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), GATHERING_SIGS.childProgressTracker()))\n" +
                    "\n" +
                    "            // Stage 5.\n" +
                    "            progressTracker.currentStep = FINALISING_TRANSACTION\n" +
                    "            // Notarise and record the transaction in both parties' vaults.\n" +
                    "            return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession), FINALISING_TRANSACTION.childProgressTracker()))\n" +
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

    fun generateRPCConnection(tasks: MutableSet<String>, doc: Document, nodeTasks: NodeList, contractId: String) : String {
        var rpcConnection = ""
        val participants = getElementValuesByAttributeName(doc, "bpmn2:participant")
        for(i in 0..tasks.size - 1) {
            val correspondingNode = nodeTasks.item(i)
            val parts = crawlChilds(correspondingNode, participants)
            val command = tasks.elementAt(i)
            val camelCaseCommand = generateCamelCaseName(command)
            val initFlow = parts.elementAt(0) + camelCaseCommand.capitalize() + "Flow"
            rpcConnection += "@PostMapping(value = [ \"" + camelCaseCommand + "\" ], produces = [ TEXT_PLAIN_VALUE ], headers = [ \"Content-Type=application/x-www-form-urlencoded\" ])\n" +
                    "    fun " + camelCaseCommand + "(request: HttpServletRequest): ResponseEntity<String> {\n" +
                    "        val partyName = request.getParameter(\"partyName\")\n" +
                    "        if(partyName == null){\n" +
                    "            return ResponseEntity.badRequest().body(\"Query parameter 'partyName' must not be null.\\n\")\n" +
                    "        }\n" +
                    "        val partyX500Name = CordaX500Name.parse(partyName)\n" +
                    "        val otherParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body(\"Party named \$partyName cannot be found.\\n\")\n" +
                    "\n" +
                    "        return try {\n" +
                    "            val signedTx = proxy.startTrackedFlow(::" + initFlow + ", otherParty).returnValue.getOrThrow()\n" +
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

    fun serverBuild(participants: MutableSet<String>) : String {
        var build = ""
        var ports = 5
        for(i in 0..participants.size-1) {
            build += "task run" + participants.elementAt(i) + "Server(type: JavaExec, dependsOn: jar) {\n" +
                    "    classpath = sourceSets.main.runtimeClasspath\n" +
                    "    main = 'com.generatedPizza.server.ServerKt'\n" +
                    "    args '--server.port=" + (ports + 50000) + "', '--config.rpc.host=localhost', '--config.rpc.port=" + (ports + 10000) + "', '--config.rpc.username=user1', '--config.rpc.password=test'\n" +
                    "}\n\n"
            ports += 3
        }
        return build
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
                                         directory + "/clients/build.gradle")
        val taskNodes = getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
        for (file in files) {
            val f = File(file)
            var text = f.readText()
            text = text.replace("_ID_", contractId)
            text = text.replace("ParticipantA", participants.elementAt(0))
            text = text.replace("ParticipantB",participants.elementAt(1))
            var generatingFile = File("ERROR")
            if(file == contractsDir + "contract/GeneratedIDContract.kt"){
                val commands = mutableSetOf<String>()
                var commandText = "\n        class Create : Commands"
                for (i in 0..tasks.size - 1) {
                    val command = tasks.elementAt(i)
                    val camelCaseCommand = generateCamelCaseName(command)
                    commands.add(camelCaseCommand)
                    commandText = commandText + "\n        class " + camelCaseCommand + " : Commands"
                }
                text = text.replace("\n        class AdditionalCommands : Commands", commandText)
                text = text.replace("            HANDLECOMMANDS", generateStateHandling(commands))
                text = text.replace("    AdditionalFunctions", generateFunctions(tasks, doc, taskNodes))
                generatingFile = File(contractsDir + "contract/GeneratedIDContract.kt")
            } else if(file == contractsDir + "state/GeneratedIDState.kt"){
                generatingFile = File(contractsDir + "state/GeneratedIDState.kt")
            } else if(file == contractsDir + "schema/ID.kt"){
                text = text.replace("_INPUTS_", generateInputs(participants))
                generatingFile = File(contractsDir + "schema/ID.kt")
            } else if(file == workflowDir + "/ExampleFlow.kt"){
                text = text.replace("    _CHOREOTASKS_", generateFlow(tasks, doc, taskNodes, contractId))
                generatingFile = File(workflowDir + "/ExampleFlow.kt")
            } else if(file == directory + "/workflows-kotlin/build.gradle") {
                text = text.replace("partsNodes", generateNodes(participants))
                generatingFile = File(directory + "/workflows-kotlin/build.gradle")
            } else if(file == directory + "/contracts-kotlin/build.gradle") {
                generatingFile = File(directory + "/contracts-kotlin/build.gradle")
            } else if(file == serverDir + "/MainController.kt"){
                text = text.replace("_AdditionalFunctions_", generateRPCConnection(tasks, doc, taskNodes, contractId))
                generatingFile = File(serverDir + "/MainController.kt")
            } else if(file == serverDir + "/NodeRPCConnection.kt"){
                generatingFile = File(serverDir + "/NodeRPCConnection.kt")
            } else if(file == serverDir + "/Server.kt"){
                generatingFile = File(serverDir + "/Server.kt")
            } else if(file == directory + "/clients/build.gradle") {
                text = text.replace("_runSever_", serverBuild(participants))
                generatingFile = File(directory + "/clients/build.gradle")
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
    val xmlReader = XmlReader("src/sequence.bpmn")
    val doc = xmlReader.readXml()
    val participantNodes = xmlReader.getElementValuesByAttributeName(doc, "bpmn2:participant")
    val participants = mutableSetOf<String>()
    for(i in 0..participantNodes.length - 1) {
        val node = participantNodes.item(i)
        participants.add(xmlReader.getValueOfNode(node, "name"))
    }
    participants.forEach { e -> println(e)}

    val taskNodes = xmlReader.getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
    val tasks = mutableSetOf<String>()
    for(i in 0..taskNodes.length - 1) {
        val node = taskNodes.item(i)
        tasks.add(xmlReader.getValueOfNode(node, "name"))
    }
    tasks.forEach { e -> println(e)}

    println()
    xmlReader.generateContractFile(doc, participants, tasks)
}
