package se.repos.vfile.gen;

/**
 * Exception thrown when comparison between a VFile and document shows that
 * they're unequal. Includes XPaths to the differing element found.
 */
public class NoMatchException extends Exception {
    private static final long serialVersionUID = 1L;
    private SimpleXPath docPath;
    private SimpleXPath vFilePath;

    public NoMatchException(SimpleXPath docPath, SimpleXPath vFilePath) {
        this.docPath = docPath;
        this.vFilePath = vFilePath;
    }

    @Override
    public String getMessage() {
        return "The given document and v-file nodes do not match:\n" + "document: "
                + this.docPath + "\n" + "v-file: " + this.vFilePath;
    }
}
