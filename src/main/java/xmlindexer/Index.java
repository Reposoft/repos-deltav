package xmlindexer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.xml.parsers.*;
import javax.xml.xpath.*;
import org.custommonkey.xmlunit.*;
import org.w3c.dom.*;

/**
 * @author Hugo Svallfors <keiter@lavabit.com>
 * Class that represents a v-file.
 */
public final class Index {

    private Document index;
    private Document currentVersion;

    /**
     * Constructor for Index.
     * @param indexDocument The XML document that is the v-file. Must be tagged.
     * @param currentVersion The version of the document indexed equal to
     * to the docVersion of indexDocument.
     * @throws NullPointerException If either argument is null.
     * @throws IllegalArgumentException If the indexDocument lacks the
     * required tag attributes.
     */
    public Index(Document indexDocument, Document currentVersion) {
        if (indexDocument == null || currentVersion == null) {
            throw new NullPointerException();
        }
        Element root = indexDocument.getDocumentElement();
        if (root == null
                || !root.hasAttribute(StringConstants.VSTART)
                || !root.hasAttribute(StringConstants.VEND)
                || !root.hasAttribute(StringConstants.DOCVERSION)) {
            throw new IllegalArgumentException();
        }
        this.index = indexDocument;
        this.currentVersion = currentVersion;
    }

    /**
     * Sets the docVersion.
     */
    public void setDocumentVersion(long version) {
        index.getDocumentElement().setAttribute(StringConstants.DOCVERSION,
                Long.toString(version));
    }

    /**
     * Gets the docVersion.
     */
    public long getDocumentVersion() {
        return Long.parseLong(index.getDocumentElement().getAttribute(
                StringConstants.DOCVERSION));
    }

    public IndexElement getRootElement() {
        return new IndexElement(this, index.getDocumentElement());
    }

    /**
     * Creates a new IndexElement belonging to this index.
     * @see IndexElement
     * @return The new IndexElement.
     */
    public IndexElement createIndexElement(String tagName) {
        Element elem = index.createElement(tagName);
        elem.setAttribute(StringConstants.VSTART,
                Long.toString(getDocumentVersion()));
        elem.setAttribute(StringConstants.VEND, StringConstants.NOW);
        IndexElement indexElem = new IndexElement(this, elem);
        return indexElem;
    }

    /**
     * Creates a new attribute belonging to this index.
     * @see IndexElement
     * @return The new IndexElement.
     */
    public IndexElement createIndexAttribute(String name, String value) {
        Element e = index.createElement(name);
        e.setAttribute(StringConstants.VSTART,
                Long.toString(getDocumentVersion()));
        e.setAttribute(StringConstants.VEND, StringConstants.NOW);
        e.setAttribute(StringConstants.ISATTR, StringConstants.YES);
        ElementUtils.setValue(e, value);
        IndexElement attr = new IndexElement(this, e);
        return attr;
    }
    
    public Document toDocument() {
    	return index;
    }

    /**
     * Creates an index from scratch from version 1 of a file.
     * @param firstVersion The first version of the document in question.
     * @param version The SVN version of the firstVersion.
     * @return The new Index.
     */
    public static Index normalizeDocument(Document firstVersion, long version) {
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
        newRoot.setAttribute(StringConstants.VSTART, Long.toString(version));
        newRoot.setAttribute(StringConstants.VEND, StringConstants.NOW);
        newRoot.setAttribute(StringConstants.DOCVERSION, Long.toString(version));
        indexXML.appendChild(newRoot);
        Index idx = new Index(indexXML, firstVersion);

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

        HAS_CHILD,
        NODE_NOT_FOUND,
        ELEM_ATTRS,
        TEXT_VALUE,
        ELEM_NAME_VALUE,
        ELEM_CHILDREN_NUMBER,
        ELEM_CHILDREN_ORDER,
        ATTR_VALUE,
        IGNORED
    };

    /**
     * A class that saves changes to be performed on a single node.
     * Includes links to the old and new versions of the node, anda set of
     * changes to perform.
     * @see CHANGE
     */
    private class DeferredChanges {

