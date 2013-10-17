package se.repos.vfile.gen;

import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class NodeMapper {
    public static Map<String, TaggedNode> getNodeMap(VFile vfile, Document controlDocument) {
        Map<String, TaggedNode> nodeMap = new HashMap<String, TaggedNode>();
        for (Node n : ElementUtils.getChildren(controlDocument)) {
            String uniqueXPath = uniqueXPathOf(n);
            nodeMap.put(uniqueXPath, findIndexNode(vfile, n, uniqueXPath));
        }
        return nodeMap;
    }

    private static String uniqueXPathOf(Node n) {
        Node parent = n.getParentNode();
        if (parent == null) {
            return "/"; // Node is root.
        }
        String localAxis;
        int localIndex = 0;
        switch (n.getNodeType()) {
        case Node.ELEMENT_NODE:
            localAxis = n.getNodeName();
            localIndex = ElementUtils.getChildIndex(n, Node.ELEMENT_NODE);
            break;
        case Node.TEXT_NODE:
            localAxis = "text()";
            localIndex = ElementUtils.getChildIndex(n, Node.TEXT_NODE);
            break;
        case Node.COMMENT_NODE:
            localAxis = "comment()";
            localIndex = ElementUtils.getChildIndex(n, Node.COMMENT_NODE);

            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            localAxis = "processing-instruction()";
            localIndex = ElementUtils.getChildIndex(n, Node.PROCESSING_INSTRUCTION_NODE);
            break;
        case Node.ATTRIBUTE_NODE:
            localAxis = "@" + n.getNodeName();
            break;
        default:
            throw new UnsupportedOperationException();
        }
        return uniqueXPathOf(parent) + "/" + localAxis
                + (localIndex == 0 ? "" : "[" + localIndex + "]");
    }

    /**
     * Finds the TaggedNode that corresponds to controlNode.
     * 
     * @param controlNode
     *            The node we are looking for in the index.
     * @param uniqueXPath
     *            The unique XPath selector of controlNode.
     * @return The TaggedNode to update.
     */
    private static TaggedNode findIndexNode(VFile vfile, Node controlNode,
            String uniqueXPath) {
        TaggedNode indexParent;
        TaggedNode returnNode;
        switch (controlNode.getNodeType()) {
        case Node.ATTRIBUTE_NODE:
            Attr attr = (Attr) controlNode;
            indexParent = NodeMapper.findTaggedNode(vfile,
                    NodeMapper.getXPathParent(uniqueXPath));
            returnNode = indexParent.getAttribute(attr.getName());
            break;
        case Node.ELEMENT_NODE:
            returnNode = NodeMapper.findTaggedNode(vfile, uniqueXPath);
            break;
        case Node.TEXT_NODE:
            indexParent = NodeMapper.findTaggedNode(vfile,
                    NodeMapper.getXPathParent(uniqueXPath));
            int textIndex = ElementUtils.getChildIndex(controlNode, Node.TEXT_NODE);
            returnNode = indexParent.getTextNode(textIndex);
            break;
        default:
            throw new UnsupportedOperationException();
        }
        if (returnNode == null) {
            throw new RuntimeException("Could not find changed node.");
        }
        if (!returnNode.isLive()) {
            throw new RuntimeException("Found node is not live.");
        }
        return returnNode;
    }

    public static String getXPathParent(String xPath) {
        return xPath.substring(0, xPath.lastIndexOf("/"));
    }

    private static TaggedNode findTaggedNode(VFile vfile, String uniqueXPath) {
        Element result = (Element) NodeMapper.xPathQuery(uniqueXPath, vfile.toDocument());
        if (result == null) {
            return null;
        }
        return new TaggedNode(vfile, result);
    }

    private static Node xPathQuery(String xPath, Document doc) {
        XPath xPathEvaluator = XPathFactory.newInstance().newXPath();
        try {
            return (Node) xPathEvaluator.evaluate(xPath, doc, XPathConstants.NODE);
        } catch (XPathExpressionException ex) {
            throw new RuntimeException(ex.getMessage());
        }
    }
}
