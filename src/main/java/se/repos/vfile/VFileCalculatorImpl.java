package se.repos.vfile;

import java.io.IOException;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import se.repos.vfile.gen.VFile;
import se.repos.vfile.store.VFileStore;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

public class VFileCalculatorImpl {

	private static final Logger logger = LoggerFactory
			.getLogger(VFileCalculatorImpl.class);

	private VFileStore storage;
	private DocumentBuilder db;

	@Inject
	public VFileCalculatorImpl(VFileStore storage) {
		this.storage = storage;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setIgnoringComments(true);
		dbf.setIgnoringElementContentWhitespace(true);
		dbf.setNamespaceAware(true);
		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * 
	 * @param itemId
	 *            The key for storing the item, with peg revision equal to
	 *            current
	 * @param previous
	 *            Previous commit revision, i.e. previous V-file is expected to
	 *            exist at this location if the item has a history, null if the
	 *            item was just added
	 * @param oldContent
	 *            parseable as XML, null if the item was just added
	 * @param current
	 *            Current commit revision, with timestamp
	 * @param newContent
	 *            parseable as XML
	 * @throws IllegalStateException
	 *             if previous is non-null and but the revision does not exist
	 *             in V-file storage
	 */
	public void increment(CmsItemId itemId, RepoRevision previous,
			InputSource oldContent, RepoRevision current, InputSource newContent) {
		logger.debug("Increment requested for {} {}->{}", itemId, previous,
				current);
		VFile index;
		try {
			if (!storage.has(itemId)) {
				index = VFile.normalizeDocument(db.parse(newContent),
						Long.toString(current.getDate().getTime()),
						Long.toString(current.getNumber()));
			} else {
				index = new VFile(storage.get(itemId));
				index.update(db.parse(oldContent), db.parse(newContent),
						Long.toString(current.getDate().getTime()),
						Long.toString(current.getNumber()));
			}
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		} catch (SAXException e) {
			throw new RuntimeException(e.getMessage());
		}
		storage.put(itemId, index.toDocument());
	}
}
