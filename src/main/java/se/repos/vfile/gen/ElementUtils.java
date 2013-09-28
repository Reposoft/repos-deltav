package se.repos.vfile.gen;

import java.util.ArrayList;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * @author Hugo Svallfors <keiter@lavabit.com>
 * A utility class for dealing with DOM Element nodes.
 */
public class ElementUtils {

    /**
     * Method that checks whether elem has an element equal to attr.
     * @param elem The element to search.
     * @param attr The attribute to search for in elem.
     * @return Whether elem contains attr.
     */
    public static boolean hasEqualAttribute(Element elem, Attr attr) {
        return elem.hasAttribute(attr.getName())
                && elem.getAttribute(attr.getName()).equals(attr.getValue());
    }

    /**
     * Retrieves the child elements of a node.
     * @param element The parent node.
     * @return The list of child elements of element.
     */
    public static ArrayList<Element> getChildElements(Element element) {
        ArrayList<Element> results = new ArrayList<>();
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

    /**
     * Retrieves the attributes elements of a node.
     * @param element The parent node.
     * @return The list of attributes of element.
     */
    public static ArrayList<Attr> getAttributes(Element element) {
        ArrayList<Attr> results = new ArrayList<>();
        NamedNodeMap attrs = element.getAttributes();
        if (attrs == null) {
            return results;
        }
        for (int i = 0; i < attrs.getLength(); i++) {
            results.add((Attr) attrs.item(i));
        }
        return results;
    }

    /**
     * Gets the text value of an element.
     * @param elem The lement whose value to retrieve.
     * @return The value of the node, or the empty string if none exists.
     */
    public static String getValue(Element elem) {
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (isTextNode(c)) {
                String value = ((Text) c).getWholeText();
                if (value.trim().isEmpty()) {
                    return "";
                } else {
                    return value;
                }
            }
        }
        return "";
    }

    /**
     * Sets the text value of elem.
     * @param elem The element whose value to set.
     * @param value The value which to set it to.
     */
    public static void setValue(Element elem, String value) {
        // First try to replace existing text field.
        NodeList children = elem.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (isTextNode(c)) {
                ((Text) c).replaceWholeText(value);
                return;
            }
        }
        // Otherwise, create new one.
        elem.appendChild(elem.getOwnerDocument().createTextNode(value));
    }

    private static boolean isTextNode(Node n) {
        short nodeType = n.getNodeType();
        return nodeType == Node.TEXT_NODE || nodeType == Node.CDATA_SECTION_NODE;
    }

    /**
     * Retrieves at which index position of it's parent node you can find
     * child.
     * @throws RuntimeException If the parent node of child does not contain an
     * equal node.
     */
    public static int getChildIndex(Element child) {
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
}
