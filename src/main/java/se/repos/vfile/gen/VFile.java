package se.repos.vfile.gen;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;

import org.custommonkey.xmlunit.DetailedDiff;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.Difference;
import org.custommonkey.xmlunit.DifferenceConstants;
import org.custommonkey.xmlunit.XMLUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import se.repos.vfile.VFileDocumentBuilderFactory;

/**
 * Class that represents a v-file.
 */
public final class VFile {

	private static final Logger logger = LoggerFactory.getLogger(VFile.class);
	
    private Document index;
    private Long reorderCounter = 0L;

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
                /*
                || !root.hasAttribute(StringConstants.TSTART)
                || !root.hasAttribute(StringConstants.TEND)
                */
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
    
    public String getReorderId() {
    	this.reorderCounter++;
    	return this.getDocumentVersion().concat("-").concat(this.reorderCounter.toString());
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
     * Creates a new {@link TaggedNode} belonging to this index.
     * 
     * @param nodeName
     *            The actual tag name of the element created.
     * @param elementName
     *            The value of v:name set on this element. If null, it is not
     *            set.
     * @param value
     *            The text content on the {@link TaggedNode}. May be null.
     * @return The new {@link TaggedNode}.
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
        /* The timestamps are a massive pain for testing and compare.
        elem.setAttribute(StringConstants.TSTART, this.getDocumentTime());
        elem.setAttribute(StringConstants.TEND, StringConstants.NOW);
        */

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
     * @param time
     *            The time stamp of firstVersion.
     * @param version
     *            The version number of firstVersion.
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
        /*
        vFileElement.setAttribute(StringConstants.TSTART, time);
        vFileElement.setAttribute(StringConstants.TEND, StringConstants.NOW);
        */
        indexXML.appendChild(vFileElement);

        VFile idx = new VFile(indexXML);
        Node root = firstVersion.getDocumentElement();
        idx.getVFileElement().normalizeNode(root);

        return idx;
    }

    /**
     * Given a control document, returns a map between each node in the control
     * document and the corresponding {@link TaggedNode}.
     * 
     * @throws NoMatchException
     *             If the given document doesn't match this {@link VFile}.
     */
    public Map<SimpleXPath, TaggedNode> getNodeMap(Document controlDocument)
            throws NoMatchException {
        Map<SimpleXPath, TaggedNode> nodeMap = new HashMap<SimpleXPath, TaggedNode>();
        this.matchDocument(controlDocument);
        this.normalizeNodeMap(nodeMap, controlDocument.getDocumentElement());
        return nodeMap;
    }

