package se.repos.vfile.gen;

import java.util.Iterator;
import java.util.LinkedHashMap;
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

    private TaggedNode getRootElement() {
        return new TaggedNode(this, this.index.getDocumentElement());
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
        // TODO Set normalization again.
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
        for (Attr a : ElementUtils.getAttributes(root)) {
            idx.getRootElement().setAttribute(a.getName(), a.getValue());
        }
        for (Node c : ElementUtils.getChildren(root)) {
            idx.getRootElement().normalizeNode(c);
        }
        return idx;
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
        Map<TaggedNode, DeferredChanges> reorderMap = new LinkedHashMap<TaggedNode, DeferredChanges>();
        MultiMap<String, Node> newNodeMap = new MultiMap<String, Node>();

        @SuppressWarnings("unchecked")
        List<Difference> differences = diff.getAllDifferences();
        for (Difference d : differences) {
            this.scheduleChange(changeMap, reorderMap, newNodeMap, d);
        }

        this.setDocumentVersion(newVersion);
        this.setDocumentTime(newTime);
        this.getRootElement().updateTaggedNode(changeMap, newNodeMap);
        this.addOrphanNodes(newNodeMap);
        VFile.reorderNodes(reorderMap);
    }

    private void scheduleChange(Map<TaggedNode, DeferredChanges> changeMap,
            Map<TaggedNode, DeferredChanges> reorderMap,
            MultiMap<String, Node> newNodeMap, Difference d) {

        CHANGE change = VFile.classifyChange(d.getId());
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
            int location = ElementUtils.getChildIndex(reorderMap.get(e).testNode);
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
        TaggedNode returnNode;
        switch (controlNode.getNodeType()) {
        case Node.ATTRIBUTE_NODE:
            Attr attr = (Attr) controlNode;
            indexParent = this.findTaggedNode(VFile.getXPathParent(uniqueXPath));
            returnNode = indexParent.getAttribute(attr.getName());
            break;
        case Node.ELEMENT_NODE:
            returnNode = this.findTaggedNode(uniqueXPath);
            break;
        case Node.TEXT_NODE:
            indexParent = this.findTaggedNode(VFile.getXPathParent(uniqueXPath));
            int textIndex = ElementUtils.getTextIndex((Text) controlNode);
            returnNode = indexParent.getTextNode(textIndex);
            break;
        default:
            throw new UnsupportedOperationException();
        }
        if (returnNode == null) {
            throw new RuntimeException("Could not find changed node.");
        }
        if (!returnNode.isLive()) {
            throw new RuntimeException("Found node is not live.");
        }
        return returnNode;
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
}