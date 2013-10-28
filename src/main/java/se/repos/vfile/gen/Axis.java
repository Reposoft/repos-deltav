package se.repos.vfile.gen;

public class Axis {
    public final int localIndex;
    public final Nodetype nodeType;
    public final String name;

    public Axis(String name, Nodetype nodeType, int localIndex) {
        if (name == null || nodeType == null) {
            throw new NullPointerException();
        }
        this.name = name;
        this.nodeType = nodeType;
        this.localIndex = localIndex;
    }

    @Override
    public String toString() {
        String localAxis = null;
        switch (this.nodeType) {
        case ATTRIBUTE:
            localAxis = this.name;
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
        if (this.nodeType == Nodetype.ATTRIBUTE) {
            return "@" + localAxis;
        }
        return localAxis + "[" + (this.localIndex + 1) + "]";
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
        if (this.name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!this.name.equals(other.name)) {
            return false;
        }
        if (this.nodeType != other.nodeType) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + this.localIndex;
        result = prime * result + ((this.name == null) ? 0 : this.name.hashCode());
        result = prime * result
                + ((this.nodeType == null) ? 0 : this.nodeType.hashCode());
        return result;
    }
}