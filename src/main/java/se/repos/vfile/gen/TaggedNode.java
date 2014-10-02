package se.repos.vfile.gen;

import java.util.ArrayList;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.helpers.MessageFormatter;
import org.w3c.dom.Attr;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * A single element of a {@link VFile}. Corresponds to a single tagged node.
 */
public class TaggedNode {

	private static final Logger logger = LoggerFactory.getLogger(TaggedNode.class);
	
    private VFile parentVFile;
    private Element element;

    /**
     * Constructs a new TaggedNode.
     * 
     * @param element
     *            The tagged node.
     * @param parentIndex
     *            The index the node belongs to.
     * @throws NullPointerException
     *             If either of the parameters are null.
     * @throws IllegalArgumentException
     *             If the tagged node is not tagged with lifetime attributes.
     */
    public TaggedNode(VFile parentIndex, Element element) {
        if (parentIndex == null || element == null) {
            throw new NullPointerException();
        }
        if (!element.hasAttribute(StringConstants.START)
                || !element.hasAttribute(StringConstants.END)
                /*
                || !element.hasAttribute(StringConstants.TSTART)
                || !element.hasAttribute(StringConstants.TEND)*/) {
            throw new IllegalArgumentException("Missing lifetime information on element.");
        }
        this.parentVFile = parentIndex;
        this.element = element;
    }

    public Element toElement() {
        return this.element;
    }

    public Nodetype getNodetype() {
        String tagName = this.element.getTagName();
        if (tagName.equals(StringConstants.ATTR)) {
            return Nodetype.ATTRIBUTE;
        } else if (tagName.equals(StringConstants.TEXT)) {
            return Nodetype.TEXT;
        } else if (tagName.equals(StringConstants.COMMENT)) {
            return Nodetype.COMMENT;
        } else if (tagName.equals(StringConstants.PI)) {
            return Nodetype.PROCESSING_INSTRUCTION;
        } else if (tagName.equals(StringConstants.FILE)) {
            return Nodetype.DOCUMENT;
        } else {
            return Nodetype.ELEMENT;
        }
    }

    private TaggedNode getParent() {
        return new TaggedNode(this.parentVFile, (Element) this.element.getParentNode());
    }

    public String getName() {
        switch (this.getNodetype()) {
        case ATTRIBUTE:
        case PROCESSING_INSTRUCTION:
            return this.element.getAttribute(StringConstants.NAME);
        default:
            return this.element.getTagName();
        }
    }

    public String getValue() {
        switch (this.getNodetype()) {
        case ELEMENT:
        case DOCUMENT:
            return null;
        default:
            return this.element.getTextContent();
        }
    }

    // Looks a lot like clone to me.
    private void setValue(String value) {
        TaggedNode newElem;
        switch (this.getNodetype()) {
        case ATTRIBUTE:
            newElem = this.parentVFile.createTaggedNode(StringConstants.ATTR,
                    this.getName(), value);
            break;
        case TEXT:
            newElem = this.parentVFile
                    .createTaggedNode(StringConstants.TEXT, null, value);
            break;
        case PROCESSING_INSTRUCTION:
            newElem = this.parentVFile.createTaggedNode(StringConstants.PI,
                    this.getName(), value);
            break;
        case COMMENT:
            newElem = this.parentVFile.createTaggedNode(StringConstants.COMMENT, null,
                    value);
            break;
        case ELEMENT:
        	// Adding this code to make cloneElement() work for ELEMENT. 
        	// More or less copied from normalizeNode.
            newElem = this.parentVFile.createTaggedNode(this.element.getTagName(), null, null);
            for (TaggedNode a : this.getAttributes()) {
            	newElem.setAttribute(a.getName(), a.getValue());
            }
            for (TaggedNode n : this.getChildren()) {
            	newElem.appendChild(n);
            }
            break;
        default:
            throw new UnsupportedOperationException(this.getNodetype().name());
        }
        this.getParent().insertBefore(newElem, this);
        this.delete();
        this.element = newElem.element;
    }

    private void cloneElement() {
        this.setValue(this.getValue());
    }

