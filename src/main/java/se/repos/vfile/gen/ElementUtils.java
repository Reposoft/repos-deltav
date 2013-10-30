package se.repos.vfile.gen;

import java.util.ArrayList;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * A utility class for dealing with DOM Element nodes.
 */
public class ElementUtils {

    /**
     * Equivalent to getLocalIndex(needle, false, false).
     */
    public static int getLocalIndex(Node needle) {
        return ElementUtils.getLocalIndex(needle, false, false);
    }

    /**
     * Equivalent to getLocalIndex(needle, specificType, false).
     */
    public static int getLocalIndex(Node needle, boolean specificType) {
        return ElementUtils.getLocalIndex(needle, specificType, false);
    }

    /**
     * Given a node, finds the index of where among it's siblings it can be
     * found.
     * 
     * @param needle
     *            The node to find the index of.
     * @param specificType
     *            Whether to count only nodes of the same type as siblings.
     * @param mustFind
     *            If no result is acceptable.
     * @return The index of needle among it's siblings, or -1 if not found and
     *         mustFind is set to false.
     * @throws RuntimeException
     *             If the needle is not found and mustFind is true.
     */
    public static int getLocalIndex(Node needle, boolean specificType, boolean mustFind) {
        Node parent = needle.getParentNode();
        int i = 0;
        ArrayList<Node> children = ElementUtils.getChildren(parent);
        for (Node child : children) {
            if (child.isSameNode(needle)) {
                return i;
            }
            if (!specificType) {
                i++;
            } else if (child.getNodeType() == needle.getNodeType()) {
                if (child.getNodeType() != Node.ELEMENT_NODE
                        || child.getNodeName().equals(needle.getNodeName())) {
                    i++;
                }
            }
        }
        if (mustFind) {
            throw new RuntimeException("Could not find node.");
        }
        return -1;
    }

    public static ArrayList<Node> getChildren(Node parent) {
        ArrayList<Node> results = new ArrayList<Node>();
        NodeList children = parent.getChildNodes();
        if (children == null) {
            return results;
        }
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            short nodeType = c.getNodeType();
            switch (nodeType) {
            case Node.DOCUMENT_TYPE_NODE:
                break; // not supported yet
            case Node.TEXT_NODE:
                if (!((Text) c).getData().trim().isEmpty()) { // no empty text
                                                              // nodes
                    results.add(c);
                }
                break;
            default:
                results.add(c);
                break;
            }
        }
        return results;
    }

    /**
     * Retrieves the attributes elements of a node. Does not include name space
     * declarations.
     * 
     * @param element
     *            The parent node.
     * @return The list of attributes of the element.
     */
    public static ArrayList<Attr> getAttributes(Element element) {
        ArrayList<Attr> results = new ArrayList<Attr>();
        NamedNodeMap attrs = element.getAttributes();
        if (attrs == null) {
            return results;
        }
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr a = (Attr) attrs.item(i);
            if (!ElementUtils.isNameSpace(a)) {
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
    public static ArrayList<Attr> getNamespaces(Element element) {
        ArrayList<Attr> results = new ArrayList<Attr>();
        NamedNodeMap attrs = element.getAttributes();
        if (attrs == null) {
            return results;
        }
        for (int i = 0; i < attrs.getLength(); i++) {
            Attr a = (Attr) attrs.item(i);
            if (ElementUtils.isNameSpace(a)) {
                results.add(a);
            }
        }
        return results;
    }

    public static boolean isNameSpace(Attr a) {
        return a.getName().startsWith("xmlns:");
    }

    public static Nodetype getNodeType(Node n) {
        switch (n.getNodeType()) {
        case Node.ATTRIBUTE_NODE:
            return Nodetype.ATTRIBUTE;
        case Node.TEXT_NODE:
            return Nodetype.TEXT;
        case Node.COMMENT_NODE:
            return Nodetype.COMMENT;
        case Node.PROCESSING_INSTRUCTION_NODE:
            return Nodetype.PROCESSING_INSTRUCTION;
        case Node.DOCUMENT_NODE:
            return Nodetype.DOCUMENT;
        case Node.ELEMENT_NODE:
            return Nodetype.ELEMENT;
        default:
            throw new UnsupportedOperationException();
        }
    }
}
