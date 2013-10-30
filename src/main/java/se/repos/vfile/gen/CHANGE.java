package se.repos.vfile.gen;

/** Enum denoting which change has occurred to a tagged node. */
enum CHANGE {
    NODE_NOT_FOUND, ELEM_ATTRS, TEXT_VALUE, ELEM_CHILDREN_NUMBER, ELEM_CHILDREN_ORDER,
    ATTR_VALUE, COMMENT_VALUE, PI_DATA
}