package com.rhinoforms;

import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.rhinoforms.serverside.InputPojo;

public class DocumentHelper {

	private XPathFactory xPathFactory;
	
	private static final Logger LOGGER = Logger.getLogger(DocumentHelper.class);

	public DocumentHelper() {
		this.xPathFactory = XPathFactory.newInstance();
	}

	public void persistFormData(List<InputPojo> inputPOJOs, String documentBasePath, Document dataDocument) throws DocumentHelperException {
		for (InputPojo inputPojo : inputPOJOs) {
			String xPathString = documentBasePath + "/" + inputPojo.name.replaceAll("\\.", "/");
			Node node = lookupOrCreateNode(dataDocument, xPathString);
			node.setTextContent(inputPojo.value);
		}
	}
	
	public String resolveXPathIndexesForAction(String xpath, Map<String, String> actionParams, Document document) throws DocumentHelperException {
		Pattern xpathIndexPattern = Pattern.compile("(.+?)\\[([^0-9]+?)\\].*");
		Matcher matcher = xpathIndexPattern.matcher(xpath);
		if (matcher.matches()) {
			String group = matcher.group(matcher.groupCount());
			if (group.equals("next")) {
				String xpathToCount = xpath.substring(0, xpath.indexOf("[next]"));
				try {
					XPathExpression xpathToCountExpression = newXPath(xpathToCount);
					NodeList list = lookup(document, xpathToCountExpression);
					int listLength = list.getLength();
					xpath = xpath.replace("[next]", "[" + ++listLength + "]");
				} catch (XPathExpressionException e) {
					throw new DocumentHelperException("Failed to compile or evaluate XPath: " + xpathToCount, e);
				}
				return resolveXPathIndexesForAction(xpath, actionParams, document);
			} else if (actionParams.containsKey(group)) {
				xpath = xpath.replace("[" + group + "]", "[" + actionParams.get(group) + "]");
				return resolveXPathIndexesForAction(xpath, actionParams, document);
			} else {
				throw new DocumentHelperException("XPath index alias not recognised. Index alias:'" + group + "', XPath:'" + xpath + "', action params:'" + actionParams + "'");
			}
		} else {
			return xpath;
		}
	}
	
	private Node lookupOrCreateNode(Document dataDocument, String xPathString) throws DocumentHelperException {
		try {
			XPathExpression fullXPathExpression = newXPath(xPathString);
			NodeList fullPathNodeList = lookup(dataDocument, fullXPathExpression);
			if (fullPathNodeList.getLength() == 1) {
				return fullPathNodeList.item(0);
			} else if (fullPathNodeList.getLength() > 1) {
				throw new DocumentHelperException("XPath matches more than one node '" + xPathString + "'.");
			} else {
				String[] xPathStringParts = xPathString.split("/");
				Stack<String> xPathPartsStack = new Stack<String>();
				Collections.addAll(xPathPartsStack, xPathStringParts);
				Collections.reverse(xPathPartsStack);
				xPathPartsStack.pop(); // discard blank string from before first slash
				
				return recursiveCreateNode(dataDocument, "", dataDocument, xPathPartsStack);
			}
		} catch (XPathExpressionException e) {
			throw new DocumentHelperException(e);
		}
	}

	private Node recursiveCreateNode(Document doc, String progressiveXpath, Node currentNode, Stack<String> xPathPartsStack) throws XPathExpressionException, DocumentHelperException {
		String nodeToFindOrCreate = xPathPartsStack.pop();
		progressiveXpath += "/" + nodeToFindOrCreate;
		NodeList nodeSet = lookup(doc, newXPath(progressiveXpath));
		Node nextNode;
		if (nodeSet.getLength() == 0) {
			LOGGER.debug("Creating node at " + progressiveXpath);
			nextNode = doc.createElement(cleanNodeName(nodeToFindOrCreate));
			currentNode.appendChild(nextNode);
		} else if (nodeSet.getLength() == 1){
			LOGGER.debug("Found node at " + progressiveXpath);
			nextNode = nodeSet.item(0);
		} else {
			throw new DocumentHelperException("Node list should contain one element. XPath:'" + progressiveXpath + "', node count:" + nodeSet.getLength());
		}
		if (!xPathPartsStack.isEmpty()) {
			return recursiveCreateNode(doc, progressiveXpath, nextNode, xPathPartsStack);
		} else {
			return nextNode;
		}
	}

	private String cleanNodeName(String nodeName) {
		int indexOfBracket = nodeName.indexOf("[");
		if (indexOfBracket != -1) {
			return nodeName.substring(0, indexOfBracket);
		} else {
			return nodeName;
		}
	}

	private NodeList lookup(Document dataDocument, XPathExpression fullXPathExpression) throws XPathExpressionException {
		return (NodeList) fullXPathExpression.evaluate(dataDocument, XPathConstants.NODESET);
	}

	private XPathExpression newXPath(String xPathString) throws XPathExpressionException {
		return xPathFactory.newXPath().compile(xPathString);
	}
	
	public String documentToString(Node document) throws TransformerException {
		StringWriter writer = new StringWriter();
		documentToWriter(document, writer);
		return writer.toString();
	}

	public void documentToWriter(Node document, Writer writer) throws TransformerException {
		TransformerFactory transFactory = TransformerFactory.newInstance();
		Transformer transformer = transFactory.newTransformer();
		transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
		transformer.transform(new DOMSource(document), new StreamResult(writer));
	}

	public void deleteNodes(String xpath, Document dataDocument) throws XPathExpressionException {
		XPathExpression expression = newXPath(xpath);
		NodeList nodesToDelete = (NodeList) expression.evaluate(dataDocument, XPathConstants.NODESET);
		for (int i = 0; i < nodesToDelete.getLength(); i++) {
			Node nodeToDelete = nodesToDelete.item(i);
			nodeToDelete.getParentNode().removeChild(nodeToDelete);
		}
	}

}
