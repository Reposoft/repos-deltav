package se.repos.vfile.gen;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.w3c.dom.Attr;
import org.w3c.dom.Comment;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
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

    private short getNodetype() {
        if (this.element.getTagName().equals(StringConstants.ATTR)) {
            return Node.ATTRIBUTE_NODE;
        } else if (this.element.getTagName().equals(StringConstants.TEXT)) {
            return Node.TEXT_NODE;
        } else if (this.element.getTagName().equals(StringConstants.COMMENT)) {
            return Node.COMMENT_NODE;
        } else if (this.element.getTagName().equals(StringConstants.PI)) {
            return Node.PROCESSING_INSTRUCTION_NODE;
        } else {
            return Node.ELEMENT_NODE;
        }
    }

    private TaggedNode getParent() {
        return new TaggedNode(this.parentVFile, (Element) this.element.getParentNode());
    }

    private String getName() {
        if (this.getNodetype() == Node.ATTRIBUTE_NODE) {
            return this.element.getAttribute(StringConstants.NAME);
        }
        return this.element.getTagName();
    }

    private String getValue() {
        if (this.getNodetype() == Node.ELEMENT_NODE) {
            return "";
        }
        return this.element.getTextContent();
    }

    private void setValue(String value) {
        TaggedNode newElem;
        switch (this.getNodetype()) {
        case Node.ELEMENT_NODE:
            throw new RuntimeException();
        case Node.ATTRIBUTE_NODE:
            newElem = this.parentVFile.createAttribute(this.getName(), value);
            break;
        case Node.TEXT_NODE:
            newElem = this.parentVFile.createText(value);
            break;
        default:
            throw new UnsupportedOperationException();
        }
        this.getParent().insertBefore(newElem, this);
        this.delete();
        this.element = newElem.element;
    }

    private void setName(String name) {
        TaggedNode parent = this.getParent();
        TaggedNode newElem = null;
        switch (this.getNodetype()) {
        case Node.TEXT_NODE:
            throw new RuntimeException();
        case Node.ATTRIBUTE_NODE:
            newElem = this.parentVFile.createAttribute(name, this.getValue());
            break;
        case Node.ELEMENT_NODE:
            newElem = this.parentVFile.createTaggedNode(name);
            break;
        default:
            throw new UnsupportedOperationException();
        }

        for (TaggedNode attr : this.getAttributes()) {
            newElem.setAttribute(attr.getName(), attr.getValue());
        }
        for (TaggedNode child : this.getChildren()) {
            newElem.appendChild(child);
        }
        parent.insertBefore(newElem, this);
        this.delete();
        this.element = newElem.element;
    }

    private void cloneElement() {
        this.setName(this.getName());
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

    private boolean isLive() {
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

    public TaggedNode getTextNode(int textIndex) {
        int i = 0;
        for (TaggedNode n : this.getChildren()) {
            if (n.getNodetype() == Node.TEXT_NODE) {
                if (i == textIndex) {
                    return n;
                }
                i++;
            }
        }
        return null;
    }

    private ArrayList<TaggedNode> getAttributes() {
        ArrayList<TaggedNode> results = new ArrayList<TaggedNode>();
        for (Element c : TaggedNode.getChildElements(this.element)) {
            TaggedNode child = new TaggedNode(this.parentVFile, c);
            if (child.isLive() && child.getNodetype() == Node.ATTRIBUTE_NODE) {
                results.add(child);
            }
        }
        return results;
    }

    // Sets/creates an attribute on a index element.
    private void setAttribute(String name, String value) {
        TaggedNode attr = this.getAttribute(name);
        if (attr == null) {
            attr = this.parentVFile.createAttribute(name, value);
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
        switch (child.getNodeType()) {
        case Node.TEXT_NODE:
            this.normalizeText((Text) child);
            return;
        case Node.ELEMENT_NODE:
            this.normalizeElement((Element) child);
            return;
        case Node.PROCESSING_INSTRUCTION_NODE:
            this.normalizeProcessInstruction((ProcessingInstruction) child);
            break;
        case Node.COMMENT_NODE:
            this.normalizeComment((Comment) child);
            break;
        default:
            throw new UnsupportedOperationException();
        }
    }

    private void normalizeComment(Comment child) {
        // TODO Auto-generated method stub
    }

    private void normalizeProcessInstruction(ProcessingInstruction child) {
        // TODO Auto-generated method stub
    }

    private void normalizeText(Text child) {
        TaggedNode text = this.parentVFile.createText(child.getData());
        int index = ElementUtils.getTextIndex(child);
        if (index == this.childCount()) {
            this.appendChild(text);
        } else {
            this.insertElementAt(text, index);
        }
    }

    /**
     * Adds a new element to this IndexNode. The element is assumed not to be
     * tagged, and is normalized to the current docVersion.
     */
    private void normalizeElement(Element child) {
        TaggedNode newChild = this.parentVFile.createTaggedNode(child.getTagName());
        for (Attr a : TaggedNode.getAttributes(child)) {
            newChild.setAttribute(a.getName(), a.getValue());
        }
        for (Node n : ElementUtils.getChildren(child)) {
            newChild.normalizeNode(n);
        }
        int index = TaggedNode.getChildIndex(child);
        if (index == this.childCount()) {
            this.appendChild(newChild);
        } else {
            this.insertElementAt(newChild, index);
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
                if (child.getNodetype() != Node.ATTRIBUTE_NODE) {
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
        if (!(this.getNodetype() == docNode.getNodeType() && this.isLive())) {
            return false;
        }
        switch (this.getNodetype()) {
        case Node.ATTRIBUTE_NODE:
            Attr docAttr = (Attr) docNode;
            return this.getValue().equals(docAttr.getValue())
                    && this.getName().equals(docNode.getNodeName());
        case Node.ELEMENT_NODE:
            Element docElem = (Element) docNode;
            return this.hasSameAttributes(docElem) && this.hasSameChildren(docElem);
        case Node.TEXT_NODE:
            String thisVal = this.getValue();
            String thatVal = ((Text) docNode).getData();
            return thisVal.equals(thatVal);
        case Node.PROCESSING_INSTRUCTION_NODE:
            return ((ProcessingInstruction) docNode).getData().equals(this.getValue());
        case Node.COMMENT_NODE:
            return ((Comment) docNode).getData().equals(this.getValue());
        default:
            throw new UnsupportedOperationException();
        }
    }

    private boolean hasSameAttributes(Element docElem) {
        ArrayList<Attr> thoseAttrs = TaggedNode.getAttributes(docElem);
        for (int i = 0; i < thoseAttrs.size(); i++) {
            Attr thatAttr = thoseAttrs.get(i);
            TaggedNode thisAttr = this.getAttribute(thatAttr.getName());
            if (thisAttr == null || !thisAttr.isEqualElement(thatAttr)) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSameChildren(Element docElem) {
        ArrayList<TaggedNode> theseChildren = this.getChildren();
        ArrayList<Node> thoseChildren = ElementUtils.getChildren(docElem);
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
        String val = this.getName();
        Iterator<TaggedNode> iter = this.getAttributes().iterator();
        if (iter.hasNext()) {
            TaggedNode attr = iter.next();
            val += ": " + attr.getName() + "=" + attr.getValue();
        }
        while (iter.hasNext()) {
            TaggedNode attr = iter.next();
            val += ", " + attr.getName() + "=" + attr.getValue();
        }
        return val;
    }

    // TODO Change element ordering.
    private void reorder(int index) {
        TaggedNode parent = this.getParent();
        parent.eraseChild(this);
        if (parent.childCount() == index) {
            parent.appendChild(this);
        } else {
            parent.insertElementAt(this, index);
        }
        this.element.setAttribute(StringConstants.REORDER,
                this.parentVFile.getDocumentVersion());
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
            MultiMap<String, Node> newNodeMap) {
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
            case ELEM_NAME:
                this.setName(d.testNode.getNodeName());
                break;
            case ATTR_VALUE:
                Attr newAttr = (Attr) d.testNode;
                this.setValue(newAttr.getValue());
                break;
            case ELEM_ATTRS:
                this.updateElementAttrs((Element) d.controlNode, (Element) d.testNode);
                break;
            case TEXT_VALUE:
                this.setValue(d.testNode.getTextContent());
                break;
            case ELEM_CHILDREN_ORDER:
                int location = TaggedNode.getChildIndex((Element) d.testNode);
                this.reorder(location);
                break;
            case IGNORED:
                break;
            }
        }
    }

    private void updateElementAttrs(Element oldElement, Element newElement) {
        for (Attr oldNS : TaggedNode.getNamespaces(oldElement)) {
            if (!TaggedNode.hasEqualAttribute(newElement, oldNS)) {
                this.deleteNamespace(oldNS);
            }
        }
        for (Attr newNS : TaggedNode.getNamespaces(newElement)) {
            if (!TaggedNode.hasEqualAttribute(oldElement, newNS)) {
                this.setNamespace(newNS);
            }
        }
        for (Attr oldAttr : TaggedNode.getAttributes(oldElement)) {
            if (!TaggedNode.hasEqualAttribute(newElement, oldAttr)) {
                this.deleteAttribute(oldAttr.getName());
            }
        }
        for (Attr newAttr : TaggedNode.getAttributes(newElement)) {
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

    /**
     * Retrieves the attributes elements of a node.
     * 
     * @param element
     *            The parent node.
     * @return The list of attributes of the element.
     */
    private static ArrayList<Attr> getAttributes(Element element) {
        ArrayList<Attr> results = new ArrayList<Attr>();
        NamedNodeMap attrs = element.getAttributes();
        if (attrs == null) {
            return results;
        }
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr a = (Attr) attrs.item(i);
            if (!TaggedNode.isNameSpace(a)) {
                results.add(a);
            }
        }
        return results;
    }

    /**
     * Retrieves the name space declarations of a node.
     * 
     * @param element
     *            The parent node.
     * @return The list of name space declarations of the element.
     */
    private static ArrayList<Attr> getNamespaces(Element element) {
        ArrayList<Attr> results = new ArrayList<Attr>();
        NamedNodeMap attrs = element.getAttributes();
        if (attrs == null) {
            return results;
        }
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr a = (Attr) attrs.item(i);
            if (TaggedNode.isNameSpace(a)) {
                results.add(a);
            }
        }
        return results;
    }

    private static boolean isNameSpace(Attr a) {
        return a.getName().startsWith("xmlns:");
    }

    private void setNamespace(Attr namespace) {
        if (!TaggedNode.isNameSpace(namespace)) {
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

    private void updateElementChildren(MultiMap<String, Node> newNodeMap,
            String testNodeLocation) {
        for (Node n : newNodeMap.remove(testNodeLocation)) {
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
                this.normalizeElement(e);
            }
        }
    }

    /**
     * Retrieves at which index position of it's parent node you can find child.
     * 
     * @throws RuntimeException
     *             If the parent node of child does not contain an equal node.
     */
    private static int getChildIndex(Element child) {
        Element parent = (Element) child.getParentNode();
        int i = 0;
        for (Element e : getChildElements(parent)) {
            if (e.equals(child)) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("Element not found.");
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
