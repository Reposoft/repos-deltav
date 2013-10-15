package se.repos.vfile.gen;

import java.util.ArrayList;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

/**
 * @author Hugo Svallfors <keiter@lavabit.com> A utility class for dealing with
 *         DOM Element nodes.
 */
public class ElementUtils {

    public static ArrayList<Node> getNodes(Node parent) {
        ArrayList<Node> results = new ArrayList<Node>();
        NodeList children = parent.getChildNodes();
        if (children == null) {
            return results;
        }
        for (int i = 0; i < children.getLength(); i++) {
            results.add(children.item(i));
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
}