        public final Node controlNode;
        public final Node testNode;
        public final String controlLocation;
        public final String testLocation;
        public final Set<CHANGE> changes;

        public DeferredChanges(Node controlNode, Node testNode,
                String controlLocation, String testLocation) {
            this.controlNode = controlNode;
            this.testNode = testNode;
            this.controlLocation = controlLocation;
            this.testLocation = testLocation;
            changes = new LinkedHashSet<>();
        }

        public void addChange(CHANGE change) {
            changes.add(change);
        }
    }

    /**
     * Diffs the given document with the last version of it, and applies
     * the changes to the index.
     * @param newVersion The new version of the document.
     * @param newVersionNumber The version number of said document in SVN.
     */
    public void update(Document newVersion, long newVersionNumber)
            throws IOException {
        DetailedDiff diff = new DetailedDiff(
                new Diff(currentVersion, newVersion));
        diff.overrideElementQualifier(new NameAndPositionElementQualifier());
        Map<IndexElement, DeferredChanges> changeMap = new LinkedHashMap<>();
        Map<IndexElement, DeferredChanges> reorderMap = new LinkedHashMap<>();
        MultiMap<String, Element> newNodeMap = new MultiMap<>();
        @SuppressWarnings("unchecked")
		List<Difference> differences = (List<Difference>) diff.getAllDifferences();

        for (Difference d : differences) {
            scheduleChange(changeMap, reorderMap, newNodeMap, d);
        }

        currentVersion = newVersion;
        this.setDocumentVersion(newVersionNumber);

        updateIndexElement(changeMap, newNodeMap, this.getRootElement());
        addOrphanNodes(newNodeMap);
        reorderNodes(reorderMap);
        cleanDocument();
        assertEquals();
    }

