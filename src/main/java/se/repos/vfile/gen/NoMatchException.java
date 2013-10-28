package se.repos.vfile.gen;

public class NoMatchException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private SimpleXPath docPath;
    private SimpleXPath vFilePath;

    public NoMatchException(SimpleXPath docPath, SimpleXPath vFilePath) {
        this.docPath = docPath;
        this.vFilePath = vFilePath;
    }

    @Override
    public String getMessage() {
        return "No match found between document node " + this.docPath
                + " and v-file node " + this.vFilePath;
    }
}
