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

    /**
     * Retrieves at which index position of it's parent node you can find child.
     * 
     * @throws RuntimeException
     *             If the parent node of child does not contain an equal node.
     */
    public static int getChildIndex(Node child) {
        Element parent = (Element) child.getParentNode();
        int i = 0;
        for (Node n : ElementUtils.getChildren(parent)) {
            if (n.equals(child)) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("Element not found.");
    }

    public static ArrayList<Node> getChildren(Node parent) {
        // TODO This method should return comments and PIs.
        ArrayList<Node> results = new ArrayList<Node>();
        NodeList children = parent.getChildNodes();
        if (children == null) {
            return results;
        }
        for (int i = 0; i < children.getLength(); i++) {
            Node c = children.item(i);
            if (c.getNodeType() != Node.TEXT_NODE
                    || !((Text) c).getData().trim().isEmpty()) {
                results.add(c);
            }
        }
        return results;
    }

    /**
     * Retrieves at which index position of it's parent node you can find text.
     * 
     * @throws RuntimeException
     *             If the parent node of child does not contain an equal node.
     */
    public static int getTextIndex(Text text) {
        Element parent = (Element) text.getParentNode();
        int i = 0;
        for (Text t : getText(parent)) {
            if (t.equals(text)) {
                return i;
            }
            i++;
        }
        throw new RuntimeException("Text not found.");
    }

    private static ArrayList<Text> getText(Element element) {
        ArrayList<Text> results = new ArrayList<Text>();
        NodeList children = element.getChildNodes();
        if (children == null) {
            return results;
        }
        for (int i = 0; i < children.getLength(); i++) {
            if (children.item(i).getNodeType() == Node.TEXT_NODE) {
                Text t = (Text) children.item(i);
                if (!t.getData().trim().isEmpty()) {
                    results.add(t);
                }
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

    public static boolean isNameSpace(Attr a) {
        return a.getName().startsWith("xmlns:");
    }
}
