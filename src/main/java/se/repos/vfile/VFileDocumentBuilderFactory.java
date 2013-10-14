package se.repos.vfile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

// Class used to get the same DOM configuration on all DocumentBuilders in the project.
public class VFileDocumentBuilderFactory extends DocumentBuilderFactory {
    private DocumentBuilderFactory dbf;

    public VFileDocumentBuilderFactory() {
        this.dbf = DocumentBuilderFactory.newInstance();
        this.dbf.setCoalescing(true);
        this.dbf.setIgnoringComments(false);
        this.dbf.setNamespaceAware(false);
        this.dbf.setValidating(false);
        try {
            this.dbf.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                    false);
            this.dbf.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public Object getAttribute(String name) {
        return this.dbf.getAttribute(name);
    }

    @Override
    public boolean getFeature(String name) {
        try {
            return this.dbf.getFeature(name);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public DocumentBuilder newDocumentBuilder() {
        try {
            return this.dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void setAttribute(String name, Object value) {
        this.dbf.setAttribute(name, value);
    }

    @Override
    public void setFeature(String name, boolean value) {
        try {
            this.dbf.setFeature(name, value);
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }
}
