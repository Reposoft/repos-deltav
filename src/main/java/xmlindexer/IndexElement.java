package xmlindexer;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Objects;
import org.w3c.dom.*;

/**
 * @author Hugo Svallfors <keiter@lavabit.com>
 * A single element of an Index.
 * Corresponds to a single tagged node.
 * @see Index
 */
public class IndexElement {

    private Index parentIndex;
    private Element element;

    /**
     * Constructs a new IndexElement.
     * @param element The tagged node.
     * @param parentIndex The index the node belongs to.
     * @throws NullPointerException If either of the parameters are null.
     * @throws IllegalArgumentException If the tagged node is not tagged with
     * lifetime attributes.
     */
    public IndexElement(Index parentIndex, Element element) {
        if (parentIndex == null || element == null) {
            throw new NullPointerException();
        }
        if (!element.hasAttribute(StringConstants.VSTART)
                || !element.hasAttribute(StringConstants.VEND)) {
            throw new IllegalArgumentException(
                    "Missing lifetime information on element.");
        }
        this.parentIndex = parentIndex;
        this.element = element;
    }

    public IndexElement getParent() {
        return new IndexElement(parentIndex,
                (Element) element.getParentNode());
    }

    public Index getParentIndex() {
        return this.parentIndex;
    }

    public String getName() {
        return element.getTagName();
    }

    public String getValue() {
        return ElementUtils.getValue(element);
    }

    public void setValue(String value) {
        setNameValue(this.getName(), value);
    }

    public void setNameValue(String name, String value) {
        IndexElement parent = this.getParent();
        IndexElement newElem;
        if (this.isAttribute()) {
            newElem = parentIndex.createIndexAttribute(name, value);
        } else {
            newElem = parentIndex.createIndexElement(name);
            ElementUtils.setValue(newElem.element, value);
        }
        for (IndexElement attr : this.getAttributes()) {
            newElem.setAttribute(attr.getName(), attr.getValue());
        }
        for (IndexElement child : this.getChildElements()) {
            newElem.appendChild(child);
        }

        parent.insertBefore(newElem, this);
        this.delete();
        element = newElem.element;
    }

    /**
     * "Deletes" a tagged node, i.e sets it's VEND attrbiute to the current docVersion.
     * Also deletes all this IndexElement's children and attributes.
     */
    public void delete() {
        if (!this.isLive()) {
            return;
        }
        for (IndexElement attr : this.getAttributes()) {
            attr.delete();
        }
        for (IndexElement elem : this.getChildElements()) {
            elem.delete();
        }
        element.setAttribute(StringConstants.VEND,
                Long.toString(parentIndex.getDocumentVersion()));
    }

    public long created() {
        return Long.parseLong(element.getAttribute(StringConstants.VSTART));
    }

    public long deleted() {
        if (isLive()) {
            return -1;
        } else {
            return Long.parseLong(element.getAttribute(StringConstants.VEND));
        }
    }

    public boolean isLive() {
        return element.getAttribute(StringConstants.VEND).
                equals(StringConstants.NOW);
    }

    public boolean isAttribute() {
        return this.element.getAttribute(StringConstants.ISATTR).
                equals(StringConstants.YES);
    }

    public boolean hasAttribute(String name) {
        return this.getAttribute(name) != null;
    }

