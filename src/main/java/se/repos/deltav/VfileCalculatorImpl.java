package se.repos.deltav;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import se.repos.deltav.store.DeltaVStore;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

public class VfileCalculatorImpl {

	private static final Logger logger = 
			LoggerFactory.getLogger(VfileCalculatorImpl.class);
	
	private DeltaVStore storage;
	
	@Inject
	public VfileCalculatorImpl(DeltaVStore storage) {
		this.storage = storage;
	}
	
	/**
	 * 
	 * @param itemId The key for storing the item, with peg revision equal to current
	 * @param previous Previous commit revision, i.e. previous V-file is expected to exist at this location if the item has a history, null if the item was just added
	 * @param previousContent parseable as XML, null if the item was just added
	 * @param current Current commit revision, with timestamp
	 * @param currentContent parseable as XML
	 * @throws IllegalStateException if previous is non-null and but the revision does not exist in V-file storage
	 * @throws SAXException if parsing of previous or current fails
	 */
	public void increment(
			CmsItemId itemId, RepoRevision previous, InputSource previousContent,
			RepoRevision current, InputSource currentContent) {
		logger.debug("Increment requested for {} {}->{}", itemId, previous, current);
		//throw new UnsupportedOperationException("Not implemented");
		// TODO Implement VFileCalculator.increment.
	}
	
}
