package se.repos.vfile.store;

import java.io.File;
import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import se.simonsoft.cms.item.CmsItemId;

public class VFileStoreDisk extends VFileStore {

	private File vFileFolder;
	private DocumentBuilder db;
	private Transformer trans;

	public VFileStoreDisk(String localFilePath) {
		vFileFolder = new File(localFilePath);
		if (!vFileFolder.exists()) {
			vFileFolder.mkdirs();
		}
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setNamespaceAware(true);
		try {
			db = dbf.newDocumentBuilder();
			TransformerFactory tranFac = TransformerFactory.newInstance();
			trans = tranFac.newTransformer();
		} catch (TransformerConfigurationException e) {
			throw new RuntimeException(e.getMessage());
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public void put(CmsItemId resource, Document vfile) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		try {
			String filePath = resource.getRelPath().toString();
			File indexFile = new File(vFileFolder, filePath);
			if (!indexFile.exists()) {
				indexFile.createNewFile();
			}
			Source source = new DOMSource(vfile);
			Result result = new StreamResult(indexFile);
			trans.transform(source, result);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		} catch (TransformerException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public boolean has(CmsItemId resource) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		String filePath = resource.getRelPath().toString();
		File indexFile = new File(vFileFolder, filePath);
		return indexFile.exists();
	}

	@Override
	public Document get(CmsItemId resource) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		if (!this.has(resource)) {
			return null;
		}
		String filePath = resource.getRelPath().toString();
		File indexFile = new File(vFileFolder, filePath);
		try {
			return db.parse(indexFile);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		} catch (SAXException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}
