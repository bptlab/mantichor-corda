
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

    fun getInitParticipanForTask(taskNode: Node, participants: NodeList) : Node?{
      for(i in 0..participants.length - 1){
        if(getValueOfNode(participants.item(i), "id") == getValueOfNode(taskNode, "initiatingParticipantRef")){
          return participants.item(i)
        }
      }
      return null
    }

    fun getInitParticipanForTask(doc: Document, choreoTasks: ArrayList<Node>) : ArrayList<Node> {
      val participantNodes = getElementValuesByAttributeName(doc, "participant")
      val initParticipants = ArrayList<Node>()
      for(i in 1..choreoTasks.size - 2){
        val participant = getInitParticipanForTask(choreoTasks.get(i), participantNodes)
        initParticipants.add(participant!!)
        println(getValueOfNode(participant, "name"))
      }
      return initParticipants
    }

    fun generateContractFile(doc: Document, participants: MutableSet<String>) {
      val contractId = getValueOfNode(getElementValuesByAttributeName(doc, "choreography").item(0), "id")
      val files = arrayOf("contractTemplate.kt", "stateTemplate.kt")
      for (file in files) {
          val f = File(file)
          var text = f.readText()
          println(text)
          text = text.replace("ID", contractId)
          text = text.replace("ParticipantA", participants.elementAt(0))
          text = text.replace("ParticipantB",participants.elementAt(1))
          var generatingFile : File
          if(file == "contractTemplate.kt"){
            generatingFile = File("./" + contractId + "/" + contractId + "Contract.kt")
          } else {
            generatingFile = File("./" + contractId + "/" + contractId + "State.kt")
          }
          generatingFile.writeText(text)
          println(generatingFile.readText())
      }


    }

}
fun main(args: Array<String>) {
  val xmlReader = XmlReader("./Pizza-Choreo_simple.bpmn")
  val doc = xmlReader.readXml()
  val participantNodes = xmlReader.getElementValuesByAttributeName(doc, "participant")
  val participants = mutableSetOf<String>()
  for(i in 0..participantNodes.length - 1) {
    val node = participantNodes.item(i);
    participants.add(xmlReader.getValueOfNode(node, "name"));
  }
  participants.forEach { e -> println(e)}

  val taskNodes = xmlReader.getElementValuesByAttributeName(doc, "choreographyTask")
  val tasks = mutableSetOf<String>()
  for(i in 0..taskNodes.length - 1) {
    val node = taskNodes.item(i);
    tasks.add(xmlReader.getValueOfNode(node, "name"));
  }
  tasks.forEach { e -> println(e)}
  val foundNode = xmlReader.getNodeById(taskNodes, "sid-99ABCD46-49C9-4AD1-94B2-788BA9ACA06A")
  if(foundNode != null) {
    println(xmlReader.getValueOfNode(foundNode, "name"))
  }
  println()
  val choreoTasks = xmlReader.generateChoreoTaskOrder(doc)
  val initParticipants = xmlReader.getInitParticipanForTask(doc, choreoTasks)
  val messages = ArrayList<String>()
  for(i in 0..initParticipants.size - 1){
    val extensionElement = xmlReader.getChildNodeByName(initParticipants.get(i), "extensionElements")
    val messageNode = xmlReader.getChildNodeByName(extensionElement!!, "signavio:signavioMessageName")
    val message = xmlReader.getValueOfNode(messageNode!!, "name")
    println(message)
    messages.add(message)
  }
  xmlReader.generateContractFile(doc)
}
