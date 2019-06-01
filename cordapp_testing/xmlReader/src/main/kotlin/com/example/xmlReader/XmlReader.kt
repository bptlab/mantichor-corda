package com.example.xmlReader

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

}