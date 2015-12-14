package edu.umd.lib.fcrepo.bootstrap.data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.http.client.ClientProtocolException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Fedora2Item {

  private static final Logger LOGGER = LoggerFactory.getLogger(Fedora2Item.class);

  private final String FEDORA2URL;

  private final String GET_TYPE = "/doInfo";
  private final String GET_UMDM = "/umdm";
  private final String GET_UMAM = "/umam";
  private final String GET_RELS = "/rels-mets";

  private final DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
  private final DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();

  private final List<Fedora2Item> containingCollection = new ArrayList<Fedora2Item>();
  private final List<ItemPart> parts = new ArrayList<ItemPart>();

  private final String PID;

  private String title;

  private String type;

  private String subject;

  private String mediaType;

  private String summary;

  public Fedora2Item(String fedora2Url, String pid) throws IOException, ParserConfigurationException,
      SAXException, XPathExpressionException {
    this.PID = pid;
    this.FEDORA2URL = fedora2Url;
    initialize();
  }

  private void initialize() throws IOException, SAXException, ParserConfigurationException, XPathExpressionException {
    // Initialize Metadata
    this.type = getFirstXMLNodeValuefromUrl(FEDORA2URL + PID + GET_TYPE, "type");
    this.title = getXMLNodeValueWithAttrfromUrl(FEDORA2URL + PID + GET_UMDM, "title", "type", "main");
    this.subject = getXMLNodeValueWithAttrfromUrl(FEDORA2URL + PID + GET_UMDM, "subject", "type", "topical");
    this.mediaType = getFirstXMLNodeValuefromUrl(FEDORA2URL + PID + GET_UMDM, "form");
    this.summary = getXMLNodeValueWithAttrfromUrl(FEDORA2URL + PID + GET_UMDM, "description", "type", "summary");

    // END: Initialize Metadata

    // Initialize Relationships (Parent collections && Parts)
    Document relsDoc = dBuilder.parse(FEDORA2URL + PID + GET_RELS);
    relsDoc.getDocumentElement().normalize();
    List<String> collectionFileIDs = new ArrayList<String>();
    Map<String, ItemPart> partFileIdMap = new HashMap<String, ItemPart>();
    NodeList structNodes = relsDoc.getElementsByTagName("structMap");
    for (int index = 0; index < structNodes.getLength(); index++) {
      Node node = getFirstNonTextChild(structNodes.item(index));
      if (node != null && node.getAttributes().getNamedItem("ID").getTextContent().equals("rels")) {
        NodeList divNodes = node.getChildNodes();
        for (int divIndex = 0; divIndex < divNodes.getLength(); divIndex++) {
          Node divNode = divNodes.item(divIndex);
          if (divNode.getNodeType() != Node.ELEMENT_NODE) {
            continue;
          }
          if (divNode.getAttributes().getNamedItem("ID").getTextContent().equals("isMemberOfCollection")) {
            collectionFileIDs = getFileIDs(divNode);
          } else if (divNode.getAttributes().getNamedItem("ID").getTextContent().equals("hasPart")) {
            NodeList fptrNodes = divNode.getChildNodes();
            for (int fptrIndex = 0; fptrIndex < fptrNodes.getLength(); fptrIndex++) {
              Node fptrNode = fptrNodes.item(fptrIndex);
              if (fptrNode.getNodeType() == Node.ELEMENT_NODE
                  && fptrNode.getAttributes().getNamedItem("FILEID") != null) {
                String id = fptrNode.getAttributes().getNamedItem("FILEID").getTextContent();
                if (partFileIdMap.containsKey(id)) {
                  partFileIdMap.put(id, new ItemPart());
                }
              }
            }
          }
        }
      } else if (node != null && node.getAttributes().getNamedItem("ID") != null) {
        String type = node.getAttributes().getNamedItem("ID").getTextContent();
        NodeList divNodes = node.getChildNodes();
        for (int divIndex = 0; divIndex < divNodes.getLength(); divIndex++) {
          Node divNode = divNodes.item(divIndex);
          if (divNode.getNodeType() != Node.ELEMENT_NODE) {
            continue;
          }
          Node fptrNode = findFptrNode(divNode);
          if (fptrNode != null && fptrNode.getAttributes().getNamedItem("FILEID") != null) {
            String id = fptrNode.getAttributes().getNamedItem("FILEID").getTextContent();
            boolean hasLabel = divNode.getAttributes().getNamedItem("LABEL") != null;
            boolean hasOrder = divNode.getAttributes().getNamedItem("ORDER") != null;
            if (partFileIdMap.containsKey(id)) {
              partFileIdMap.get(id).setType(type);
              if (hasLabel) {
                partFileIdMap.get(id).setLabel(divNode.getAttributes().getNamedItem("LABEL").getTextContent());
              }
              if (hasOrder) {
                partFileIdMap.get(id).setOrder(divNode.getAttributes().getNamedItem("ORDER").getTextContent());
              }
            } else {
              ItemPart part = new ItemPart();
              part.setFileId(id);
              part.setType(type);
              if (hasLabel) {
                part.setLabel(divNode.getAttributes().getNamedItem("LABEL").getTextContent());
              }
              if (hasOrder) {
                part.setOrder(divNode.getAttributes().getNamedItem("ORDER").getTextContent());
              }
              partFileIdMap.put(id, part);
            }
          }
        }
      }
    }
    NodeList fileNodes = relsDoc.getElementsByTagName("file");
    for (int index = 0; index < fileNodes.getLength(); index++) {
      Node node = fileNodes.item(index);
      String fileId = node.getAttributes().getNamedItem("ID").getTextContent();
      String pid = getFirstNonTextChild(node).getAttributes().getNamedItem("xlink:href").getTextContent();
      if (collectionFileIDs.contains(fileId)) {
        containingCollection.add(new Fedora2Item(FEDORA2URL, pid));
        LOGGER.trace("Adding containing collection: {} to {}.", pid, PID);
      } else if (partFileIdMap.containsKey(fileId)) {
        String fileName = getFirstXMLNodeValuefromUrl(FEDORA2URL + pid + GET_UMAM, "identifier");
        partFileIdMap.get(fileId).setPid(pid);
        partFileIdMap.get(fileId).setPartUrl(FEDORA2URL + pid);
        partFileIdMap.get(fileId).setFileName(fileName);
        parts.add(partFileIdMap.get(fileId));
        LOGGER.trace("Adding part {} to {}.", pid, PID);
      }
    }
    // END: Initialize Relationships (Parent collections && Parts)

  }

  private Node findFptrNode(Node divNode) {
    Node node = divNode;
    do {
      node = getFirstNonTextChild(node);
      if (node.getNodeName().equals("fptr")) {
        return node;
      }
    } while (node != null);
    return node;
  }

  private Node getFirstNonTextChild(Node item) {
    NodeList list = item.getChildNodes();
    for (int index = 0; index < list.getLength(); index++) {
      if (!list.item(index).getNodeName().equals("#text")) {
        return list.item(index);
      }
    }
    return null;
  }

  private List<String> getFileIDs(Node divNode) {
    List<String> ids = new ArrayList<String>();
    NodeList fptrNodes = divNode.getChildNodes();
    for (int fptrIndex = 0; fptrIndex < fptrNodes.getLength(); fptrIndex++) {
      Node fptrNode = fptrNodes.item(fptrIndex);
      if (fptrNode.getNodeType() == Node.ELEMENT_NODE && fptrNode.getAttributes().getNamedItem("FILEID") != null) {
        ids.add(fptrNode.getAttributes().getNamedItem("FILEID").getTextContent());
      }
    }
    return ids;
  }

  private String getFirstXMLNodeValuefromUrl(String url, String tagName) throws ClientProtocolException, IOException,
      SAXException {
    Document doc = dBuilder.parse(url);
    doc.getDocumentElement().normalize();
    NodeList elementList = doc.getElementsByTagName(tagName);
    if (elementList.getLength() > 0) {
      LOGGER.trace("Got {} for tag: {}", elementList.item(0).getTextContent(), tagName);
      return elementList.item(0).getTextContent();
    } else {
      LOGGER.trace("Tag: {} not found!", tagName);
      return null;
    }
  }

  private String getXMLNodeValueWithAttrfromUrl(String url, String tagName, String attrName, String attrValue)
      throws SAXException,
      IOException {
    Document doc = dBuilder.parse(url);
    doc.getDocumentElement().normalize();
    NodeList elementList = doc.getElementsByTagName(tagName);
    if (elementList.getLength() > 0) {
      String value = "";
      for (int index = 0; index < elementList.getLength(); index++) {
        if (elementList.item(index).getAttributes().getNamedItem(attrName) != null) {
          if (elementList.item(index).getAttributes().getNamedItem(attrName).getTextContent().equals(attrValue)) {
            value += elementList.item(index).getTextContent() + " ";
          }
        }
      }
      value = value.trim();
      LOGGER.trace("Got {} for tag: {}", value, tagName);
      return value;
    } else {
      LOGGER.trace("Tag: {} not found!", tagName);
      return null;
    }
  }

  public String getType() {
    return this.type;
  }

  public String getTitle() {
    return this.title;
  }

  public String getSubject() {
    return this.subject;
  }

  public String getMediaType() {
    return this.mediaType;
  }

  public String getSummary() {
    return this.summary;
  }

  public List<Fedora2Item> getContainingCollections() {
    return this.containingCollection;
  }

  public List<ItemPart> getParts() {
    return this.parts;
  }

  public String getPid() {
    return this.PID;
  }

}