    private void scheduleChange(
            Map<IndexElement, DeferredChanges> changeMap,
            Map<IndexElement, DeferredChanges> reorderMap,
            MultiMap<String, Element> newNodeMap,
            Difference d) {

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
            IndexElement element = findIndexNode(controlNode, controlLocation);
            Map<IndexElement, DeferredChanges> map;
            if (change == CHANGE.ELEM_CHILDREN_ORDER) {
                map = reorderMap;
            } else {
                map = changeMap;
            }
            if (!map.containsKey(element)) {
                map.put(element, new DeferredChanges(controlNode,
                        testNode, controlLocation, testLocation));
                map.get(element).addChange(change);
            } else {
                map.get(element).addChange(change);
            }
        }
    }

    // Add any node that couldn't be added in updateIndexElement.
    private void addOrphanNodes(MultiMap<String, Element> newNodeMap) {
        Iterator<String> xPaths = newNodeMap.keySet().iterator();
        while (xPaths.hasNext()) {
            String xPath = xPaths.next();
            IndexElement parent = findIndexElement(xPath);
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

    private void reorderNodes(Map<IndexElement, DeferredChanges> reorderMap) {
        for (IndexElement e : reorderMap.keySet()) {
            reorderElementChildren(e, reorderMap.get(e).testNode);
        }
    }

    /**
     * Classifies each change constant of XMLUnit into a function to be called.
     * @param id The chnaged to be classified.
     * @return The corresponding CHANGE.
     * @see CHANGE
     * @throws UnsupportedOperationException If the change type in question
     * isn't implemented yet.
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
        for (IndexElement child : this.getRootElement().elements(false)) {
            cleanNode(this.getRootElement(), child);
        }
    }

    /* Removes nodes with the same VSTART/VEND time,
     * and sort deleted nodes to the bottom of the file*/
    private void cleanNode(IndexElement parent, IndexElement child) {
        if (child.created() == child.deleted()) {
            parent.eraseChild(child);
        } else {
            if (!child.isLive()) {
                parent.eraseChild(child);
                parent.appendChild(child);
            }
            for (IndexElement childOfChild : child.elements(false)) {
                cleanNode(child, childOfChild);
            }
        }
    }

    private void assertEquals() throws IOException {
        if (!this.getRootElement().isEqualElement(
                currentVersion.getDocumentElement())) {
            throw new RuntimeException(
                    "Index does not match lastest version of file."
                    + "\nFaulty index dumped to err.xml.");
        }
    }

    /**
     * Finds the IndexElement that corresponds to controlNode.
     * @param controlNode The node we are looking for in the index.
     * @param uniqueXPath The unique XPath selector of controlNode.
     * @return The IndexElement to update.
     */
    private IndexElement findIndexNode(Node controlNode, String uniqueXPath) {
        switch (controlNode.getNodeType()) {
            case Node.ATTRIBUTE_NODE:
                Attr attr = (Attr) controlNode;
                IndexElement indexParent =
                        findIndexElement(getXPathParent(uniqueXPath));
                return indexParent.getAttribute(attr.getName());
            case Node.ELEMENT_NODE:
                return findIndexElement(uniqueXPath);
            case Node.TEXT_NODE:
                return findIndexElement(getXPathParent(uniqueXPath));
            default:
                throw new UnsupportedOperationException();
        }
    }

    private String getXPathParent(String xPath) {
        return xPath.substring(0, xPath.lastIndexOf("/"));
    }

    private IndexElement findIndexElement(String uniqueXPath) {
        Element result = (Element) xPathQuery(uniqueXPath, index);
        if (result == null) {
            return null;
        } else {
            return new IndexElement(this, result);
        }
    }

    private Node xPathQuery(String xPath, Document doc) {
        XPath xPathEvaluator = XPathFactory.newInstance().newXPath();
        try {
            return (Node) xPathEvaluator.evaluate(xPath, doc, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }

    /**
     * Does a bottom-up update of the XML index.
     * @param changeMap A map of IndexElements to changes to be performed
     * on that node.
     * @param newNodeMap A map from xpath location of an element
     * to the new nodes that are to be added there.
     * @param element The element being updated.
     */
    private void updateIndexElement(
            Map<IndexElement, DeferredChanges> changeMap,
            MultiMap<String, Element> newNodeMap,
            IndexElement element) {
        for (IndexElement child : element.elements(true)) {
            updateIndexElement(changeMap, newNodeMap, child);
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

    private void updateElementNameValue(IndexElement element, Node testNode) {
        element.setNameValue(testNode.getNodeName(),
                testNode.getNodeValue());
    }

    private void updateTextValue(IndexElement element, Node testNode) {
        // Delete old node, insert new similar node with new value.
        Text newText = (Text) testNode;
        String newValue = newText.getWholeText();
        if (newValue == null) {
            element.setValue("");
        } else {
            element.setValue(newValue);
        }
    }

    private void updateElementAttrs(IndexElement changedNode,
            Node controlNode, Node testNode) {
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
            IndexElement element, String testNodeLocation) {
        Set<Element> newElems = newNodeMap.remove(testNodeLocation);
        for (Element e : newElems) {
            element.normalizeElement(e);
        }
    }

    private void updateElementChild(IndexElement element, Node controlNode,
            Node testNode) {
        Element newElement = (Element) testNode;
        Element oldElement = (Element) controlNode;
        ArrayList<Element> newElements =
                ElementUtils.getChildElements(newElement);
        ArrayList<Element> oldElements =
                ElementUtils.getChildElements(oldElement);

        if (newElements.isEmpty() && oldElements.isEmpty()) {
            throw new RuntimeException("Missing child element.");
        } else if (!newElements.isEmpty() && !oldElements.isEmpty()) {
            throw new RuntimeException("Found two child elements where expected one.");
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

    private void updateAttributeValue(IndexElement attr, Node testNode) {
        Attr newAttr = (Attr) testNode;
        attr.setValue(newAttr.getValue());
    }

    private void reorderElementChildren(IndexElement element, Node testNode) {
        int location = ElementUtils.getChildIndex((Element) testNode);
        element.reorder(location);
    }
}
