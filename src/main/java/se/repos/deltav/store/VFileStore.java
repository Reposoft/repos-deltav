package se.repos.deltav.store;

import org.w3c.dom.Document;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Stores v-files per resource and revision.
 */
public interface VFileStore {

	/**
	 * Stores a newer V-file than the one reported by
	 * {@link #getHighestCalculated(CmsItemId)}
	 * 
	 * @param resource
	 *            the resource+revision that this v-file is calculated for
	 * @param deltav
	 *            the complete v-file for all revisions up to this one
	 * @throws IllegalArgumentException
	 *             if the resource lacks peg revision.
	 * @throws IllegalArgumentException
	 *             if the resource has a revision that is older than highest
	 *             calculated
	 */
	public void put(CmsItemId resource, Document deltav);

	/**
	 * @param resource
	 *            identifier, with revision
	 * @return true if this resource at this revision has a V-file
	 */
	public boolean has(CmsItemId resource);

	/**
	 * @param resource
	 *            identifier, without revision
	 * @return highest revision for which there is a V-file of this resource,
	 *         null if no earlier revision of the resource
	 */
	public RepoRevision getHighestCalculated(CmsItemId resource);

	/**
	 * Reads latest V-file from storage.
	 * @param resource
	 *            identifier, without revision
	 * @return V-file for the resource, null if no V-file for this resource
	 * @throws IllegalArgumentException
	 *             if the resource id has revision specified, because we
	 *             currently keep the current V-file
	 */
	public Document get(CmsItemId resource);
}
