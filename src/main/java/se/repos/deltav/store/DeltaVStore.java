package se.repos.deltav.store;

import java.io.OutputStream;
import org.w3c.dom.Document;
import se.simonsoft.cms.item.CmsItemId;

/**
 * Stores Delta-V files per resource and revision.
 */
public interface DeltaVStore {

	/**
	 * @param resource the resource+revision that this deltav is calculated for
	 * @param indexLocation The location of the indexes in the file system.
	 */
	public void put(CmsItemId resource, String indexLocation);
	
	public boolean has(CmsItemId resource, String indexLocation);
	
	public long getHighestCalculated(CmsItemId resource, String indexLocation);
	
	public void get(CmsItemId resource, String indexLocation, OutputStream indexStream);
		
	public Document get(CmsItemId resource, String indexLocation);
}
