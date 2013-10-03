package se.repos.vfile.gen;

import java.util.ArrayList;
import java.util.Iterator;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;

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
        if (!element.hasAttribute(StringConstants.VSTART)
                || !element.hasAttribute(StringConstants.VEND)
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

    public VFile getParentIndex() {
        return this.parentVFile;
    }

    public String getName() {
        return this.element.getTagName();
    }

    public String getValue() {
        return ElementUtils.getValue(this.element);
    }

    public void setValue(String value) {
        this.setNameValue(this.getNameSpaceURI(), this.getName(), value);
    }

    public String getNameSpaceURI() {
        return this.element.getNamespaceURI();
    }

    public void setNameValue(String nameSpaceURI, String name, String value) {
        TaggedNode parent = this.getParent();
        TaggedNode newElem;
        if (this.isAttribute()) {
            newElem = this.parentVFile.createAttribute(nameSpaceURI, name, value);
        } else {
            newElem = this.parentVFile.createTaggedNode(nameSpaceURI, name);
            ElementUtils.setValue(newElem.element, value);
        }
        for (TaggedNode attr : this.getAttributes()) {
            newElem.setAttribute(nameSpaceURI, attr.getName(), attr.getValue());
        }
        for (TaggedNode child : this.getChildElements()) {
            newElem.appendChild(child);
        }

        parent.insertBefore(newElem, this);
        this.delete();
        this.element = newElem.element;
    }

    /**
     * "Deletes" a tagged node, i.e sets it's VEND attrbiute to the current
     * docVersion. Also deletes all this TaggedNodes children and attributes.
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
        this.element.setAttribute(StringConstants.VEND,
                this.parentVFile.getDocumentVersion());
        this.element.setAttribute(StringConstants.TEND,
                this.parentVFile.getDocumentTime());
    }

    public String getVStart() {
        return this.element.getAttribute(StringConstants.VSTART);
    }

    public String getVEnd() {
        return this.element.getAttribute(StringConstants.VEND);
    }

    public String getTStart() {
        return this.element.getAttribute(StringConstants.TSTART);
    }

    public String getTEnd() {
        return this.element.getAttribute(StringConstants.TEND);
    }

    public boolean isLive() {
        return this.getTEnd().equals(StringConstants.NOW)
                && this.getVEnd().equals(StringConstants.NOW);
    }

    public boolean isAttribute() {
        return this.element.getAttribute(StringConstants.ISATTR).equals(
                StringConstants.YES);
    }

    public boolean hasAttribute(String nameSpaceURI, String name) {
        return this.getAttribute(nameSpaceURI, name) != null;
    }

    public TaggedNode getAttribute(String nameSpaceURI, String name) {
        for (TaggedNode a : this.getAttributes()) {
            if (a.getName().equals(name) && a.getNameSpaceURI().equals(nameSpaceURI)) {
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
    public TaggedNode setAttribute(String nameSpaceURI, String name, String value) {
        TaggedNode attr = this.getAttribute(nameSpaceURI, name);
        if (attr == null) {
            attr = this.parentVFile.createAttribute(nameSpaceURI, name, value);
            this.element.appendChild(attr.element);
        } else {
            attr.setValue(value);
        }
        return attr;
    }

    public void deleteAttribute(String nameSpaceURI, String name) {
        TaggedNode attr = this.getAttribute(nameSpaceURI, name);
        if (attr == null) {
            throw new RuntimeException("Tried to delete non-present attribute.");
        }
        attr.delete();
    }

    public void appendChild(TaggedNode child) {
        this.element.appendChild(child.element);
    }

    public void insertBefore(TaggedNode element, TaggedNode ref) {
        this.element.insertBefore(element.element, ref.element);
    }

    public void insertElementAt(TaggedNode e, int index) {
        ArrayList<TaggedNode> children = this.getChildElements();
        TaggedNode ref = children.get(index);
        this.element.insertBefore(e.element, ref.element);
    }

    /**
     * Adds a new element to this IndexNode. The element is assumed not to be
     * tagged, and is normalized to the current docVersion.
     */
    public void normalizeElement(Element child) {
        TaggedNode newChild = this.parentVFile.createTaggedNode(child.getNamespaceURI(),
                child.getTagName());
        for (Attr a : ElementUtils.getAttributes(child)) {
            newChild.setAttribute(a.getNamespaceURI(), a.getName(), a.getValue());
        }
        for (Element c : ElementUtils.getChildElements(child)) {
            newChild.normalizeElement(c);
        }
        String value = ElementUtils.getValue(child);
        if (!value.isEmpty()) {
            ElementUtils.setValue(newChild.element, value);
        }
        int index = ElementUtils.getChildIndex(child);
        if (index == this.childCount()) {
            this.appendChild(newChild);
        } else {
            this.insertElementAt(newChild, index);
        }
    }

    public void deleteChildElement(Element e) {
        TaggedNode elem = this.getEqualElement(e);
        if (elem == null) {
            throw new RuntimeException("Tried to delete non-present element.");
        }
        elem.delete();
    }

    public int childCount() {
        return this.getChildElements().size();
    }

    public ArrayList<TaggedNode> getChildElements() {
        return this.getChildElements(true);
    }

    public ArrayList<TaggedNode> getChildElements(boolean mustBeLive) {
        ArrayList<TaggedNode> results = new ArrayList<TaggedNode>();
        for (TaggedNode child : this.elements(mustBeLive)) {
            if (!child.isAttribute()) {
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
     * TaggedNode. The comparsion only takes into account live
     * children/attributes of the tagged node.
     * 
     * @return True if the elements are equal.
     */
    public boolean isEqualElement(Element docElem) {
        return !this.isAttribute() && this.isLive()
                && this.element.getTagName().equals(docElem.getTagName())
                && this.hasEqualValue(docElem) && this.hasSameAttributes(docElem)
                && this.hasSameChildren(docElem);
    }

    private boolean hasEqualValue(Element docElem) {
        String thisValue = ElementUtils.getValue(this.element);
        String docValue = ElementUtils.getValue(docElem);
        return docValue.equals(thisValue);
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
            TaggedNode a2 = this.getAttribute(a1.getNamespaceURI(), a1.getName());
            if (a2 == null || !a1.getValue().equals(a2.getValue())) {
                return false;
            }
        }
        return true;
    }

    public boolean hasEqualElement(Element target) {
        return this.getEqualElement(target) != null;
    }

    public TaggedNode getEqualElement(Element target) {
        for (TaggedNode e : this.getChildElements()) {
            if (e.isEqualElement(target)) {
                return e;
            }
        }
        return null;
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
    }

    /**
     * Permanently deletes e from this TaggedNode's child list.
     */
    public void eraseChild(TaggedNode e) {
        this.element.removeChild(e.element);
    }
}
