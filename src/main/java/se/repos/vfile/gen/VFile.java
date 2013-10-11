package se.repos.vfile.gen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import se.repos.vfile.VFileDocumentBuilderFactory;

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
        if (root == null || !root.hasAttribute(StringConstants.START)
                || !root.hasAttribute(StringConstants.END)
                || !root.hasAttribute(StringConstants.TSTART)
                || !root.hasAttribute(StringConstants.TEND)
                || !root.hasAttribute(StringConstants.DOCVERSION)
                || !root.hasAttribute(StringConstants.DOCTIME)) {
            throw new IllegalArgumentException();
        }
        this.index = indexDocument;
    }

    public void setDocumentVersion(String version) {
        this.index.getDocumentElement().setAttribute(StringConstants.DOCVERSION, version);
    }

    public String getDocumentVersion() {
        return this.index.getDocumentElement().getAttribute(StringConstants.DOCVERSION);
    }

    public String getDocumentTime() {
        return this.index.getDocumentElement().getAttribute(StringConstants.DOCTIME);
    }

    public void setDocumentTime(String time) {
        this.index.getDocumentElement().setAttribute(StringConstants.DOCTIME, time);
    }

    public TaggedNode getRootElement() {
        return new TaggedNode(this, this.index.getDocumentElement());
    }

    /**
     * Creates a new TaggedNode belonging to this index.
     * 
     * @see TaggedNode
     * @return The new TaggedNode.
     */
    public TaggedNode createTaggedNode(String name) {
        Element elem = this.index.createElement(name);
        elem.setAttribute(StringConstants.START, this.getDocumentVersion());
        elem.setAttribute(StringConstants.END, StringConstants.NOW);
        elem.setAttribute(StringConstants.TSTART, this.getDocumentTime());
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
        Element elem = this.index.createElement(StringConstants.ATTR);
        elem.setAttribute(StringConstants.NAME, name);
        elem.setAttribute(StringConstants.START, this.getDocumentVersion());
        elem.setAttribute(StringConstants.END, StringConstants.NOW);
        elem.setAttribute(StringConstants.TSTART, this.getDocumentTime());
        elem.setAttribute(StringConstants.TEND, StringConstants.NOW);
        elem.setTextContent(value);
        TaggedNode attr = new TaggedNode(this, elem);
        return attr;
    }

    /**
     * Creates a new text element belonging to this index.
     * 
     * @see TaggedNode
     * @return The new TaggedNode.
     */
    public TaggedNode createText(String value) {
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Empty text node.");
        }
        Element elem = this.index.createElement(StringConstants.TEXT);
        elem.setAttribute(StringConstants.START, this.getDocumentVersion());
        elem.setAttribute(StringConstants.END, StringConstants.NOW);
        elem.setAttribute(StringConstants.TSTART, this.getDocumentTime());
        elem.setAttribute(StringConstants.TEND, StringConstants.NOW);
        elem.setTextContent(value);
        TaggedNode attr = new TaggedNode(this, elem);
        return attr;
    }

    public Document toDocument() {
        return this.index;
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
        firstVersion.normalizeDocument();
        DocumentBuilder db = new VFileDocumentBuilderFactory().newDocumentBuilder();

        Document indexXML = db.newDocument();
        indexXML.setXmlVersion(firstVersion.getXmlVersion());

        Element root = firstVersion.getDocumentElement();
        Element newRoot = indexXML.createElement(root.getTagName());
        newRoot.setAttribute(StringConstants.START, version);
        newRoot.setAttribute(StringConstants.END, StringConstants.NOW);
        newRoot.setAttribute(StringConstants.TSTART, time);
        newRoot.setAttribute(StringConstants.TEND, StringConstants.NOW);
        newRoot.setAttribute(StringConstants.DOCVERSION, version);
        newRoot.setAttribute(StringConstants.DOCTIME, time);
        newRoot.setAttribute("xmlns:v", "http://www.repos.se/namespace/v");
        indexXML.appendChild(newRoot);
        VFile idx = new VFile(indexXML);

        for (Attr a : ElementUtils.getNamespaces(root)) {
            newRoot.setAttribute(a.getName(), a.getValue());
        }
        for (Attr a : ElementUtils.getAttributes(root)) {
            idx.getRootElement().setAttribute(a.getName(), a.getValue());
        }
        for (Element c : ElementUtils.getChildElements(root)) {
            idx.getRootElement().normalizeElement(c);
        }
        for (Text t : ElementUtils.getText(root)) {
            idx.getRootElement().normalizeText(t);
        }
        return idx;
    }

    // Enum for which update function to call for a changed element.
    private enum CHANGE {

        HAS_CHILD, NODE_NOT_FOUND, ELEM_ATTRS, TEXT_VALUE, ELEM_NAME, ELEM_CHILDREN_NUMBER, ELEM_CHILDREN_ORDER, ATTR_VALUE, IGNORED
    }

    /**
     * A class that saves changes to be performed on a single node. Includes
     * links to the old and new versions of the node, and a set of changes to
     * perform.
     * 
     * @see CHANGE
     */
    private class DeferredChanges {

        public final Node controlNode;
        public final Node testNode;
        public final String testLocation;
        public final Set<CHANGE> changes;

        public DeferredChanges(Node controlNode, Node testNode, String testLocation) {
            this.controlNode = controlNode;
            this.testNode = testNode;
            this.testLocation = testLocation;
            this.changes = new LinkedHashSet<CHANGE>();
        }

        public void addChange(CHANGE change) {
            this.changes.add(change);
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
    public void update(Document oldDocument, Document newDocument, String newTime,
            String newVersion) {
        oldDocument.normalizeDocument();
        newDocument.normalizeDocument();
        DetailedDiff diff = new DetailedDiff(new Diff(oldDocument, newDocument));
        diff.overrideElementQualifier(new NameAndPositionElementQualifier());

        Map<TaggedNode, DeferredChanges> changeMap = new LinkedHashMap<TaggedNode, DeferredChanges>();
        Map<TaggedNode, DeferredChanges> reorderMap = new LinkedHashMap<TaggedNode, DeferredChanges>();
        MultiMap<String, Node> newNodeMap = new MultiMap<String, Node>();

        @SuppressWarnings("unchecked")
        List<Difference> differences = diff.getAllDifferences();
        for (Difference d : differences) {
            this.scheduleChange(changeMap, reorderMap, newNodeMap, d);
        }

        this.setDocumentVersion(newVersion);
        this.setDocumentTime(newTime);
        VFile.updateTaggedNode(changeMap, newNodeMap, this.getRootElement());
        this.addOrphanNodes(newNodeMap);
        VFile.reorderNodes(reorderMap);
        this.cleanDocument();
    }

    private void scheduleChange(Map<TaggedNode, DeferredChanges> changeMap,
            Map<TaggedNode, DeferredChanges> reorderMap,
            MultiMap<String, Node> newNodeMap, Difference d) {

        CHANGE change = VFile.classifyChange(d.getId());
        if (change == CHANGE.IGNORED) {
            return;
        }
        Node controlNode = d.getControlNodeDetail().getNode();
        Node testNode = d.getTestNodeDetail().getNode();
        String controlLocation = d.getControlNodeDetail().getXpathLocation();
        String testLocation = d.getTestNodeDetail().getXpathLocation();

        if (controlNode == null) {
            String testParentLocation = VFile.getXPathParent(testLocation);
            newNodeMap.put(testParentLocation, testNode);
        } else {
            TaggedNode element = this.findIndexNode(controlNode, controlLocation);
            Map<TaggedNode, DeferredChanges> map;
            if (change == CHANGE.ELEM_CHILDREN_ORDER) {
                map = reorderMap;
            } else {
                map = changeMap;
            }
            if (!map.containsKey(element)) {
                map.put(element, new DeferredChanges(controlNode, testNode, testLocation));
                map.get(element).addChange(change);
            } else {
                map.get(element).addChange(change);
            }
        }
    }

    // Add any node that couldn't be added in updateTaggedNode.
    private void addOrphanNodes(MultiMap<String, Node> newNodeMap) {
        Iterator<String> xPaths = newNodeMap.keySet().iterator();
        while (xPaths.hasNext()) {
            String xPath = xPaths.next();
            TaggedNode parent = this.findTaggedNode(xPath);
            if (parent == null) {
                throw new RuntimeException("Unable to find node " + xPath
                        + " to add child nodes to.");
            }
            Set<Node> newNodes = newNodeMap.get(xPath);
            xPaths.remove();
            for (Node n : newNodes) {
                parent.normalizeNode(n);
            }
        }
        if (!newNodeMap.isEmpty()) {
            throw new RuntimeException("Some new child nodes where not added.");
        }
    }

    private static void reorderNodes(Map<TaggedNode, DeferredChanges> reorderMap) {
        for (TaggedNode e : reorderMap.keySet()) {
            int location = ElementUtils
                    .getChildIndex((Element) reorderMap.get(e).testNode);
            e.reorder(location);
        }
    }

    /**
     * Classifies each change constant of XMLUnit into a function to be called.
     * 
     * @param id
     *            The change to be classified.
     * @return The corresponding CHANGE.
     * @see CHANGE
     * @throws UnsupportedOperationException
     *             If the change type in question isn't implemented yet.
     */
    private static CHANGE classifyChange(int id) {
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
            return CHANGE.ELEM_NAME;
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
            VFile.cleanNode(this.getRootElement(), child);
        }
    }

    /*
     * Removes nodes with the same VSTART/VEND time, and sort deleted nodes to
     * the bottom of the file
     */
    private static void cleanNode(TaggedNode parent, TaggedNode child) {
        if (child.getVStart().equals(child.getEnd())) {
            parent.eraseChild(child);
        } else {
            if (!child.isLive()) {
                parent.eraseChild(child);
                parent.appendChild(child);
            }
            for (TaggedNode childOfChild : child.elements(false)) {
                VFile.cleanNode(child, childOfChild);
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
        return this.getRootElement().isEqualElement(currentVersion.getDocumentElement());
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
        TaggedNode indexParent;
        switch (controlNode.getNodeType()) {
        case Node.ATTRIBUTE_NODE:
            Attr attr = (Attr) controlNode;
            indexParent = this.findTaggedNode(VFile.getXPathParent(uniqueXPath));
            return indexParent.getAttribute(attr.getName());
        case Node.ELEMENT_NODE:
            return this.findTaggedNode(uniqueXPath);
        case Node.TEXT_NODE:
            indexParent = this.findTaggedNode(VFile.getXPathParent(uniqueXPath));
            int textIndex = ElementUtils.getTextIndex((Text) controlNode);
            return indexParent.getText().get(textIndex);
        default:
            throw new UnsupportedOperationException();
        }
    }

    private static String getXPathParent(String xPath) {
        return xPath.substring(0, xPath.lastIndexOf("/"));
    }

    private TaggedNode findTaggedNode(String uniqueXPath) {
        Element result = (Element) VFile.xPathQuery(uniqueXPath, this.index);
        if (result == null) {
            return null;
        }
        return new TaggedNode(this, result);
    }

    private static Node xPathQuery(String xPath, Document doc) {
        XPath xPathEvaluator = XPathFactory.newInstance().newXPath();
        try {
            return (Node) xPathEvaluator.evaluate(xPath, doc, XPathConstants.NODE);
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
     *            A map from XPath location of an element to the new nodes that
     *            are to be added there.
     * @param element
     *            The element being updated.
     */
    private static void updateTaggedNode(Map<TaggedNode, DeferredChanges> changeMap,
            MultiMap<String, Node> newNodeMap, TaggedNode element) {
        for (TaggedNode child : element.elements(true)) {
            VFile.updateTaggedNode(changeMap, newNodeMap, child);
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
                VFile.updateElementChildren(element, newNodeMap, d.testLocation);
                break;
            case HAS_CHILD:
                VFile.updateElementChild(element, (Element) d.controlNode,
                        (Element) d.testNode);
                break;
            case ELEM_NAME:
                element.setName(d.testNode.getNodeName());
                break;
            case ATTR_VALUE:
                Attr newAttr = (Attr) d.testNode;
                element.setValue(newAttr.getValue());
                break;
            case ELEM_ATTRS:
                VFile.updateElementAttrs(element, (Element) d.controlNode,
                        (Element) d.testNode);
                break;
            case TEXT_VALUE:
                element.setValue(d.testNode.getTextContent());
                break;
            case ELEM_CHILDREN_ORDER:
                // Dealt with later.
                break;
            case IGNORED:
                break;
            }
        }
    }

    private static void updateElementAttrs(TaggedNode element, Element oldElement,
            Element newElement) {
        for (Attr oldNS : ElementUtils.getNamespaces(oldElement)) {
            if (!ElementUtils.hasEqualAttribute(newElement, oldNS)) {
                element.deleteNamespace(oldNS);
            }
        }
        for (Attr newNS : ElementUtils.getNamespaces(newElement)) {
            if (!ElementUtils.hasEqualAttribute(oldElement, newNS)) {
                element.setNamespace(newNS);
            }
        }
        for (Attr oldAttr : ElementUtils.getAttributes(oldElement)) {
            if (!ElementUtils.hasEqualAttribute(newElement, oldAttr)) {
                element.deleteAttribute(oldAttr.getName());
            }
        }
        for (Attr newAttr : ElementUtils.getAttributes(newElement)) {
            if (!ElementUtils.hasEqualAttribute(oldElement, newAttr)) {
                element.setAttribute(newAttr.getName(), newAttr.getValue());
            }
        }
    }

    private static void updateElementChildren(TaggedNode element,
            MultiMap<String, Node> newNodeMap, String testNodeLocation) {
        for (Node n : newNodeMap.remove(testNodeLocation)) {
            element.normalizeNode(n);
        }
    }

    private static void updateElementChild(TaggedNode element, Element oldElement,
            Element newElement) {
        ArrayList<Element> newElements = ElementUtils.getChildElements(newElement);
        ArrayList<Element> oldElements = ElementUtils.getChildElements(oldElement);

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
}