package se.repos.vfile.gen;

import org.custommonkey.xmlunit.ElementQualifier;
import org.w3c.dom.Element;

/**
 * The element qualifier for the XML indexer. Considers nodes with the same name
 * and relative position comparable.
 * 
 * @see ElementQualifier
 */
class NameAndPositionElementQualifier implements ElementQualifier {

    public NameAndPositionElementQualifier() {
    }

    /**
     * Method that return whether two elements of the new and old
     * document in the diff are considered comparable.
     * Considers two elements comparable if they have the same name and the
     * same relative position to other elements of the same name.
     * I.e if we have two documents that look like this:
     * <foo>                <foo>                  
     *      <bar .../>          <baz ... />
     *      <bar .../>          <bar ... />
     * </foo>                   <bar ... />
     *                      </foo>
     * It would consider comparable bar[1] to bar[1], bar[2] to bar[2]
     * and would consider baz[1] unmatched.
     */
    @Override
    public boolean qualifyForComparison(Element elmnt1, Element elmnt2) {
        if (!elmnt1.getTagName().equals(elmnt2.getTagName())) {
            return false;
        }
        // I am not sure the requirement "equal index is good".
        // It forces an inserted section to be compared with a section that has a match further down.
        // Problem is likely that XMUnit will only compare the first pair where this method returns true.
        // Do we get notified of the different order if we relax this requirement?
        // Alternative could be to listen to MatchTracker and make note of the reorders.
        int elmnt1Pos = ElementUtils.getLocalIndex(elmnt1, true, false, false);
        int elmnt2Pos = ElementUtils.getLocalIndex(elmnt2, true, false, false);
        return elmnt1Pos != -1 && elmnt2Pos != -1 && elmnt1Pos == elmnt2Pos;
    }
}
