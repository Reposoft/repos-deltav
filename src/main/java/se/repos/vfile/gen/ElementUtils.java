package se.repos.vfile.gen;

import java.util.ArrayList;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * @author Hugo Svallfors <keiter@lavabit.com> A utility class for dealing with
 *         DOM Element nodes.
 */
public class ElementUtils {

    public static int getChildIndex(Node child) {
        return ElementUtils.getChildIndex(child, false, false);
    }

    public static int getChildIndex(Node child, boolean specificType) {
        return ElementUtils.getChildIndex(child, specificType, false);
    }

    public static int getChildIndex(Node child, boolean specificType, boolean mustFind) {
        Node parent = child.getParentNode();
        int i = 0;
        for (Node n : ElementUtils.getChildren(parent)) {
            if (n.isSameNode(child)) {
                return i;
            }
            if (!specificType
                    || (n.getNodeType() == child.getNodeType() && n.getNodeName().equals(
                            child.getNodeName()))) {
                i++;
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
     * Retrieves the attributes elements of a node.
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
}
