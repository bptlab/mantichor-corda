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

fun getFollowTasksForTask(taskNode: Node, doc: Document) : MutableSet<String> {
    val tasks =  mutableSetOf<String>()
    val childs = taskNode.childNodes
    var messageFlowID = ""
    for(i in 0..childs.length -1) {
        if(childs.item(i).nodeName == "bpmn2:outgoing") {
            messageFlowID = childs.item(i).textContent
        }
    }
    val sequenceFlows = getElementValuesByAttributeName(doc, "bpmn2:sequenceFlow")
    var targetRefID = ""
    for(i in 0..sequenceFlows.length - 1) {
        val node = sequenceFlows.item(i)
        if(getValueOfNode(node, "id") == messageFlowID){
            targetRefID = getValueOfNode(node, "targetRef")
        }
    }
    val tasksNodes =  getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
    for(i in 0..tasksNodes.length - 1) {
        val node = tasksNodes.item(i)
        if(getValueOfNode(node, "id") == targetRefID){
            tasks.add(getValueOfNode(node, "name"))
        }
    }
    return tasks
}

fun main(args: Array<String>) {
    val task = args[0]
    val doc = readXml("choreo.bpmn")

    val taskNodes = getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
    var availableTasks = mutableSetOf<String>()
    for(i in 0..taskNodes.length - 1) {
        val node = taskNodes.item(i)
        if(getValueOfNode(node, "name") == task){
            availableTasks = getFollowTasksForTask(node, doc)
        }
    }
    val generatingFile = File("tasks.txt")
    var taskPrints = ""
    availableTasks.forEach { e -> taskPrints += e}
    generatingFile.writeText(taskPrints)

}
