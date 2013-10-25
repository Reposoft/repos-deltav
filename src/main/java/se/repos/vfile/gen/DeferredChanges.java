package se.repos.vfile.gen;

import java.util.LinkedHashSet;
import java.util.Set;

import org.w3c.dom.Node;

/**
 * A class that saves changes to be performed on a single node. Includes links
 * to the old and new versions of the node, and a set of changes to perform.
 * 
 * @see CHANGE
 */
class DeferredChanges {

    public final Node controlNode;
    public final Node testNode;
    public final SimpleXPath testLocation;
    public final Set<CHANGE> changes;

    public DeferredChanges(Node controlNode, Node testNode, SimpleXPath testLocation) {
        this.controlNode = controlNode;
        this.testNode = testNode;
        this.testLocation = testLocation;
        this.changes = new LinkedHashSet<CHANGE>();
    }

    public void addChange(CHANGE change) {
        this.changes.add(change);
    }
}