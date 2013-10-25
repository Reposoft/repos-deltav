package se.repos.vfile.gen;

import java.util.LinkedList;

import org.w3c.dom.Attr;
import org.w3c.dom.Node;

// TODO Javadoc this class.
public class SimpleXPath {

    private LinkedList<Axis> axi;

    public SimpleXPath(String xPath) {
        this.axi = new LinkedList<Axis>();
        String[] axisStrings = xPath.split("/");
        for (String axisString : axisStrings) {
            String[] axisParts = axisString.split("\\[|\\]");
            if (axisParts.length != 2) {
                throw new IllegalArgumentException("String is not a simple XPath.");
            }
            int localIndex = Integer.parseInt(axisParts[1]);
            String localAxis = axisParts[0];
            Nodetype nodeType;
            if (localAxis.equals("comment()")) {
                nodeType = Nodetype.COMMENT;
            } else if (localAxis.equals("processing-instruction()")) {
                nodeType = Nodetype.PROCESSING_INSTRUCTION;
            } else if (localAxis.equals("text()")) {
                nodeType = Nodetype.TEXT;
            } else if (localAxis.startsWith("@")) {
                localAxis = localAxis.substring(1); // drop the '@'
                nodeType = Nodetype.ATTRIBUTE;
            } else {
                nodeType = Nodetype.ELEMENT;
            }
            this.axi.addLast(new Axis(localAxis, nodeType, localIndex));
        }
    }

    public SimpleXPath(Node node) {
        this.axi = new LinkedList<Axis>();
        Nodetype nodeType = ElementUtils.getNodeType(node);
        if (nodeType == Nodetype.DOCUMENT) {
            return;
        }
        String localAxis;
        int localIndex = -1;
        Node current = node;
        Node parent;
        while (current != null) {
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
                parent = ((Attr) current).getOwnerElement();
                break;
            default:
                throw new UnsupportedOperationException();
            }
            this.axi.addFirst(new Axis(localAxis, nodeType, localIndex));
            current = parent;
        }
    }

    public void removeLastAxis() {
        this.axi.removeLast();
    }

    public TaggedNode eval(TaggedNode context) {
        TaggedNode currentContext = context;
        for (Axis axis : this.axi) {
            if (currentContext == null) {
                throw new RuntimeException("Node not found");
            }
            if (!currentContext.isLive()) {
                throw new RuntimeException("Node is no longer live.");
            }
            if (axis.nodeType != currentContext.getNodetype()) {
                throw new RuntimeException("Node of incorrect type.");
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

    private class Axis {
        public int localIndex;
        public Nodetype nodeType;
        public String name;

        public Axis(String name, Nodetype nodeType, int localIndex) {
            if (name == null || nodeType == null) {
                throw new NullPointerException();
            }
            this.name = name;
            this.nodeType = nodeType;
            this.localIndex = localIndex;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + this.localIndex;
            result = prime * result
                    + ((this.nodeType == null) ? 0 : this.nodeType.hashCode());
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
            Axis other = (Axis) obj;
            if (this.localIndex != other.localIndex) {
                return false;
            }
            if (this.nodeType != other.nodeType) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            String localAxis = null;
            switch (this.nodeType) {
            case ATTRIBUTE:
                localAxis = "@" + this.name;
                break;
            case COMMENT:
                localAxis = "comment()";
                break;
            case DOCUMENT:
                localAxis = "";
                break;
            case ELEMENT:
                localAxis = this.name;
                break;
            case PROCESSING_INSTRUCTION:
                localAxis = "processing-instruction()";
                break;
            case TEXT:
                localAxis = "text()";
                break;
            }
            return localAxis + "[" + this.localIndex + "]";
        }
    }
}