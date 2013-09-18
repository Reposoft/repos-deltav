package se.repos.deltav;

import java.io.InputStream;

import javax.inject.Inject;

import org.xml.sax.SAXException;

import se.repos.deltav.store.DeltaVStore;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

public class VfileCalculatorImpl {

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
	public void increment(CmsItemId itemId, RepoRevision previous, InputStream previousContent, RepoRevision current, InputStream currentContent) {
		throw new UnsupportedOperationException("Not implemented");
	}
	
}