    public IndexElement getAttribute(String name) {
        for (IndexElement a : this.getAttributes()) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    public ArrayList<IndexElement> getAttributes() {
        ArrayList<IndexElement> results = new ArrayList<>();
        for (IndexElement child : this.elements(true)) {
            if (child.isAttribute()) {
                results.add(child);
            }
        }
        return results;
    }

    // Sets/creates an attribute on a index element.
    public IndexElement setAttribute(String name, String value) {
        IndexElement attr = this.getAttribute(name);
        if (attr == null) {
            attr = parentIndex.createIndexAttribute(name, value);
            element.appendChild(attr.element);
        } else {
            attr.setValue(value);
        }
        return attr;
    }

    public void deleteAttribute(String name) {
        IndexElement attr = this.getAttribute(name);
        if (attr == null) {
            throw new RuntimeException("Tried to delete non-present attribute.");
        }
        attr.delete();
    }

    public void appendChild(IndexElement child) {
        element.appendChild(child.element);
    }

    public void insertBefore(IndexElement element, IndexElement ref) {
        this.element.insertBefore(element.element, ref.element);
    }

    public void insertElementAt(IndexElement e, int index) {
        ArrayList<IndexElement> children = this.getChildElements();
        IndexElement ref = children.get(index);
        element.insertBefore(e.element, ref.element);
    }

    /**
     * Adds a new element to this IndexNode. The element is assumed not
     * to be tagged, and is normalized to the current docVersion.
     */
    public void normalizeElement(Element child) {
        IndexElement newChild =
                parentIndex.createIndexElement(child.getTagName());
        for (Attr a : ElementUtils.getAttributes(child)) {
            newChild.setAttribute(a.getName(), a.getValue());
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
        IndexElement elem = this.getEqualElement(e);
        if (elem == null) {
            throw new RuntimeException("Tried to delete non-present element.");
        }
        elem.delete();
    }

    public int childCount() {
        return this.getChildElements().size();
    }

    public ArrayList<IndexElement> getChildElements() {
        return this.getChildElements(true);
    }

    public ArrayList<IndexElement> getChildElements(boolean mustBeLive) {
        ArrayList<IndexElement> results = new ArrayList<>();
        for (IndexElement child : this.elements(mustBeLive)) {
            if (!child.isAttribute()) {
                results.add(child);
            }
        }
        return results;
    }

    public ArrayList<IndexElement> elements(boolean mustBeLive) {
        ArrayList<IndexElement> results = new ArrayList<>();
        for (Element c : ElementUtils.getChildElements(element)) {
            IndexElement child = new IndexElement(parentIndex, c);
            if (!mustBeLive || child.isLive()) {
                results.add(child);
            }
        }
        return results;
    }

    /**
     * Test of equality between a normal element and the tagged node
     * of this IndexElement. The comparsion only takes into account
     * live children/attributes of the tagged node.
     * @return True if the elements are equal.
     */
    public boolean isEqualElement(Element docElem) {
        return !this.isAttribute()
                && this.isLive()
                && element.getTagName().equals(docElem.getTagName())
                && hasEqualValue(docElem)
                && hasSameAttributes(docElem)
                && hasSameChildren(docElem);
    }

    private boolean hasEqualValue(Element docElem) {
        String thisValue = ElementUtils.getValue(element);
        String docValue = ElementUtils.getValue(docElem);
        return docValue.equals(thisValue);
    }

    private boolean hasSameChildren(Element docElem) {
        ArrayList<IndexElement> theseChildren =
                this.getChildElements();
        ArrayList<Element> thoseChildren =
                ElementUtils.getChildElements(docElem);
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
        for (IndexElement attr : this.getAttributes()) {
            String iVal = attr.getValue();
            String dVal = docElem.getAttribute(attr.getName());
            if (dVal == null || !iVal.equals(dVal)) {
                return false;
            }
        }
        for (Attr a1 : ElementUtils.getAttributes(docElem)) {
            IndexElement a2 = this.getAttribute(a1.getName());
            if (a2 == null || !a1.getValue().equals(a2.getValue())) {
                return false;
            }
        }
        return true;
    }

    public boolean hasEqualElement(Element target) {
        return this.getEqualElement(target) != null;
    }

    public IndexElement getEqualElement(Element target) {
        for (IndexElement e : this.getChildElements()) {
            if (e.isEqualElement(target)) {
                return e;
            }
        }
        return null;
    }

    public boolean equals(Object o) {
        if (!(o instanceof IndexElement)) {
            return false;
        }
        IndexElement that = (IndexElement) o;
        return this.parentIndex.equals(that.parentIndex)
                && this.element.equals(that.element);
    }

    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(this.parentIndex);
        hash = 79 * hash + Objects.hashCode(this.element);
        return hash;
    }

    public String toString() {
        String val = this.getName();
        Iterator<IndexElement> iter = this.getAttributes().iterator();
        if (iter.hasNext()) {
            IndexElement attr = iter.next();
            val += ": " + attr.getName() + "=" + attr.getValue();
        }
        while (iter.hasNext()) {
            IndexElement attr = iter.next();
            val += ", " + attr.getName() + "=" + attr.getValue();
        }
        return val;
    }

    /**
     * Reorders the position of this IndexElement in the parent's
     * child list, so that this index element is at the position
     * given, or at the end if the index is larger than the child
     * list length.
     * @param index The position to move this element to.
     */
    public void reorder(int index) {
        IndexElement parent = this.getParent();
        parent.eraseChild(this);
        if (parent.childCount() == index) {
            parent.appendChild(this);
        } else {
            parent.insertElementAt(this, index);
        }
    }

    /**
     * Permanently deletes e from this IndexElement's child list.
     */
    public void eraseChild(IndexElement e) {
        element.removeChild(e.element);
    }
}