    /**
     * "Deletes" a tagged node, i.e sets it's VEND/TEND attributes to the
     * current docVersion. Also deletes all this TaggedNodes children and
     * attributes.
     */
    private void delete() {
        if (!this.isLive()) {
            return;
        }
        for (TaggedNode attr : this.getAttributes()) {
            attr.delete();
        }
        for (TaggedNode elem : this.getChildren()) {
            elem.delete();
        }
        this.element.setAttribute(StringConstants.END,
                this.parentVFile.getDocumentVersion());
        /*
        this.element.setAttribute(StringConstants.TEND,
                this.parentVFile.getDocumentTime());
                */
        if (this.getTStart().equals(this.getTEnd())
                && this.getStart().equals(this.getEnd())) {
            this.getParent().eraseChild(this);
        }
    }

    public boolean isLive() {
        return this.getEnd().equals(StringConstants.NOW); // No need to test on both end-attributes.
    }

    private String getStart() {
        return this.element.getAttribute(StringConstants.START);
    }

    private String getEnd() {
        return this.element.getAttribute(StringConstants.END);
    }

    private String getTStart() {
        return this.element.getAttribute(StringConstants.TSTART);
    }

    private String getTEnd() {
        return this.element.getAttribute(StringConstants.TEND);
    }

    public TaggedNode getAttribute(String name) {
        for (TaggedNode a : this.getAttributes()) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    /**
     * Finds the nth child node of the given type.
     * 
     * @param n
     *            The index of the needle.
     * @param nodeType
     *            The {@link Nodetype} of the needle.
     * @return The needle, or null if not found.
     */
    public TaggedNode getNthNodeOfType(int n, Nodetype nodeType) {
        int i = 0;
        for (TaggedNode child : this.getChildren()) {
            if (child.getNodetype() == nodeType) {
                if (i == n) {
                    return child;
                }
                i++;
            }
        }
        return null;
    }

    public ArrayList<TaggedNode> getElementsByTagName(String tagName) {
        ArrayList<TaggedNode> results = new ArrayList<TaggedNode>();
        for (TaggedNode child : this.getChildren()) {
            if (child.getNodetype() == Nodetype.ELEMENT
                    && child.getName().equals(tagName)) {
                results.add(child);
            }
        }
        return results;
    }

    private ArrayList<TaggedNode> getAttributes() {
        ArrayList<TaggedNode> results = new ArrayList<TaggedNode>();
        for (Element c : TaggedNode.getChildElements(this.element)) {
            TaggedNode child = new TaggedNode(this.parentVFile, c);
            if (child.isLive() && child.getNodetype() == Nodetype.ATTRIBUTE) {
                results.add(child);
            }
        }
        return results;
    }

    public void setAttribute(String name, String value) {
        TaggedNode attr = this.getAttribute(name);
        if (attr == null) {
            attr = this.parentVFile.createTaggedNode(StringConstants.ATTR, name, value);
            this.element.appendChild(attr.element);
        } else {
            attr.setValue(value);
        }
    }

    private void appendChild(TaggedNode child) {
        this.element.appendChild(child.element);
    }

    private void insertBefore(TaggedNode e, TaggedNode ref) {
        this.element.insertBefore(e.element, ref.element);
    }

    private void insertElementAt(TaggedNode e, int index) {
        ArrayList<TaggedNode> children = this.getChildren();
        if (index >= children.size()) {
            throw new IndexOutOfBoundsException("When inserting node \"" + e.getName()
                    + "\" at local index " + index + " in node " + this.getXPath());
        }
        TaggedNode ref = children.get(index);
        this.element.insertBefore(e.element, ref.element);
    }

    /**
     * Given a DOM {@link Node}, adds a corresponding {@link TaggedNode} to this
     * node's child list. The new {@link TaggedNode} will be inserted at the
     * same local index as it's corresponding node.
     * 
     * @see ElementUtils.getLocalIndex
     */
    public void normalizeNode(Node child) {
        TaggedNode norm;
        switch (child.getNodeType()) {
        case Node.TEXT_NODE:
            Text t = (Text) child;
            norm = this.parentVFile.createTaggedNode(StringConstants.TEXT, null,
                    t.getData());
            break;
        case Node.ELEMENT_NODE:
            Element e = (Element) child;
            norm = this.parentVFile.createTaggedNode(e.getTagName(), null, null);
            for (Attr a : ElementUtils.getAttributes(e)) {
                norm.setAttribute(a.getName(), a.getValue());
            }
            for (Node n : ElementUtils.getChildren(child)) {
                norm.normalizeNode(n);
            }
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            ProcessingInstruction p = (ProcessingInstruction) child;
            norm = this.parentVFile.createTaggedNode(StringConstants.PI, p.getTarget(),
                    p.getData());
            break;
        case Node.COMMENT_NODE:
            Comment c = (Comment) child;
            norm = this.parentVFile.createTaggedNode(StringConstants.COMMENT, null,
                    c.getData());
            break;
        default:
            throw new UnsupportedOperationException();
        }

        int index = ElementUtils.getLocalIndex(child);
        if (index == this.childCount() || this.getNodetype() == Nodetype.DOCUMENT) {
        	this.appendChild(norm);
        } else {
            this.insertElementAt(norm, index);
        }
    }

    private int childCount() {
        return this.getChildren().size();
    }

    private ArrayList<TaggedNode> getChildren() {
        ArrayList<TaggedNode> results = new ArrayList<TaggedNode>();
        for (Element c : TaggedNode.getChildElements(this.element)) {
            TaggedNode child = new TaggedNode(this.parentVFile, c);
            if (child.isLive()) {
                if (child.getNodetype() != Nodetype.ATTRIBUTE) {
                    results.add(child);
                }
            }
        }
        return results;
    }

    /**
     * Test of equality between a normal node and the tagged node of this
     * TaggedNode. The comparison only takes into account live
     * children/attributes of the tagged node.
     * 
     * @throws NoMatchException
     *             If the nodes are unequal.
     */
    public void matchNode(Node docNode) throws NoMatchException {
        if (!(this.getNodetype() == ElementUtils.getNodeType(docNode) && this.isLive())) {
            throw new NoMatchException(new SimpleXPath(docNode), this.getXPath());
        }
        boolean b;
        switch (this.getNodetype()) {
        case ATTRIBUTE:
            Attr docAttr = (Attr) docNode;
            b = this.getName().equals(docNode.getNodeName())
                    && this.getValue().equals(docAttr.getValue());
            break;
        case ELEMENT:
            Element docElem = (Element) docNode;
            if (!this.getName().equals(docElem.getTagName())) {
                throw new NoMatchException(new SimpleXPath(docNode), this.getXPath());
            }
            this.matchAttributes(docElem);
            this.matchChildren(docElem);
            b = true;
            break;
        case TEXT:
        case PROCESSING_INSTRUCTION:
        case COMMENT:
            String thisValue = this.getValue();
            String thatValue;
            if (this.getNodetype() == Nodetype.PROCESSING_INSTRUCTION) {
                thatValue = ((ProcessingInstruction) docNode).getData();
            } else {
                thatValue = ((CharacterData) docNode).getData();
            }
            b = thisValue.trim().equals(thatValue.trim());
            break;
        default:
            throw new UnsupportedOperationException();
        }
        if (!b) {
            throw new NoMatchException(new SimpleXPath(docNode), this.getXPath());
        }
    }

    private void matchAttributes(Element docElem) throws NoMatchException {
        ArrayList<Attr> thoseAttrs = ElementUtils.getAttributes(docElem);
        for (int i = 0; i < thoseAttrs.size(); i++) {
            Attr thatAttr = thoseAttrs.get(i);
            TaggedNode thisAttr = this.getAttribute(thatAttr.getName());
            if (thisAttr == null) {
                throw new NoMatchException(new SimpleXPath(docElem), this.getXPath());
            }
            thisAttr.matchNode(thatAttr);
        }
    }

    private void matchChildren(Node docNode) throws NoMatchException {
        ArrayList<TaggedNode> theseChildren = this.getChildren();
        ArrayList<Node> thoseChildren = ElementUtils.getChildren(docNode);
        if (theseChildren.size() != thoseChildren.size()) {
            throw new NoMatchException(new SimpleXPath(docNode), this.getXPath());
        }
        for (int i = 0; i < theseChildren.size(); i++) {
            theseChildren.get(i).matchNode(thoseChildren.get(i));
        }
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TaggedNode)) {
            return false;
        }
        TaggedNode that = (TaggedNode) o;
        return this.parentVFile.equals(that.parentVFile)
                && this.element.equals(that.element);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + this.parentVFile.hashCode();
        hash = 79 * hash + this.element.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        switch (this.getNodetype()) {
        case ATTRIBUTE:
            return this.getName() + "=" + "\"" + this.getValue() + "\"";
        default:
            return "[" + this.getName() + ": " + this.getValue() + "]";
        }
    }

    private SimpleXPath getXPath() {
        SimpleXPath xPath = new SimpleXPath();
        TaggedNode current = this;
        while (current.getNodetype() != Nodetype.DOCUMENT) {
            xPath.addFirst(new Axis(current.getName(), current.getNodetype(), current
                    .getLocalIndex()));
            current = current.getParent();
        }
        return xPath;
    }

    private int getLocalIndex() {
        return ElementUtils.getLocalIndex(this.element, true);
    }

    /**
     * Permanently deletes e from this TaggedNode's child list.
     */
    private void eraseChild(TaggedNode e) {
        this.element.removeChild(e.element);
    }

    /**
     * Does a bottom-up update of the XML index.
     * 
     * @param changeMap
     *            A map of TaggedNodes to changes to be performed on that node.
     * @param newNodeMap
     *            A map from XPath location of an element to the new nodes that
     *            are to be added there.
     */
    public void updateTaggedNode(Map<TaggedNode, DeferredChanges> changeMap,
            MultiMap<SimpleXPath, Node> newNodeMap) {
        for (TaggedNode child : this.getAttributes()) {
            child.updateTaggedNode(changeMap, newNodeMap);
        }
        for (TaggedNode child : this.getChildren()) {
            child.updateTaggedNode(changeMap, newNodeMap);
        }
        if (!changeMap.containsKey(this)) {
            return;
        }
        DeferredChanges d = changeMap.remove(this);
        for (CHANGE change : d.changes) {
            switch (change) {
            case NODE_NOT_FOUND:
                this.delete();
                return;
            case ELEM_CHILDREN_NUMBER:
                this.updateElementChildren(newNodeMap, d.testLocation);
                break;
            case ELEM_ATTRS:
                this.updateElementAttrs((Element) d.controlNode, (Element) d.testNode);
                break;
            case ATTR_VALUE:
                Attr newAttr = (Attr) d.testNode;
                this.setValue(newAttr.getValue());
                break;
            case TEXT_VALUE:
            case COMMENT_VALUE:
                this.setValue(((CharacterData) d.testNode).getData());
                break;
            case PI_DATA:
                this.setValue(((ProcessingInstruction) d.testNode).getData());
                break;
            case ELEM_CHILDREN_ORDER:
                break; // dealt with in VFile.reorderNodes.e
            }
        }
        changeMap.put(this, d); // updates key in case node deleting change
                                // occured.
    }

    public void reorder(int index) {
    	
    	if (false) 
    		return;
    	
        TaggedNode parent = this.getParent();
        int childCount = parent.childCount();
        String reorderId = this.parentVFile.getReorderId();
        int currentIndex = ElementUtils.getLocalIndex(this.element, false, true, true);
        int relativeIndex = index - currentIndex;
        if (relativeIndex == 0) {
        	logger.debug("Reordering {} {} is not needed: {} = {}", reorderId, this.getName(), index, currentIndex);
        	return;
        }
        Integer vfileIndex = null;
        Integer searchIndex = null;
        Node e = element;
        if (relativeIndex < 0) {
        	// Searching for the correct location.

        	while ((e = e.getPreviousSibling()) != null) {
        		
        		searchIndex = ElementUtils.getLocalIndex(e, false, true, true);
        		if (searchIndex == index) {
        			break;
        		}
        	}
        } else {
        		while ((e = e.getNextSibling()) != null) {
        		
        		searchIndex = ElementUtils.getLocalIndex(e, false, true, true);
        		if (searchIndex == index) {
        			break;
        		}
        	}
        }
        if (searchIndex == null || searchIndex != index) {
			throw new RuntimeException("Reordering failed to find the expected index: " + index);
		}
        if (!(e instanceof Element)) {
        	// The VFile should not contain direct text/comment/PI nodes in this location.
        	throw new RuntimeException("Reordering encountered unexpected node type " + e.getNodeType());
        }
        vfileIndex = ElementUtils.getLocalIndex((Element) e, false, true, true);
        
        this.element.setAttribute(StringConstants.REORDERID, reorderId);
        logger.warn("Reordering {} {} from {} to index {} (VFile index {})", reorderId, this.getName(), currentIndex, index, vfileIndex);
        this.cloneElement();
        if (childCount == vfileIndex || childCount - 1 == vfileIndex) {
            parent.eraseChild(this);
            parent.appendChild(this);
        } else {
            parent.eraseChild(this);
            parent.insertElementAt(this, vfileIndex);
        }
        this.element.setAttribute(StringConstants.REORDER,
                this.parentVFile.getDocumentVersion());
        
        int reorderedIndex = ElementUtils.getLocalIndex(this.element, false, true, true);
        if (reorderedIndex != index) {
        	String msg = MessageFormatter.format("Reordering did not produce expected result: {} {}", index, reorderedIndex).getMessage();
        	logger.error(msg);
        }
    }

    private void updateElementAttrs(Element oldElement, Element newElement) {
        for (Attr oldNS : ElementUtils.getNamespaces(oldElement)) {
            if (!TaggedNode.hasEqualAttribute(newElement, oldNS)) {
                this.deleteNamespace(oldNS);
            }
        }
        for (Attr newNS : ElementUtils.getNamespaces(newElement)) {
            if (!TaggedNode.hasEqualAttribute(oldElement, newNS)) {
                this.setNamespace(newNS);
            }
        }
        for (Attr oldAttr : ElementUtils.getAttributes(oldElement)) {
            if (!TaggedNode.hasEqualAttribute(newElement, oldAttr)) {
                this.deleteAttribute(oldAttr.getName());
            }
        }
        for (Attr newAttr : ElementUtils.getAttributes(newElement)) {
            if (!TaggedNode.hasEqualAttribute(oldElement, newAttr)) {
                this.setAttribute(newAttr.getName(), newAttr.getValue());
            }
        }
    }

    /**
     * Method that checks whether elem has an element equal to attr.
     * 
     * @param elem
     *            The element to search.
     * @param attr
     *            The attribute to search for in elem.
     * @return Whether elem contains attr.
     */
    private static boolean hasEqualAttribute(Element elem, Attr attr) {
        return elem.hasAttribute(attr.getName())
                && elem.getAttribute(attr.getName()).equals(attr.getValue());
    }

    private void setNamespace(Attr namespace) {
        if (!ElementUtils.isNameSpace(namespace)) {
            throw new IllegalArgumentException();
        }
        this.cloneElement();
        this.element.setAttribute(namespace.getName(), namespace.getValue());
    }

    private void deleteNamespace(Attr namespace) {
        if (!namespace.getPrefix().equals("xmlns")) {
            throw new IllegalArgumentException();
        }
        this.cloneElement();
        this.element.removeAttribute(namespace.getName());
    }

    private void deleteAttribute(String name) {
        TaggedNode attr = this.getAttribute(name);
        if (attr == null) {
            throw new RuntimeException("Tried to delete non-present attribute.");
        }
        attr.delete();
    }

    private void updateElementChildren(MultiMap<SimpleXPath, Node> newNodeMap,
            SimpleXPath testLocation) {
        for (Node n : newNodeMap.remove(testLocation)) {
            this.normalizeNode(n);
        }
    }

    /**
     * Retrieves the child elements of a node.
     * 
     * @param element
     *            The parent node.
     * @return The list of child elements of element.
     */
    private static ArrayList<Element> getChildElements(Element element) {
        ArrayList<Element> results = new ArrayList<Element>();
        NodeList children = element.getChildNodes();
        if (children == null) {
            return results;
        }
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.ELEMENT_NODE) {
                results.add((Element) children.item(i));
            }
        }
        return results;
    }
}
