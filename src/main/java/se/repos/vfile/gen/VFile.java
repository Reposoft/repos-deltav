package se.repos.vfile.gen;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

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

    private void setDocumentVersion(String version) {
        this.index.getDocumentElement().setAttribute(StringConstants.DOCVERSION, version);
    }

    public String getDocumentVersion() {
        return this.index.getDocumentElement().getAttribute(StringConstants.DOCVERSION);
    }

    public String getDocumentTime() {
        return this.index.getDocumentElement().getAttribute(StringConstants.DOCTIME);
    }

    private void setDocumentTime(String time) {
        this.index.getDocumentElement().setAttribute(StringConstants.DOCTIME, time);
    }

    private TaggedNode getVFileElement() {
        return new TaggedNode(this, this.index.getDocumentElement());
    }

    private TaggedNode getDocumentElement() {
        return this.getVFileElement().getNthNodeOfType(0, Nodetype.ELEMENT);
    }

    /**
     * Creates a new TaggedNode belonging to this index.
     * 
     * @see TaggedNode
     * @return The new TaggedNode.
     */
    public TaggedNode createTaggedNode(String nodeName, String elementName, String value) {
        if (nodeName == null || nodeName.isEmpty()) {
            throw new IllegalArgumentException("Empty node name.");
        }
        if (elementName != null && elementName.isEmpty()) {
            throw new IllegalArgumentException("Empty element name.");
        }

        Element elem = this.index.createElement(nodeName);
        elem.setAttribute(StringConstants.START, this.getDocumentVersion());
        elem.setAttribute(StringConstants.END, StringConstants.NOW);
        elem.setAttribute(StringConstants.TSTART, this.getDocumentTime());
        elem.setAttribute(StringConstants.TEND, StringConstants.NOW);

        if (elementName != null) {
            elem.setAttribute(StringConstants.NAME, elementName);
        }
        if (value != null) {
            elem.setTextContent(value);
        }

        TaggedNode indexElem = new TaggedNode(this, elem);
        return indexElem;
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

        Element vFileElement = indexXML.createElement(StringConstants.FILE);
        vFileElement.setAttribute("xmlns:v", "http://www.repos.se/namespace/v");
        vFileElement.setAttribute(StringConstants.DOCVERSION, version);
        vFileElement.setAttribute(StringConstants.DOCTIME, time);
        vFileElement.setAttribute(StringConstants.START, version);
        vFileElement.setAttribute(StringConstants.END, StringConstants.NOW);
        vFileElement.setAttribute(StringConstants.TSTART, time);
        vFileElement.setAttribute(StringConstants.TEND, StringConstants.NOW);
        indexXML.appendChild(vFileElement);

        VFile idx = new VFile(indexXML);
        Node root = firstVersion.getDocumentElement();
        idx.getVFileElement().normalizeNode(root);

        return idx;
    }

    public Map<SimpleXPath, TaggedNode> getNodeMap(Document controlDocument) {
        Map<SimpleXPath, TaggedNode> nodeMap = new HashMap<SimpleXPath, TaggedNode>();
        if (!this.documentEquals(controlDocument)) {
            throw new IllegalArgumentException("Control document doesn't match v-file.");
        }
        this.normalizeNodeMap(nodeMap, controlDocument.getDocumentElement());
        return nodeMap;
    }

    private void normalizeNodeMap(Map<SimpleXPath, TaggedNode> nodeMap, Node controlNode) {
        SimpleXPath uniqueXPath = new SimpleXPath(controlNode);
        TaggedNode node = uniqueXPath.eval(this.getVFileElement());
        if (!node.isEqualNode(controlNode)) {
            throw new RuntimeException("Found incorrect node.");
        }
        nodeMap.put(uniqueXPath, node);
        for (Node n : ElementUtils.getChildren(controlNode)) {
            this.normalizeNodeMap(nodeMap, n);
        }
        if (controlNode.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) controlNode;
            for (Attr a : ElementUtils.getAttributes(e)) {
                SimpleXPath attrXPath = new SimpleXPath(a);
                nodeMap.put(attrXPath, attrXPath.eval(this.getVFileElement()));
            }
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
        DetailedDiff diff = new DetailedDiff(new Diff(oldDocument, newDocument));
        diff.overrideElementQualifier(new NameAndPositionElementQualifier());

        Map<TaggedNode, DeferredChanges> changeMap = new LinkedHashMap<TaggedNode, DeferredChanges>();
        MultiMap<SimpleXPath, Node> newNodeMap = new MultiMap<SimpleXPath, Node>();

        @SuppressWarnings("unchecked")
        List<Difference> differences = diff.getAllDifferences();
        for (Difference d : differences) {
            this.scheduleChange(changeMap, newNodeMap, d);
        }

        this.setDocumentVersion(newVersion);
        this.setDocumentTime(newTime);

        for (TaggedNode node : changeMap.keySet()) {
            node.updateTaggedNode(changeMap.get(node));
        }
        this.addOrphanNodes(newNodeMap);
    }

    private void scheduleChange(Map<TaggedNode, DeferredChanges> changeMap,
            MultiMap<SimpleXPath, Node> newNodeMap, Difference d) {

        CHANGE change = VFile.classifyChange(d.getId());
        Node controlNode = d.getControlNodeDetail().getNode();
        Node testNode = d.getTestNodeDetail().getNode();
        SimpleXPath testLocation;

        if (controlNode == null) {
            testLocation = new SimpleXPath(d.getTestNodeDetail().getXpathLocation());
            testLocation.removeLastAxis();
            newNodeMap.put(testLocation, testNode);
        } else {
            SimpleXPath controlLocation = new SimpleXPath(d.getControlNodeDetail()
                    .getXpathLocation());
            testLocation = testNode == null ? null : new SimpleXPath(d
                    .getTestNodeDetail().getXpathLocation());
            TaggedNode element = controlLocation.eval(this.getVFileElement());
            if (!changeMap.containsKey(element)) {
                changeMap.put(element, new DeferredChanges(controlNode, testNode,
                        testLocation));
                changeMap.get(element).addChange(change);
            } else {
                changeMap.get(element).addChange(change);
            }
        }
    }

    // Add any node that couldn't be added in updateTaggedNode.
    private void addOrphanNodes(MultiMap<SimpleXPath, Node> newNodeMap) {
        Iterator<SimpleXPath> xPaths = newNodeMap.keySet().iterator();
        while (xPaths.hasNext()) {
            SimpleXPath xPath = xPaths.next();
            TaggedNode parent = xPath.eval(this.getVFileElement());
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
        case DifferenceConstants.COMMENT_VALUE_ID:
            return CHANGE.COMMENT_VALUE;
        case DifferenceConstants.PROCESSING_INSTRUCTION_DATA_ID:
            return CHANGE.PI_DATA;
        case DifferenceConstants.ATTR_VALUE_ID:
        case DifferenceConstants.ATTR_VALUE_EXPLICITLY_SPECIFIED_ID:
            return CHANGE.ATTR_VALUE;
        case DifferenceConstants.ATTR_NAME_NOT_FOUND_ID:
        case DifferenceConstants.ELEMENT_NUM_ATTRIBUTES_ID:
            return CHANGE.ELEM_ATTRS;
        case DifferenceConstants.CHILD_NODELIST_SEQUENCE_ID:
            return CHANGE.ELEM_CHILDREN_ORDER;
        default:
            throw new UnsupportedOperationException();
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
        return this.getDocumentElement().isEqualNode(currentVersion.getDocumentElement());
    }
}