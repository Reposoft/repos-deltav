package se.repos.vfile.gen;

import java.util.ArrayList;

import org.slf4j.helpers.MessageFormatter;
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
        return ElementUtils.getLocalIndex(needle, false, false, false);
    }

    /**
     * Equivalent to getLocalIndex(needle, specificType, false).
     */
    public static int getLocalIndex(Node needle, boolean specificType) {
        return ElementUtils.getLocalIndex(needle, specificType, false, false);
    }

    /**
     * Given a node, finds the index of where among it's siblings it can be
     * found.
     * 
     * This method can now be used in a VFile.
     * 
     * @param needle
     *            The node to find the index of.
     * @param specificType
     *            Whether to count only nodes of the same type as siblings.
     * @param mustFind
     *            If no result is acceptable.
     * @param isVfile will calculate document index when given a VFile (disregard attr and historic elements)
     * @return The index of needle among it's siblings, or -1 if not found and
     *         mustFind is set to false.
     * @throws RuntimeException
     *             If the needle is not found and mustFind is true.
     */
    public static int getLocalIndex(Node needle, boolean specificType, boolean mustFind, boolean isVfile) {
        Node parent = needle.getParentNode();
        if (parent == null) {
        	// Likely means that we are a Document node. Perhaps an element can also be without parent while moving.
        	throw new IllegalArgumentException("Node does not have a parent: " + needle.getNodeType());
        	//return 0;
        }
        int i = 0;
        ArrayList<Node> children = ElementUtils.getChildren(parent);
        for (Node child : children) {
            if (child.isSameNode(needle)) {
                return i;
            }
            
            if (isVfile && child.hasAttributes()) { 
            	
                if (isVFileAttribute(child)) {
                	// Don't take VFile attributes into account.
                	continue;
                }
            	
            	if (!isVFileElementLive(child)) {
            		// Element is assumed to be VFile (ifVfile=true) and it is no longer Live.
            		continue;
            	}
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
                // TODO: Investigate how to manage non-vital whitespace. This is NOT acceptable for MIXED.
            	// Is this really safe as basis for getLocalIndex ?
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
    
    public static boolean isVFileAttribute(Node n) {
    	
    	if (n.hasAttributes()) { 
        	Element e = (Element) n;
        	String tagName = e.getTagName();
            if (tagName.equals(StringConstants.ATTR)) {
            	return true;
            }
    	}
    	return false;
    }
    
    
    public static boolean isVFileElementLive(Node n) {
    	
    	if (n.hasAttributes()) { 
        	Element e = (Element) n;
        	String end = e.getAttribute(StringConstants.END);
        	if (end != null && !end.isEmpty() && !end.equals(StringConstants.NOW)) {
        		// Element is a VFile and it is no longer Live.
        		return false;
        	}
        	return true;
    	}
    	
    	throw new IllegalArgumentException("The node is not a VFile element: " + n);
    }
    
    
    public static int findVfileIndex(Element parent, Integer index) {
    	
    	Integer liveElems = 0;
    	NodeList nl = parent.getChildNodes();
    	if (nl == null || nl.getLength() == 0) {
            return 0;
        }
    	
    	for (Integer i = 0; i < nl.getLength(); i++) {
    		Node n = nl.item(i);
    		if (!isVFileAttribute(n) && liveElems == index) {
    			return i;
    		}
    		
    		if (!isVFileAttribute(n) && isVFileElementLive(n)) {
    			liveElems++;
    		}
    		
    		/*
    		if (isVFileAttribute(n) && i == nl.getLength()-1) {
    			// Special case when all-attributes.
    			return i;
    		}
    		*/
    	}
    	if (liveElems == index) {
    		return nl.getLength();
    	}
    	
    	String msg = MessageFormatter.format("Index {} not found in {} children of " + parent.getNodeName(), index, new Integer(nl.getLength())).getMessage();
    	throw new IllegalArgumentException(msg);
    	
    }
    
    
    public static int getPreviousSiblingCount(Element element) {
    	
    	int i = 0;
    	Node e = element;
    	while ((e = e.getPreviousSibling()) != null) {
    		i++;
    	}
    	return i;
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
