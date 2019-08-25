import org.w3c.dom.Document
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

fun getElementValuesByAttributeName(doc: Document, attributeName: String): NodeList {
    return doc.getElementsByTagName(attributeName)
}

fun readXml(xmlPath: String): Document {
    val xmlFile = File(xmlPath)
    val dbFactory = DocumentBuilderFactory.newInstance()
    val dBuilder = dbFactory.newDocumentBuilder()
    return dBuilder.parse(xmlFile)
}

fun getValueOfNode(node: Node, valueName: String) : String {
    return node.attributes.getNamedItem(valueName).nodeValue
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

fun checkIfReachableState(state: String, checkstring: String, index: Int) : Boolean {
    val subStates = state.split(",")
    val mode = checkstring.get(0)
    val subCheckStates = checkstring.drop(1).split("%")[0].split(",")
    for(i in 0..subStates.size - 1) {
        if(subStates[i].split("_").contains("" + index) ) {
            return false
        }
    }
    subLoop@ for(i in 0..subCheckStates.size - 1) {
        var subFound = false
        for(j in 0..subStates.size - 1) {
            if(subStates[j].split("_").contains(subCheckStates[i])){
                subFound = true
                if(mode != 'p'){
                    break@subLoop
                }
            }
        }
        if(!subFound) {
            return false
        }
    }
    return true
}

fun main(args: Array<String>) {
    val state = args[0]
    val doc = readXml("choreo.bpmn")
    val taskNodes = getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
    var taskPrints = ""
    for(i in 0..taskNodes.length - 1) {
        val correspondingNode = taskNodes.item(i)
        val checkstring = checkIfIncommingNodeIsGateway(doc, correspondingNode)
        if(checkIfReachableState(state, checkstring, i+1)){
            taskPrints += getValueOfNode(correspondingNode, "name") + "\n"
        }
    }
    val generatingFile = File("tasks.txt")
    generatingFile.writeText(taskPrints)

}
