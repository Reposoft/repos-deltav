package se.repos.vfile.gen;

import java.util.Iterator;
import java.util.LinkedList;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

// TODO Javadoc this class.
public class SimpleXPath implements Iterable<Axis> {

    private LinkedList<Axis> axi;

    public SimpleXPath(String xPath) {
        if (xPath == null) {
            throw new NullPointerException();
        }
        this.axi = new LinkedList<Axis>();
        String[] axisStrings = xPath.substring(1).split("/"); // the substring
                                                              // drop the
                                                              // leading '/'
        for (String axisString : axisStrings) {
            int localIndex = -1;
            String localAxis;
            Nodetype nodeType;
            if (axisString.startsWith("@")) {
                localAxis = axisString.substring(1); // drops the '@'.
                nodeType = Nodetype.ATTRIBUTE;
            } else {
                String[] axisParts = axisString.split("\\[|\\]");
                if (axisParts.length != 2) {
                    throw new IllegalArgumentException("String is not a simple XPath.");
                }
                localIndex = Integer.parseInt(axisParts[1]) - 1; // uses 0-based
                                                                 // index, hence
                                                                 // the - 1.
                localAxis = axisParts[0];
                if (localAxis.equals("comment()")) {
                    nodeType = Nodetype.COMMENT;
                } else if (localAxis.equals("processing-instruction()")) {
                    nodeType = Nodetype.PROCESSING_INSTRUCTION;
                } else if (localAxis.equals("text()")) {
                    nodeType = Nodetype.TEXT;
                } else {
                    nodeType = Nodetype.ELEMENT;
                }
            }
            this.addLast(new Axis(localAxis, nodeType, localIndex));
        }
    }

    public SimpleXPath(Node node) {
        if (node == null) {
            throw new NullPointerException();
        }
        this.axi = new LinkedList<Axis>();
        Nodetype nodeType = ElementUtils.getNodeType(node);
        if (nodeType == Nodetype.DOCUMENT) {
            return;
        }
        String localAxis;
        int localIndex = -1;
        Node current = node;
        Node parent;

        while (nodeType != Nodetype.DOCUMENT) {
            switch (nodeType) {
            case ELEMENT:
                localAxis = current.getNodeName();
                localIndex = ElementUtils.getChildIndex(current, true);
                parent = current.getParentNode();
                break;
            case TEXT:
                localAxis = "text()";
                localIndex = ElementUtils.getChildIndex(current, true);
                parent = current.getParentNode();
                break;
            case COMMENT:
                localAxis = "comment()";
                localIndex = ElementUtils.getChildIndex(current, true);
                parent = current.getParentNode();
                break;
            case PROCESSING_INSTRUCTION:
                localAxis = "processing-instruction()";
                localIndex = ElementUtils.getChildIndex(current, true);
                parent = current.getParentNode();
                break;
            case ATTRIBUTE:
                localAxis = current.getNodeName();
                localIndex = -1;
                parent = ((Attr) current).getOwnerElement();
                break;
            default:
                throw new UnsupportedOperationException();
            }
            this.addFirst(new Axis(localAxis, nodeType, localIndex));
            current = parent;
            nodeType = ElementUtils.getNodeType(current);
        }
    }

    public void addFirst(Axis a) {
        this.axi.addFirst(a);
    }

    public void addLast(Axis a) {
        this.axi.addLast(a);
    }

    public Axis getFirstAxis() {
        return this.axi.getFirst();
    }

    public Axis getLastAxis() {
        return this.axi.getLast();
    }

    public Axis removeFirstAxis() {
        return this.axi.removeFirst();
    }

    public Axis removeLastAxis() {
        return this.axi.removeLast();
    }

    public TaggedNode eval(TaggedNode context) {
        TaggedNode currentContext = context;
        for (Axis axis : this) {
            if (currentContext == null) {
                throw new RuntimeException("Node not found");
            }
            if (!currentContext.isLive()) {
                throw new RuntimeException("Node is no longer live.");
            }
            switch (axis.nodeType) {
            case ATTRIBUTE:
                currentContext = currentContext.getAttribute(axis.name);
                break;
            case COMMENT:
                currentContext = currentContext.getNthNodeOfType(axis.localIndex,
                        Nodetype.COMMENT);
                break;
            case DOCUMENT:
                break;
            case PROCESSING_INSTRUCTION:
                currentContext = currentContext.getNthNodeOfType(axis.localIndex,
                        Nodetype.PROCESSING_INSTRUCTION);
                break;
            case TEXT:
                currentContext = currentContext.getNthNodeOfType(axis.localIndex,
                        Nodetype.TEXT);
                break;
            case ELEMENT:
                currentContext = currentContext.getElementsByTagName(axis.name).get(
                        axis.localIndex);
                break;
            }
        }

        if (currentContext == null) {
            throw new RuntimeException("Node not found");
        }
        if (!currentContext.isLive()) {
            throw new RuntimeException("Node is no longer live.");
        }
        return currentContext;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((this.axi == null) ? 0 : this.axi.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (this.getClass() != obj.getClass()) {
            return false;
        }
        SimpleXPath other = (SimpleXPath) obj;
        if (this.axi == null) {
            if (other.axi != null) {
                return false;
            }
        } else if (!this.axi.equals(other.axi)) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Axis a : this.axi) {
            sb.append("/");
            sb.append(a);
        }
        return sb.toString();
    }

    @Override
    public Iterator<Axis> iterator() {
        return this.axi.iterator();
    }
}
