package se.repos.vfile.gen;

// Enum for which update function to call for a changed element.
enum CHANGE {
    HAS_CHILD, NODE_NOT_FOUND, ELEM_ATTRS, TEXT_VALUE, ELEM_NAME, ELEM_CHILDREN_NUMBER, ELEM_CHILDREN_ORDER, ATTR_VALUE, IGNORED
}