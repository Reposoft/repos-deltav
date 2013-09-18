package se.repos.deltav.store;

import org.w3c.dom.Document;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Stores Delta-V files per resource and revision.
 */
public interface DeltaVStore {

	/**
	 * Stores a newer V-file than the one reported by {@link #getHighestCalculated(CmsItemId)}
	 * @param resource the resource+revision that this deltav is calculated for
	 * @param deltav the complete V-file for all revisions up to this one
	 * @throws IllegalArgumentException if the resource lacks peg rev
	 * @throws IllegalArgumentException if the resource has a revision that is older than highest calculated
	 */
	public void put(CmsItemId resource, Document deltav);

	/**
	 * @param resouce identfier, with revision
	 * @return true if this resource at this revision has a V-file
	 */
	public boolean has(CmsItemId resouce);
	
	/**
	 * @param resource identifier, without revision
	 * @return highest revision for which there is a V-file of this resource, null if no earlier revision of the resource
	 */
	public RepoRevision getHighestCalculated(CmsItemId resource);
	
	//public void get(CmsItemId resouces, OutputStream deltav);

	/**
	 * Reads latest V-file fromfrom storage.
	 * @param resource identifier, without revision
	 * @return V-file for the resource, null if no V-file for this resource
	 * @throws IllegalArgumentException if the resource id has revision specified, because we currently keep the current V-file
	 */
	public Document get(CmsItemId resource);
	
}
