package se.repos.vfile.store;

import java.util.Date;

import org.w3c.dom.Document;

import se.repos.vfile.gen.VFile;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Stores v-files per resource.
 */
public abstract class VFileStore {

    /**
     * Stores a newer V-file than the one reported by
     * {@link #getHighestCalculated(CmsItemId)}
     * 
     * @param resource
     *            he resource+revision that this v-file is calculated for
     * @param vfile
     *            the complete v-file for all revisions up to this one
     * @throws IllegalArgumentException
     *             If resource has a peg revision.
     */
    public abstract void put(CmsItemId resource, Document vfile);

    /**
     * @param resource
     *            identifier
     * @return true if this resource has a V-file
     * @throws IllegalArgumentException
     *             If resource has a peg revision.
     */
    public abstract boolean has(CmsItemId resource);

    /**
     * @param resource
     *            identifier
     * @param version
     *            The version to check if the v-file tracks.
     * @return true if this resource has a V-file that tracks version or later.
     * @throws IllegalArgumentException
     *             If resource has a peg revision.
     */
    public final boolean has(CmsItemId resource, RepoRevision version) {
        RepoRevision highest = this.getHighestCalculated(resource);
        return highest != null && highest.isNewerOrEqual(version);
    }

    /**
     * @param resource
     *            identifier, without revision
     * @return highest revision for which there is a V-file of this resource,
     *         null if no earlier revision of the resource
     * @throws IllegalArgumentException
     *             If resource has a peg revision.
     */
    public final RepoRevision getHighestCalculated(CmsItemId resource) {
        if (resource.getPegRev() != null) {
            throw new IllegalArgumentException("Resource should not have a peg revision.");
        }
        if (!this.has(resource)) {
            return null;
        }
        VFile vfile = new VFile(this.get(resource));
        return new RepoRevision(Long.parseLong(vfile.getDocumentVersion()), new Date(
                Long.parseLong(vfile.getDocumentTime())));
    }

    /**
     * Reads latest V-file from storage.
     * 
     * @param resource
     *            identifier, without revision
     * @return V-file for the resource, null if no V-file for this resource
     * @throws IllegalArgumentException
     *             If resource has a peg revision.
     */
    public abstract Document get(CmsItemId resource);
}
