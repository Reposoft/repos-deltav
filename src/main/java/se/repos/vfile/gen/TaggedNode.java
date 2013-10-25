package se.repos.vfile.gen;

import java.util.ArrayList;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;
import org.w3c.dom.Text;

/**
 * @author Hugo Svallfors <keiter@lavabit.com> A single element of a VFile.
 *         Corresponds to a single tagged node.
 * @see VFile
 */
public class TaggedNode {

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
                || !element.hasAttribute(StringConstants.TSTART)
                || !element.hasAttribute(StringConstants.TEND)) {
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

    private String getName() {
        switch (this.getNodetype()) {
        case ATTRIBUTE:
        case PROCESSING_INSTRUCTION:
            return this.element.getAttribute(StringConstants.NAME);
        default:
            return this.element.getTagName();
        }
    }

    private String getValue() {
        switch (this.getNodetype()) {
        case ELEMENT:
        case DOCUMENT:
            return null;
        default:
            return this.element.getTextContent();
        }
    }

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
        default:
            throw new UnsupportedOperationException();
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
        this.element.setAttribute(StringConstants.TEND,
                this.parentVFile.getDocumentTime());
        if (this.getTStart().equals(this.getTEnd())
                && this.getStart().equals(this.getEnd())) {
            this.getParent().eraseChild(this);
        }
    }

    public boolean isLive() {
        return this.getTEnd().equals(StringConstants.NOW)
                && this.getEnd().equals(StringConstants.NOW);
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

    public TaggedNode getNthNodeOfType(int textIndex, Nodetype nodeType) {
        int i = 0;
        for (TaggedNode n : this.getChildren()) {
            if (n.getNodetype() == nodeType) {
                if (i == textIndex) {
                    return n;
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

    // Sets/creates an attribute on a index element.
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
        TaggedNode ref = children.get(index);
        this.element.insertBefore(e.element, ref.element);
    }

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

        int index = ElementUtils.getChildIndex(child);
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
     * Test of equality between a normal element and the tagged node of this
     * TaggedNode. The comparison only takes into account live
     * children/attributes of the tagged node.
     * 
     * @return True if the elements are equal.
     */
    public boolean isEqualElement(Node docNode) {
        if (!(this.getNodetype() == ElementUtils.getNodeType(docNode) && this.isLive())) {
            return false;
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
            b = this.hasSameAttributes(docElem) && this.hasSameChildren(docElem);
            break;
        case TEXT:
            b = this.getValue().equals(((Text) docNode).getData());
            break;
        case PROCESSING_INSTRUCTION:
            b = ((ProcessingInstruction) docNode).getData().equals(this.getValue());
            break;
        case COMMENT:
            b = ((Comment) docNode).getData().equals(this.getValue());
            break;
        default:
            throw new UnsupportedOperationException();
        }
        if (!b) {
            return false; // for setting breakpoint on
        }
        return true;
    }

    private boolean hasSameAttributes(Element docElem) {
        ArrayList<Attr> thoseAttrs = ElementUtils.getAttributes(docElem);
        for (int i = 0; i < thoseAttrs.size(); i++) {
            Attr thatAttr = thoseAttrs.get(i);
            TaggedNode thisAttr = this.getAttribute(thatAttr.getName());
            if (thisAttr == null || !thisAttr.isEqualElement(thatAttr)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSameChildren(Node docNode) {
        ArrayList<TaggedNode> theseChildren = this.getChildren();
        ArrayList<Node> thoseChildren = ElementUtils.getChildren(docNode);
        if (theseChildren.size() != thoseChildren.size()) {
            return false;
        }
        for (int i = 0; i < theseChildren.size(); i++) {
            if (!theseChildren.get(i).isEqualElement(thoseChildren.get(i))) {
                return false;
            }
        }
        return true;
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
        DeferredChanges d = changeMap.get(this);
        for (CHANGE change : d.changes) {
            switch (change) {
            case NODE_NOT_FOUND:
                this.delete();
                return;
            case ELEM_CHILDREN_NUMBER:
                this.updateElementChildren(newNodeMap, d.testLocation);
                break;
            case HAS_CHILD:
                this.updateElementChild((Element) d.controlNode, (Element) d.testNode);
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
            case PI_DATA:
                this.setValue(d.testNode.getTextContent());
                break;
            case ELEM_CHILDREN_ORDER:
                throw new RuntimeException(); // should not occur here.
            }
        }
    }

    public void reorder(int index) {
        // TODO This is bugged.
        TaggedNode parent = this.getParent();
        if (parent.childCount() == index) {
            parent.eraseChild(this);
            parent.appendChild(this);
        } else {
            parent.eraseChild(this);
            parent.insertElementAt(this, index);
        }
        this.element.setAttribute(StringConstants.REORDER,
                this.parentVFile.getDocumentVersion());
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

    private void updateElementChild(Element oldElement, Element newElement) {
        ArrayList<Element> newElements = TaggedNode.getChildElements(newElement);
        ArrayList<Element> oldElements = TaggedNode.getChildElements(oldElement);

        if (newElements.isEmpty() && oldElements.isEmpty()) {
            throw new RuntimeException("Missing child element.");
        } else if (!newElements.isEmpty() && !oldElements.isEmpty()) {
            throw new RuntimeException("Found two child elements where expected one.");
        } else if (newElements.isEmpty()) {
            for (Element e : oldElements) {
                this.deleteChildElement(e);
            }
        } else if (oldElements.isEmpty()) {
            for (Element e : newElements) {
                this.normalizeNode(e);
            }
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

    private void deleteChildElement(Element target) {
        for (TaggedNode e : this.getChildren()) {
            if (e.isEqualElement(target)) {
                e.delete();
                return;
            }
        }
        throw new RuntimeException("Tried to delete non-present element.");
    }
}
