package se.repos.vfile.gen;

import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
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

    public TaggedNode getParent() {
        return new TaggedNode(this.parentVFile, (Element) this.element.getParentNode());
    }

    public String getName() {
        if (this.isAttribute()) {
            return this.element.getAttribute(StringConstants.NAME);
        }
        return this.element.getTagName();
    }

    public String getValue() {
        if (this.isElement()) {
            return "";
        }
        return this.element.getTextContent();
    }

    public void setValue(String value) {
        TaggedNode newElem;
        if (this.isElement()) {
            throw new RuntimeException();
        } else if (this.isAttribute()) {
            newElem = this.parentVFile.createAttribute(this.getName(), value);
        } else {
            newElem = this.parentVFile.createText(value);
            this.element.setTextContent(value);
        }

        this.getParent().insertBefore(newElem, this);
        this.delete();
        this.element = newElem.element;
    }

    public void setName(String name) {
        TaggedNode parent = this.getParent();
        TaggedNode newElem;
        if (this.isText()) {
            throw new RuntimeException();
        } else if (this.isAttribute()) {
            newElem = this.parentVFile.createAttribute(name, this.getValue());
        } else {
            newElem = this.parentVFile.createTaggedNode(name);
        }
        for (TaggedNode attr : this.getAttributes()) {
            newElem.setAttribute(attr.getName(), attr.getValue());
        }
        for (TaggedNode child : this.getChildElements()) {
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
    public void delete() {
        if (!this.isLive()) {
            return;
        }
        for (TaggedNode attr : this.getAttributes()) {
            attr.delete();
        }
        for (TaggedNode elem : this.getChildElements()) {
            elem.delete();
        }
        this.element.setAttribute(StringConstants.END,
                this.parentVFile.getDocumentVersion());
        this.element.setAttribute(StringConstants.TEND,
                this.parentVFile.getDocumentTime());
    }

    public String getVStart() {
        return this.element.getAttribute(StringConstants.START);
    }

    public String getEnd() {
        return this.element.getAttribute(StringConstants.END);
    }

    public String getStart() {
        return this.element.getAttribute(StringConstants.TSTART);
    }

    public String getTEnd() {
        return this.element.getAttribute(StringConstants.TEND);
    }

    public boolean isLive() {
        return this.getTEnd().equals(StringConstants.NOW)
                && this.getEnd().equals(StringConstants.NOW);
    }

    public boolean isAttribute() {
        return this.element.getTagName().equals(StringConstants.ATTR);
    }

    public boolean isText() {
        return this.element.getNodeName().equals(StringConstants.TEXT);
    }

    public boolean isElement() {
        return !this.isAttribute() && !this.isText();
    }

    public TaggedNode getAttribute(String name) {
        for (TaggedNode a : this.getAttributes()) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    public ArrayList<TaggedNode> getAttributes() {
        ArrayList<TaggedNode> results = new ArrayList<TaggedNode>();
        for (TaggedNode child : this.elements(true)) {
            if (child.isAttribute()) {
                results.add(child);
            }
        }
        return results;
    }

    // Sets/creates an attribute on a index element.
    public void setAttribute(String name, String value) {
        TaggedNode attr = this.getAttribute(name);
        if (attr == null) {
            attr = this.parentVFile.createAttribute(name, value);
            this.element.appendChild(attr.element);
        } else {
            attr.setValue(value);
        }
    }

    public void setNamespace(Attr namespace) {
        if (!ElementUtils.isNameSpace(namespace)) {
            throw new IllegalArgumentException();
        }
        this.cloneElement();
        this.element.setAttribute(namespace.getName(), namespace.getValue());
    }

    public void deleteNamespace(Attr namespace) {
        if (!namespace.getPrefix().equals("xmlns")) {
            throw new IllegalArgumentException();
        }
        this.cloneElement();
        this.element.removeAttribute(namespace.getName());
    }

    public void deleteAttribute(String name) {
        TaggedNode attr = this.getAttribute(name);
        if (attr == null) {
            throw new RuntimeException("Tried to delete non-present attribute.");
        }
        attr.delete();
    }

    public void appendChild(TaggedNode child) {
        this.element.appendChild(child.element);
    }

    private void insertBefore(TaggedNode e, TaggedNode ref) {
        this.element.insertBefore(e.element, ref.element);
    }

    private void insertElementAt(TaggedNode e, int index) {
        ArrayList<TaggedNode> children = this.getChildElements();
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
        default:
            throw new UnsupportedOperationException();
        }
    }

    public void normalizeText(Text child) {
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
    public void normalizeElement(Element child) {
        TaggedNode newChild = this.parentVFile.createTaggedNode(child.getTagName());
        for (Attr a : ElementUtils.getNamespaces(child)) {
            newChild.element.setAttributeNode(a);
        }
        for (Attr a : ElementUtils.getAttributes(child)) {
            newChild.setAttribute(a.getName(), a.getValue());
        }
        for (Element c : ElementUtils.getChildElements(child)) {
            newChild.normalizeElement(c);
        }
        for (Text t : ElementUtils.getText(child)) {
            newChild.normalizeText(t);
        }
        int index = ElementUtils.getChildIndex(child);
        if (index == this.childCount()) {
            this.appendChild(newChild);
        } else {
            this.insertElementAt(newChild, index);
        }
    }

    public void deleteChildElement(Element target) {
        for (TaggedNode e : this.getChildElements()) {
            if (e.isEqualElement(target)) {
                e.delete();
                return;
            }
        }
        throw new RuntimeException("Tried to delete non-present element.");
    }

    private int childCount() {
        return this.getChildElements().size();
    }

    public ArrayList<TaggedNode> getChildElements() {
        ArrayList<TaggedNode> results = new ArrayList<TaggedNode>();
        for (TaggedNode child : this.elements(true)) {
            if (child.isElement()) {
                results.add(child);
            }
        }
        return results;
    }

    public ArrayList<TaggedNode> getText() {
        ArrayList<TaggedNode> results = new ArrayList<TaggedNode>();
        for (TaggedNode child : this.elements(true)) {
            if (child.isText()) {
                results.add(child);
            }
        }
        return results;
    }

    public ArrayList<TaggedNode> elements(boolean mustBeLive) {
        ArrayList<TaggedNode> results = new ArrayList<TaggedNode>();
        for (Element c : ElementUtils.getChildElements(this.element)) {
            TaggedNode child = new TaggedNode(this.parentVFile, c);
            if (!mustBeLive || child.isLive()) {
                results.add(child);
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
    public boolean isEqualElement(Element docElem) {
        // TODO Add comparison on name spaces.
        return this.isElement() && this.isLive()
                && this.element.getTagName().equals(docElem.getTagName())
                && this.hasSameText(docElem) && this.hasSameAttributes(docElem)
                && this.hasSameChildren(docElem);
    }

    private boolean hasSameText(Element docElem) {
        ArrayList<TaggedNode> thisText = this.getText();
        ArrayList<Text> thatText = ElementUtils.getText(docElem);
        if (thisText.size() != thatText.size()) {
            return false;
        }
        for (int i = 0; i < thisText.size(); i++) {
            if (!thisText.get(i).getValue().equals(thatText.get(i).getData())) {
                return false;
            }
        }
        return true;
    }

    private boolean hasSameChildren(Element docElem) {
        ArrayList<TaggedNode> theseChildren = this.getChildElements();
        ArrayList<Element> thoseChildren = ElementUtils.getChildElements(docElem);
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

    private boolean hasSameAttributes(Element docElem) {
        for (TaggedNode attr : this.getAttributes()) {
            String iVal = attr.getValue();
            String dVal = docElem.getAttribute(attr.getName());
            if (dVal == null || !iVal.equals(dVal)) {
                return false;
            }
        }
        for (Attr a1 : ElementUtils.getAttributes(docElem)) {
            TaggedNode a2 = this.getAttribute(a1.getName());
            if (a2 == null || !a1.getValue().equals(a2.getValue())) {
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

    /**
     * Reorders the position of this TaggedNode in the parent's child list, so
     * that this index element is at the position given, or at the end if the
     * index is larger than the child list length.
     * 
     * @param index
     *            The position to move this element to.
     */
    public void reorder(int index) {
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
    public void eraseChild(TaggedNode e) {
        this.element.removeChild(e.element);
    }
}
