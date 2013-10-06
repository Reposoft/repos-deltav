package se.repos.vfile.store;

import java.io.File;
import java.io.IOException;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import se.repos.vfile.VFileDocumentBuilder;
import se.simonsoft.cms.item.CmsItemId;

public class VFileStoreDisk extends VFileStore {

    private File vFileFolder;
    private VFileDocumentBuilder db;
    private Transformer trans;

    public VFileStoreDisk(String localFilePath) {
        this.vFileFolder = new File(localFilePath);
        if (!this.vFileFolder.exists()) {
            this.vFileFolder.mkdirs();
        }
        this.db = new VFileDocumentBuilder();
        try {
            this.trans = TransformerFactory.newInstance().newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e.getMessage());
        } catch (TransformerFactoryConfigurationError e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public void put(CmsItemId resource, Document vfile) {
        if (resource.getPegRev() != null) {
            throw new IllegalArgumentException("Resource should not have a peg revision.");
        }
        try {
            String filePath = resource.getRelPath().toString();
            File indexFile = new File(this.vFileFolder, filePath);
            if (!indexFile.exists()) {
                indexFile.createNewFile();
            }
            Source source = new DOMSource(vfile);
            Result result = new StreamResult(indexFile);
            this.trans.transform(source, result);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } catch (TransformerException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean has(CmsItemId resource) {
        if (resource.getPegRev() != null) {
            throw new IllegalArgumentException("Resource should not have a peg revision.");
        }
        String filePath = resource.getRelPath().toString();
        File indexFile = new File(this.vFileFolder, filePath);
        return indexFile.exists();
    }

    @Override
    public Document get(CmsItemId resource) {
        if (resource.getPegRev() != null) {
            throw new IllegalArgumentException("Resource should not have a peg revision.");
        }
        if (!this.has(resource)) {
            return null;
        }
        String filePath = resource.getRelPath().toString();
        File indexFile = new File(this.vFileFolder, filePath);
        try {
            return this.db.parse(indexFile);
        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        } catch (SAXException e) {
            throw new RuntimeException(e.getMessage());
        }
    }

}
