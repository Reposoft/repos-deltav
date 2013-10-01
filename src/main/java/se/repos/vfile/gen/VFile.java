package se.repos.vfile.gen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.XMLUnit;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * @author Hugo Svallfors <keiter@lavabit.com> Class that represents a v-file.
 */
public final class VFile {

	private Document index;

	/**
	 * Constructor for Index.
	 * 
	 * @param indexDocument
	 *            The XML document that is the v-file. Must be tagged.
	 * @param currentVersion
	 *            The version of the document indexed equal to to the docVersion
	 *            of indexDocument.
	 * @throws NullPointerException
	 *             If either argument is null.
	 * @throws IllegalArgumentException
	 *             If the indexDocument lacks the required tag attributes.
	 */
	public VFile(Document indexDocument) {
		if (indexDocument == null) {
			throw new NullPointerException();
		}
		Element root = indexDocument.getDocumentElement();
		if (root == null || !root.hasAttribute(StringConstants.VSTART)
				|| !root.hasAttribute(StringConstants.VEND)
				|| !root.hasAttribute(StringConstants.TSTART)
				|| !root.hasAttribute(StringConstants.TEND)
				|| !root.hasAttribute(StringConstants.DOCVERSION)
				|| !root.hasAttribute(StringConstants.DOCTIME)) {
			throw new IllegalArgumentException();
		}
		this.index = indexDocument;

		// Set global parameters for XMLUnit.
		XMLUnit.setCompareUnmatched(false);
		XMLUnit.setIgnoreAttributeOrder(true);
		XMLUnit.setIgnoreComments(true);
		XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
		XMLUnit.setIgnoreWhitespace(true);
		XMLUnit.setNormalize(true);
		XMLUnit.setNormalizeWhitespace(false);
	}

	public void setDocumentVersion(String version) {
		index.getDocumentElement().setAttribute(StringConstants.DOCVERSION,
				version);
	}

	public String getDocumentVersion() {
		return index.getDocumentElement().getAttribute(
				StringConstants.DOCVERSION);
	}

	public String getDocumentTime() {
		return index.getDocumentElement().getAttribute(StringConstants.DOCTIME);
	}

	public void setDocumentTime(String time) {
		index.getDocumentElement().setAttribute(StringConstants.DOCTIME, time);
	}

	public TaggedNode getRootElement() {
		return new TaggedNode(this, index.getDocumentElement());
	}

	/**
	 * Creates a new TaggedNode belonging to this index.
	 * 
	 * @see TaggedNode
	 * @return The new TaggedNode.
	 */
	public TaggedNode createTaggedNode(String tagName) {
		Element elem = index.createElement(tagName);
		elem.setAttribute(StringConstants.VSTART, getDocumentVersion());
		elem.setAttribute(StringConstants.VEND, StringConstants.NOW);
		elem.setAttribute(StringConstants.TSTART, getDocumentTime());
		elem.setAttribute(StringConstants.TEND, StringConstants.NOW);
		TaggedNode indexElem = new TaggedNode(this, elem);
		return indexElem;
	}

	/**
	 * Creates a new attribute belonging to this index.
	 * 
	 * @see TaggedNode
	 * @return The new TaggedNode.
	 */
	public TaggedNode createAttribute(String name, String value) {
		Element elem = index.createElement(name);
		elem.setAttribute(StringConstants.VSTART, getDocumentVersion());
		elem.setAttribute(StringConstants.VEND, StringConstants.NOW);
		elem.setAttribute(StringConstants.TSTART, getDocumentTime());
		elem.setAttribute(StringConstants.TEND, StringConstants.NOW);
		elem.setAttribute(StringConstants.ISATTR, StringConstants.YES);
		ElementUtils.setValue(elem, value);
		TaggedNode attr = new TaggedNode(this, elem);
		return attr;
	}

	public Document toDocument() {
		return index;
	}

