package se.repos.vfile.store;

import org.w3c.dom.Document;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

public class VFileStoreBaseX implements VFileStore {

	@Override
	public void put(CmsItemId resource, Document vfile) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		throw new UnsupportedOperationException(
				"BaseX storage implementation deferred");
	}

	@Override
	public boolean has(CmsItemId resource) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		throw new UnsupportedOperationException(
				"BaseX storage implementation deferred");
	}

	@Override
	public boolean has(CmsItemId resource, RepoRevision version) {
		RepoRevision highest = this.getHighestCalculated(resource);
		return highest != null && highest.isNewerOrEqual(version);
	}

	@Override
	public RepoRevision getHighestCalculated(CmsItemId resource) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		throw new UnsupportedOperationException(
				"BaseX storage implementation deferred");
	}

	@Override
	public Document get(CmsItemId resource) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		throw new UnsupportedOperationException(
				"BaseX storage implementation deferred");
	}

}