    private void normalizeNodeMap(Map<SimpleXPath, TaggedNode> nodeMap, Node controlNode)
            throws NoMatchException {
        SimpleXPath uniqueXPath = new SimpleXPath(controlNode);
        TaggedNode node = uniqueXPath.eval(this.getVFileElement());
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
     * @param oldDocument
     *            The old version of the document.
     * @param newDocument
     *            The new version of the document.
     * @param newVersion
     *            The version number of the new document.
     * @param newTime
     *            The time stamp of the new document.
     */
    public void update(Document oldDocument, Document newDocument, String newTime,
            String newVersion) {
        Map<SimpleXPath, TaggedNode> nodeMap;
        try {
            nodeMap = this.getNodeMap(oldDocument);
        } catch (NoMatchException e) {
            throw new IllegalArgumentException(
                    "Provided document doesn't match the one indexed.", e);
        }
        /*
        XMLUnit.setNormalizeWhitespace(true);
        XMLUnit.setNormalize(true);
        */
        DetailedDiff diff = new DetailedDiff(new Diff(oldDocument, newDocument));
        diff.overrideElementQualifier(new NameAndPositionElementQualifier());

        Map<TaggedNode, DeferredChanges> changeMap = new LinkedHashMap<TaggedNode, DeferredChanges>();
        MultiMap<SimpleXPath, Node> newNodeMap = new MultiMap<SimpleXPath, Node>();

        @SuppressWarnings("unchecked")
        List<Difference> differences = diff.getAllDifferences();
        for (Difference d : differences) {
        	logger.debug("Diff: ({}) {} - {} {}", d.getId(), d.getDescription(), d.getControlNodeDetail().getValue(), d.getTestNodeDetail().getValue());
            VFile.scheduleChange(nodeMap, changeMap, newNodeMap, d);
        }

        this.setDocumentVersion(newVersion);
        this.setDocumentTime(newTime);
        this.getDocumentElement().updateTaggedNode(changeMap, newNodeMap);
        this.addOrphanNodes(newNodeMap);
        VFile.reorderNodes(changeMap);
    }

    @SuppressWarnings("null")
    private static void scheduleChange(Map<SimpleXPath, TaggedNode> nodeMap,
            Map<TaggedNode, DeferredChanges> changeMap,
            MultiMap<SimpleXPath, Node> newNodeMap, Difference d) {
        CHANGE change = VFile.classifyChange(d.getId());
        Node controlNode = d.getControlNodeDetail().getNode();
        Node testNode = d.getTestNodeDetail().getNode();
        SimpleXPath testLocation = null;
        if (testNode != null) {
            testLocation = new SimpleXPath(d.getTestNodeDetail().getXpathLocation());
        }

        if (controlNode == null) {
            testLocation.removeLastAxis();
            newNodeMap.put(testLocation, testNode);
        } else {
            SimpleXPath controlLocation = new SimpleXPath(d.getControlNodeDetail()
                    .getXpathLocation());
            // TODO: Investigate basing on matched Nodes rather than XPath.
            TaggedNode element = nodeMap.get(controlLocation);
            if (!changeMap.containsKey(element)) {
                changeMap.put(element, new DeferredChanges(controlNode, testNode,
                        testLocation));
            }
            changeMap.get(element).addChange(change);
        }
    }

    // Add any node that couldn't be added in updateTaggedNode.
    private void addOrphanNodes(MultiMap<SimpleXPath, Node> newNodeMap) {
        Iterator<SimpleXPath> xPaths = newNodeMap.keySet().iterator();
        while (xPaths.hasNext()) {
            SimpleXPath xPath = xPaths.next();
            TaggedNode parent = xPath.eval(this.getVFileElement());
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

    private static void reorderNodes(Map<TaggedNode, DeferredChanges> changeMap) {
        List<Entry<TaggedNode, Integer>> reorderList = new LinkedList<Entry<TaggedNode, Integer>>();
    	for (TaggedNode element : changeMap.keySet()) {
            DeferredChanges d = changeMap.get(element);
            if (d.changes.contains(CHANGE.ELEM_CHILDREN_ORDER)) {
                if (!element.isLive()) {
                    throw new RuntimeException("Moving non-live node.");
                }
                // TODO: Why did Hugo calculate this location? Ah, because the DeferredChange-API can not communicate detail.
                int location = ElementUtils.getLocalIndex(d.testNode);
                //int indexDetail = d.getTestNodeDetail().getValue()
                reorderList.add(new AbstractMap.SimpleImmutableEntry<TaggedNode, Integer>(element, location));
            }
        }
    	Comparator<Entry<TaggedNode, Integer>> comp = new Comparator<Entry<TaggedNode, Integer>>() {

			@Override
			public int compare(Entry<TaggedNode, Integer> arg0, Entry<TaggedNode, Integer> arg1) {
				
				return arg0.getValue().compareTo(arg1.getValue());
			}
		};
    	// Theory: By doing reordering with lowest target index first, we can avoid some reordering 
		// because later nodes take their target location as consequence of earlier reordering.
		// TODO: Investigate why the sorting makes matching fail / incorrect result for rev27 of test50k27revs.
		//Collections.sort(reorderList, comp);
        for (Entry<TaggedNode, Integer> e: reorderList) {
        	e.getKey().reorder(e.getValue());
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
        case DifferenceConstants.HAS_CHILD_NODES_ID:
            return CHANGE.ELEM_CHILDREN_NUMBER;
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
     * Matches the document given and the one indexed in this V-File.
     * 
     * @param currentVersion
     *            The document to compare to this index.
     * @throws NoMatchException
     *             If the document doesn't match.
     * @see {@link TaggedNode#matchNode(Node)}
     */
    public void matchDocument(Document currentVersion) throws NoMatchException {
        this.getDocumentElement().matchNode(currentVersion.getDocumentElement());
    }
}