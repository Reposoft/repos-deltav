package se.repos.deltav.store;

import java.io.OutputStream;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Stores Delta-V files per resource and revision.
 */
public interface DeltaVStore {

	/**
	 * @param resource the resource+revision that this deltav is calculated for
	 * @param deltav TODO what type is suitable for storage, DOM, InputStream, Jdom?
	 */
	public void put(CmsItemId resource, Object deltav);
	
	public boolean has(CmsItemId resouce);
	
	public RepoRevision getHighestCalculated(CmsItemId resource);
	
	public void get(CmsItemId resouces, OutputStream deltav);
		
	public Object get(CmsItemId resource);
	
}
