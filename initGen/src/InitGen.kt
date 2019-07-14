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

fun main(args: Array<String>) {
    val task = args[0]
    var port = 0
    var parties = ""
    val doc = readXml("choreo.bpmn")
    val participants = getElementValuesByAttributeName(doc, "bpmn2:participant")
    if(task == "init") {
        port = 50005
        for(j in 1..participants.length - 1) {
            parties += "\npartyName" + j + "=O=" + getValueOfNode(participants.item(j), "name") +
                    ", L=London, C=GB"
        }
    } else {
        val taskNodes = getElementValuesByAttributeName(doc, "bpmn2:choreographyTask")
        for(i in 0..taskNodes.length - 1) {
            val node = taskNodes.item(i)
            if(getValueOfNode(node, "name") == task){
                val initRef = getValueOfNode(node, "initiatingParticipantRef")
                for(j in 0..participants.length - 1) {
                    if(getValueOfNode(participants.item(j), "id") == initRef) {
                        port = 50005 + j*3
                    } else {
                        parties += "\npartyName" + j + "=O=" + getValueOfNode(participants.item(j), "name") +
                                ", L=London, C=GB"
                    }
                }
            }
        }
    }
    val generatingFile = File("changeRequest.txt")
    val requestValue = port.toString() + parties
    generatingFile.writeText(requestValue)
}