	/**
	 * Creates an index from scratch from version 1 of a file.
	 * 
	 * @param firstVersion
	 *            The first version of the document in question.
	 * @param version
	 *            The SVN version of the firstVersion.
	 * @return The new Index.
	 */
	public static VFile normalizeDocument(Document firstVersion, String time,
			String version) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(true);
		dbf.setIgnoringComments(true);
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setValidating(false);
		DocumentBuilder db;
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException ex) {
			throw new RuntimeException(ex.getMessage());
		}

		Document indexXML = db.newDocument();
		indexXML.setXmlVersion(firstVersion.getXmlVersion());

		Element root = firstVersion.getDocumentElement();
		Element newRoot = indexXML.createElement(root.getTagName());
		newRoot.setAttribute(StringConstants.VSTART, version);
		newRoot.setAttribute(StringConstants.VEND, StringConstants.NOW);
		newRoot.setAttribute(StringConstants.TSTART, time);
		newRoot.setAttribute(StringConstants.TEND, StringConstants.NOW);
		newRoot.setAttribute(StringConstants.DOCVERSION, version);
		newRoot.setAttribute(StringConstants.DOCTIME, time);
		indexXML.appendChild(newRoot);
		VFile idx = new VFile(indexXML);

		for (Attr a : ElementUtils.getAttributes(root)) {
			idx.getRootElement().setAttribute(a.getName(), a.getValue());
		}
		String value = ElementUtils.getValue(root);
		if (!value.isEmpty()) {
			ElementUtils.setValue(root, value);
		}
		for (Element c : ElementUtils.getChildElements(root)) {
			idx.getRootElement().normalizeElement(c);
		}
		return idx;
	}

	// Enum for which update function to call for a changed element.
	private enum CHANGE {

		HAS_CHILD, NODE_NOT_FOUND, ELEM_ATTRS, TEXT_VALUE, ELEM_NAME_VALUE, ELEM_CHILDREN_NUMBER, ELEM_CHILDREN_ORDER, ATTR_VALUE, IGNORED
	};

	/**
	 * A class that saves changes to be performed on a single node. Includes
	 * links to the old and new versions of the node, anda set of changes to
	 * perform.
	 * 
	 * @see CHANGE
	 */
	private class DeferredChanges {

		public final Node controlNode;
		public final Node testNode;
		public final String testLocation;
		public final Set<CHANGE> changes;

		public DeferredChanges(Node controlNode, Node testNode,
				String testLocation) {
			this.controlNode = controlNode;
			this.testNode = testNode;
			this.testLocation = testLocation;
			changes = new LinkedHashSet<CHANGE>();
		}

		public void addChange(CHANGE change) {
			changes.add(change);
		}
	}

	/**
	 * Diffs the given document with the last version of it, and applies the
	 * changes to the index.
	 * 
	 * @param newDocument
	 *            The new version of the document.
	 * @param newVersion
	 *            The version number of said document in SVN.
	 */
	public void update(Document oldDocument, Document newDocument,
			String newTime, String newVersion) {
		DetailedDiff diff = new DetailedDiff(new Diff(oldDocument, newDocument));
		diff.overrideElementQualifier(new NameAndPositionElementQualifier());

		Map<TaggedNode, DeferredChanges> changeMap = new LinkedHashMap<TaggedNode, DeferredChanges>();
		Map<TaggedNode, DeferredChanges> reorderMap = new LinkedHashMap<TaggedNode, DeferredChanges>();
		MultiMap<String, Element> newNodeMap = new MultiMap<String, Element>();

		@SuppressWarnings("unchecked")
		List<Difference> differences = (List<Difference>) diff
				.getAllDifferences();
		for (Difference d : differences) {
			scheduleChange(changeMap, reorderMap, newNodeMap, d);
		}

		this.setDocumentVersion(newVersion);
		this.setDocumentTime(newTime);
		updateTaggedNode(changeMap, newNodeMap, this.getRootElement());
		addOrphanNodes(newNodeMap);
		reorderNodes(reorderMap);
		cleanDocument();
	}

	private void scheduleChange(Map<TaggedNode, DeferredChanges> changeMap,
			Map<TaggedNode, DeferredChanges> reorderMap,
			MultiMap<String, Element> newNodeMap, Difference d) {

		CHANGE change = classifyChange(d.getId());
		if (change == CHANGE.IGNORED) {
			return;
		}
		Node controlNode = d.getControlNodeDetail().getNode();
		Node testNode = d.getTestNodeDetail().getNode();
		String controlLocation = d.getControlNodeDetail().getXpathLocation();
		String testLocation = d.getTestNodeDetail().getXpathLocation();

		if (controlNode == null) {
			String testParentLocation = getXPathParent(testLocation);
			newNodeMap.put(testParentLocation, (Element) testNode);
		} else {
			TaggedNode element = findIndexNode(controlNode, controlLocation);
			Map<TaggedNode, DeferredChanges> map;
			if (change == CHANGE.ELEM_CHILDREN_ORDER) {
				map = reorderMap;
			} else {
				map = changeMap;
			}
			if (!map.containsKey(element)) {
				map.put(element, new DeferredChanges(controlNode, testNode,
						testLocation));
				map.get(element).addChange(change);
			} else {
				map.get(element).addChange(change);
			}
		}
	}

	// Add any node that couldn't be added in updateTaggedNode.
	private void addOrphanNodes(MultiMap<String, Element> newNodeMap) {
		Iterator<String> xPaths = newNodeMap.keySet().iterator();
		while (xPaths.hasNext()) {
			String xPath = xPaths.next();
			TaggedNode parent = findTaggedNode(xPath);
			if (parent == null) {
				throw new RuntimeException("Unable to find node " + xPath
						+ " to add child nodes to.");
			}
			Set<Element> newNodes = newNodeMap.get(xPath);
			xPaths.remove();
			for (Element e : newNodes) {
				parent.normalizeElement(e);
			}
		}
		if (!newNodeMap.isEmpty()) {
			throw new RuntimeException("Some new child nodes where not added.");
		}
	}

	private void reorderNodes(Map<TaggedNode, DeferredChanges> reorderMap) {
		for (TaggedNode e : reorderMap.keySet()) {
			reorderElementChildren(e, reorderMap.get(e).testNode);
		}
	}

	/**
	 * Classifies each change constant of XMLUnit into a function to be called.
	 * 
	 * @param id
	 *            The chnaged to be classified.
	 * @return The corresponding CHANGE.
	 * @see CHANGE
	 * @throws UnsupportedOperationException
	 *             If the change type in question isn't implemented yet.
	 */
	private CHANGE classifyChange(int id) {
		switch (id) {
		case DifferenceConstants.CHILD_NODE_NOT_FOUND_ID:
			return CHANGE.NODE_NOT_FOUND;
		case DifferenceConstants.CHILD_NODELIST_LENGTH_ID:
			return CHANGE.ELEM_CHILDREN_NUMBER;
		case DifferenceConstants.HAS_CHILD_NODES_ID:
			return CHANGE.HAS_CHILD;
		case DifferenceConstants.TEXT_VALUE_ID:
			return CHANGE.TEXT_VALUE;
		case DifferenceConstants.ELEMENT_TAG_NAME_ID:
		case DifferenceConstants.CDATA_VALUE_ID:
			return CHANGE.ELEM_NAME_VALUE;
		case DifferenceConstants.ATTR_VALUE_ID:
		case DifferenceConstants.ATTR_VALUE_EXPLICITLY_SPECIFIED_ID:
			return CHANGE.ATTR_VALUE;
		case DifferenceConstants.ATTR_NAME_NOT_FOUND_ID:
		case DifferenceConstants.ELEMENT_NUM_ATTRIBUTES_ID:
			return CHANGE.ELEM_ATTRS;
		case DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID:
			return CHANGE.ELEM_CHILDREN_ORDER;
		case DifferenceConstants.ATTR_SEQUENCE_ID:
		case DifferenceConstants.COMMENT_VALUE_ID:
			return CHANGE.IGNORED;
		default:
			throw new UnsupportedOperationException();
		}
	}

	private void cleanDocument() {
		for (TaggedNode child : this.getRootElement().elements(false)) {
			cleanNode(this.getRootElement(), child);
		}
	}

	/*
	 * Removes nodes with the same VSTART/VEND time, and sort deleted nodes to
	 * the bottom of the file
	 */
	private void cleanNode(TaggedNode parent, TaggedNode child) {
		if (child.getVStart().equals(child.getVEnd())) {
			parent.eraseChild(child);
		} else {
			if (!child.isLive()) {
				parent.eraseChild(child);
				parent.appendChild(child);
			}
			for (TaggedNode childOfChild : child.elements(false)) {
				cleanNode(child, childOfChild);
			}
		}
	}

	/**
	 * Returns whether the given document is the latest version of this index.
	 * 
	 * @param currentVersion
	 *            The document to compare to this index.
	 * @return Whether currentVersion is saved in this index.
	 */
	public boolean documentEquals(Document currentVersion) {
		return this.getRootElement().isEqualElement(
				currentVersion.getDocumentElement());
	}

	/**
	 * Finds the TaggedNode that corresponds to controlNode.
	 * 
	 * @param controlNode
	 *            The node we are looking for in the index.
	 * @param uniqueXPath
	 *            The unique XPath selector of controlNode.
	 * @return The TaggedNode to update.
	 */
	private TaggedNode findIndexNode(Node controlNode, String uniqueXPath) {
		switch (controlNode.getNodeType()) {
		case Node.ATTRIBUTE_NODE:
			Attr attr = (Attr) controlNode;
			TaggedNode indexParent = findTaggedNode(getXPathParent(uniqueXPath));
			return indexParent.getAttribute(attr.getName());
		case Node.ELEMENT_NODE:
			return findTaggedNode(uniqueXPath);
		case Node.TEXT_NODE:
			return findTaggedNode(getXPathParent(uniqueXPath));
		default:
			throw new UnsupportedOperationException();
		}
	}

	private String getXPathParent(String xPath) {
		return xPath.substring(0, xPath.lastIndexOf("/"));
	}

	private TaggedNode findTaggedNode(String uniqueXPath) {
		Element result = (Element) xPathQuery(uniqueXPath, index);
		if (result == null) {
			return null;
		} else {
			return new TaggedNode(this, result);
		}
	}

	private Node xPathQuery(String xPath, Document doc) {
		XPath xPathEvaluator = XPathFactory.newInstance().newXPath();
		try {
			return (Node) xPathEvaluator.evaluate(xPath, doc,
					XPathConstants.NODE);
		} catch (XPathExpressionException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}

	/**
	 * Does a bottom-up update of the XML index.
	 * 
	 * @param changeMap
	 *            A map of TaggedNodes to changes to be performed on that node.
	 * @param newNodeMap
	 *            A map from xpath location of an element to the new nodes that
	 *            are to be added there.
	 * @param element
	 *            The element being updated.
	 */
	private void updateTaggedNode(Map<TaggedNode, DeferredChanges> changeMap,
			MultiMap<String, Element> newNodeMap, TaggedNode element) {
		for (TaggedNode child : element.elements(true)) {
			updateTaggedNode(changeMap, newNodeMap, child);
		}
		if (!changeMap.containsKey(element)) {
			return;
		}
		DeferredChanges d = changeMap.get(element);
		for (CHANGE change : d.changes) {
			switch (change) {
			case NODE_NOT_FOUND:
				element.delete();
				return;
			case ELEM_CHILDREN_NUMBER:
				updateElementChildren(newNodeMap, element, d.testLocation);
				break;
			case HAS_CHILD:
				updateElementChild(element, d.controlNode, d.testNode);
				break;
			case TEXT_VALUE:
				updateTextValue(element, d.testNode);
				break;
			case ELEM_NAME_VALUE:
				updateElementNameValue(element, d.testNode);
				break;
			case ATTR_VALUE:
				updateAttributeValue(element, d.testNode);
				break;
			case ELEM_ATTRS:
				updateElementAttrs(element, d.controlNode, d.testNode);
				break;
			default:
				throw new UnsupportedOperationException();
			}
		}
	}

	private void updateElementNameValue(TaggedNode element, Node testNode) {
		element.setNameValue(testNode.getNodeName(), testNode.getNodeValue());
	}

	private void updateTextValue(TaggedNode element, Node testNode) {
		// Delete old node, insert new similar node with new value.
		Text newText = (Text) testNode;
		String newValue = newText.getWholeText();
		if (newValue == null) {
			element.setValue("");
		} else {
			element.setValue(newValue);
		}
	}

	private void updateElementAttrs(TaggedNode changedNode, Node controlNode,
			Node testNode) {
		Element oldElement = (Element) controlNode;
		Element newElement = (Element) testNode;

		for (Attr oldAttr : ElementUtils.getAttributes(oldElement)) {
			if (!ElementUtils.hasEqualAttribute(newElement, oldAttr)) {
				changedNode.deleteAttribute(oldAttr.getName());
			}
		}
		for (Attr newAttr : ElementUtils.getAttributes(newElement)) {
			if (!ElementUtils.hasEqualAttribute(oldElement, newAttr)) {
				changedNode.setAttribute(newAttr.getName(), newAttr.getValue());
			}
		}
	}

	private void updateElementChildren(MultiMap<String, Element> newNodeMap,
			TaggedNode element, String testNodeLocation) {
		Set<Element> newElems = newNodeMap.remove(testNodeLocation);
		for (Element e : newElems) {
			element.normalizeElement(e);
		}
	}

	private void updateElementChild(TaggedNode element, Node controlNode,
			Node testNode) {
		Element newElement = (Element) testNode;
		Element oldElement = (Element) controlNode;
		ArrayList<Element> newElements = ElementUtils
				.getChildElements(newElement);
		ArrayList<Element> oldElements = ElementUtils
				.getChildElements(oldElement);

		if (newElements.isEmpty() && oldElements.isEmpty()) {
			throw new RuntimeException("Missing child element.");
		} else if (!newElements.isEmpty() && !oldElements.isEmpty()) {
			throw new RuntimeException(
					"Found two child elements where expected one.");
		} else if (newElements.isEmpty()) {
			for (Element e : oldElements) {
				element.deleteChildElement(e);
			}
		} else if (oldElements.isEmpty()) {
			for (Element e : newElements) {
				element.normalizeElement(e);
			}
		}
	}

	private void updateAttributeValue(TaggedNode attr, Node testNode) {
		Attr newAttr = (Attr) testNode;
		attr.setValue(newAttr.getValue());
	}

	private void reorderElementChildren(TaggedNode element, Node testNode) {
		int location = ElementUtils.getChildIndex((Element) testNode);
		element.reorder(location);
	}
}
