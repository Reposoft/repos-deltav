package se.repos.deltav;

import java.io.IOException;
import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import se.repos.deltav.store.VFileStore;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import xmlindexer.Index;

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
	 * @throws SAXException
	 *             if parsing of previous or current fails
	 */
	public void increment(CmsItemId itemId, RepoRevision previous,
			InputSource oldContent, RepoRevision current, InputSource newContent) {
		logger.debug("Increment requested for {} {}->{}", itemId, previous, current);
		Index index;
		try {
			if (!storage.has(itemId)) {
				index = Index.normalizeDocument(db.parse(newContent), current.toString());
			} else {
				index = new Index(storage.get(itemId));
				index.update(db.parse(oldContent), db.parse(newContent),
						current.toString());
			}
		} catch (IOException | SAXException e) {
			throw new RuntimeException(e.getMessage());
		}
		storage.put(itemId, index.toDocument());
	}

}
