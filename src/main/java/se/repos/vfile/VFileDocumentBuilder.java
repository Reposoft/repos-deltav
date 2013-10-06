package se.repos.vfile;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.DOMImplementation;
import org.w3c.dom.Document;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class VFileDocumentBuilder extends DocumentBuilder {
    private DocumentBuilder db;

    public VFileDocumentBuilder() {
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setCoalescing(true);
            dbf.setIgnoringComments(true);
            dbf.setIgnoringElementContentWhitespace(true);
            dbf.setNamespaceAware(true);
            dbf.setValidating(false);
            dbf.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
                    false);
            dbf.setFeature(
                    "http://apache.org/xml/features/nonvalidating/load-external-dtd",
                    false);
            this.db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public DOMImplementation getDOMImplementation() {
        return this.db.getDOMImplementation();
    }

    @Override
    public boolean isNamespaceAware() {
        return this.db.isNamespaceAware();
    }

    @Override
    public boolean isValidating() {
        return this.db.isValidating();
    }

    @Override
    public Document newDocument() {
        return this.db.newDocument();
    }

    @Override
    public Document parse(InputSource is) throws SAXException, IOException {
        return this.db.parse(is);
    }

    @Override
    public void setEntityResolver(EntityResolver er) {
        this.db.setEntityResolver(er);
    }

    @Override
    public void setErrorHandler(ErrorHandler eh) {
        this.db.setErrorHandler(eh);
    }
}
