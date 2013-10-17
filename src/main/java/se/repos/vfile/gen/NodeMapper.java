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
        Map<Node, String> memoTable = new HashMap<Node, String>();
        NodeMapper.getNodeMap(nodeMap, memoTable, vfile,
                controlDocument.getDocumentElement());
        return nodeMap;
    }

    private static Map<String, TaggedNode> getNodeMap(Map<String, TaggedNode> nodeMap,
            Map<Node, String> memoTable, VFile vfile, Node controlNode) {
        String uniqueXPath = uniqueXPathOf(memoTable, controlNode);
        nodeMap.put(uniqueXPath, findIndexNode(vfile, controlNode, uniqueXPath));
        for (Node n : ElementUtils.getChildren(controlNode)) {
            getNodeMap(nodeMap, memoTable, vfile, n);
        }
        if (controlNode.getNodeType() == Node.ELEMENT_NODE) {
            Element e = (Element) controlNode;
            for (Attr a : ElementUtils.getAttributes(e)) {
                String attrXPath = uniqueXPathOf(memoTable, a);
                nodeMap.put(attrXPath, findIndexNode(vfile, a, attrXPath));
            }
        }
        return nodeMap;
    }

    /*
     * Calculates an unique XPath to the given node. If this XPath is then run
     * on the same document, the given node should be the only result.
     */
    private static String uniqueXPathOf(Map<Node, String> memoTable, Node n) {
        if (memoTable.containsKey(n)) {
            return memoTable.get(n);
        }
        short nodeType = n.getNodeType();
        if (nodeType == Node.DOCUMENT_NODE) {
            return "";
        }
        String localAxis;
        int localIndex = -1;
        Node parent;
        switch (nodeType) {
        case Node.ELEMENT_NODE:
            localAxis = n.getNodeName();
            localIndex = ElementUtils.getChildIndex(n, true);
            parent = n.getParentNode();
            break;
        case Node.TEXT_NODE:
            localAxis = "text()";
            localIndex = ElementUtils.getChildIndex(n, true);
            parent = n.getParentNode();
            break;
        case Node.COMMENT_NODE:
            localAxis = "comment()";
            localIndex = ElementUtils.getChildIndex(n, true);
            parent = n.getParentNode();
            break;
        case Node.PROCESSING_INSTRUCTION_NODE:
            localAxis = "processing-instruction()";
            localIndex = ElementUtils.getChildIndex(n, true);
            parent = n.getParentNode();
            break;
        case Node.ATTRIBUTE_NODE:
            localAxis = "@" + n.getNodeName();
            parent = ((Attr) n).getOwnerElement();
            break;
        default:
            throw new UnsupportedOperationException();
        }
        String xPath = uniqueXPathOf(memoTable, parent) + "/" + localAxis
                + (localIndex == -1 ? "" : "[" + (localIndex + 1) + "]");
        memoTable.put(n, xPath);
        return xPath;
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
            int textIndex = ElementUtils.getChildIndex(controlNode, true);
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
            throw new RuntimeException("Could not find changed node.");
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
