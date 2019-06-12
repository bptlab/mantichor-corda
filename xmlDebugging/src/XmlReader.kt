
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
        val taskNodes = getElementValuesByAttributeName(doc, "choreographyTask")
        val sequenceFlows = getElementValuesByAttributeName(doc, "sequenceFlow")
        val endNodes = getElementValuesByAttributeName(doc, "endEvent")
        val startNode = getElementValuesByAttributeName(doc, "startEvent").item(0)
        var outgoingNode = getChildNodeByName(startNode, "outgoing")
        choreo.add(startNode)
        var sequenceFlow = getNodeById(sequenceFlows, outgoingNode!!.textContent)
        while(getNodeById(taskNodes, getValueOfNode(sequenceFlow!!, "targetRef")) != null) {
            val nextNode = getNodeById(taskNodes, getValueOfNode(sequenceFlow, "targetRef"))
            choreo.add(nextNode!!)
            println(getValueOfNode(nextNode, "name"))
            outgoingNode = getChildNodeByName(nextNode, "outgoing")
            sequenceFlow = getNodeById(sequenceFlows, outgoingNode!!.textContent)
        }
        if(getNodeById(endNodes, getValueOfNode(sequenceFlow, "targetRef")) != null){
            choreo.add(getNodeById(endNodes, getValueOfNode(sequenceFlow, "targetRef"))!!)
            println("We did it reddit")
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
        val participantNodes = getElementValuesByAttributeName(doc, "participant")
        val initParticipants = ArrayList<Node>()
        for(i in 1..choreoTasks.size - 2){
            val participant = getInitParticipantForTask(choreoTasks.get(i), participantNodes)
            initParticipants.add(participant!!)
            println(getValueOfNode(participant, "name"))
        }
        return initParticipants
    }

    fun generateStateHandling(commands: MutableSet<String>) : String {
        val indention = "                "
        var handleString = ""
        for(i in 0..commands.size - 1) {
            handleString += indention + "is Commands." + commands.elementAt(i) + " -> {\n"
            handleString += indention + "    val input = inputs.single()\n"
            handleString += indention + "    requireThat {\n"
            handleString += indention + "        \"the state is propagated\" using (outputs.size == 1)\n"
            handleString += indention + "    }\n"
            handleString += indention + "}\n"
        }
        return  handleString
    }

    fun generateFunctions(tasks: MutableSet<String>, doc: Document, nodeTasks: NodeList) : String {
        val participants = getElementValuesByAttributeName(doc, "participant")
        val indention = "        "
        var functions = ""
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
        }
        return functions
    }

    fun crawlChilds(node: Node, participants: NodeList) : MutableSet<String>{
        val partsIDs = mutableSetOf<String>()
        val childs = node.childNodes

        for(i in 0..childs.length -1) {
            if(childs.item(i).nodeName == "participantRef") {
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

    fun generateContractFile(doc: Document, participants: MutableSet<String>, tasks: MutableSet<String>) {
        val contractId = getValueOfNode(getElementValuesByAttributeName(doc, "choreography").item(0), "id")
        val files = arrayOf("src/contractTemplate.txt", "src/stateTemplate.txt")
        val subdirectory = File("./generated_contracts/" + contractId + "/")
        val taskNodes = getElementValuesByAttributeName(doc, "choreographyTask")
        subdirectory.mkdirs()
        for (file in files) {
            val f = File(file)
            var text = f.readText()
            println(text)
            text = text.replace("ID", contractId)
            text = text.replace("ParticipantA", participants.elementAt(0))
            text = text.replace("ParticipantB",participants.elementAt(1))
            var generatingFile : File
            if(file == "src/contractTemplate.txt"){
                val commands = mutableSetOf<String>()
                var commandText = "\n        class Create : Commands"
                for (i in 0..tasks.size - 1) {
                    val command = tasks.elementAt(i)
                    val camelCaseCommand = generateCamelCaseName(command)
                    commands.add(camelCaseCommand)
                    commandText = commandText + "\n        class " + camelCaseCommand + " : TypeOnlyCommandData(), Commands"
                }
                text = text.replace("\n        class AdditionalCommands : Commands", commandText)
                text = text.replace("                HANDLECOMMANDS", generateStateHandling(commands))
                text = text.replace("    AdditionalFunctions", generateFunctions(tasks, doc, taskNodes))
                generatingFile = File("./generated_contracts/" + contractId + "/" + contractId + "Contract.kt")
            } else {
                generatingFile = File("./generated_contracts/" + contractId + "/" + contractId + "State.kt")
            }
            generatingFile.writeText(text)
            println(generatingFile.readText())
        }


    }

}
fun main(args: Array<String>) {
    val xmlReader = XmlReader("src/Pizza-Choreo_simple.bpmn")
    val doc = xmlReader.readXml()
    val participantNodes = xmlReader.getElementValuesByAttributeName(doc, "participant")
    val participants = mutableSetOf<String>()
    for(i in 0..participantNodes.length - 1) {
        val node = participantNodes.item(i);
        participants.add(xmlReader.getValueOfNode(node, "name"))
    }
    participants.forEach { e -> println(e)}

    val taskNodes = xmlReader.getElementValuesByAttributeName(doc, "choreographyTask")
    val tasks = mutableSetOf<String>()
    for(i in 0..taskNodes.length - 1) {
        val node = taskNodes.item(i);
        tasks.add(xmlReader.getValueOfNode(node, "name"))
    }
    tasks.forEach { e -> println(e)}
    val foundNode = xmlReader.getNodeById(taskNodes, "sid-99ABCD46-49C9-4AD1-94B2-788BA9ACA06A")
    if(foundNode != null) {
        println(xmlReader.getValueOfNode(foundNode, "name"))
    }
    println()
    val choreoTasks = xmlReader.generateChoreoTaskOrder(doc)
    val initParticipants = xmlReader.getInitParticipantForTasks(doc, choreoTasks)
    val messages = ArrayList<String>()
    for(i in 0..initParticipants.size - 1){
        val extensionElement = xmlReader.getChildNodeByName(initParticipants.get(i), "extensionElements")
        val messageNode = xmlReader.getChildNodeByName(extensionElement!!, "signavio:signavioMessageName")
        val message = xmlReader.getValueOfNode(messageNode!!, "name")
        println(message)
        messages.add(message)
    }
    xmlReader.generateContractFile(doc, participants, tasks)
}
