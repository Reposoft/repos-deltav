package se.repos.vfile.store;

import java.util.HashMap;

import org.w3c.dom.Document;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import xmlindexer.Index;

public class VFileStoreMemory implements VFileStore {
	private HashMap<CmsItemId, Document> vFileTable;

	public VFileStoreMemory() {
		vFileTable = new HashMap<>();
	}

	@Override
	public void put(CmsItemId resource, Document vfile) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		vFileTable.put(resource, vfile);
	}

	@Override
	public boolean has(CmsItemId resource) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		return vFileTable.containsKey(resource);
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
		if (!vFileTable.containsKey(resource)) {
			return null;
		}
		String docVersion = new Index(vFileTable.get(resource))
				.getDocumentVersion();
		// TODO Convert docVersion to a RepoRevision?
		return null;
	}

	@Override
	public Document get(CmsItemId resource) {
		if (resource.getPegRev() != null) {
			throw new IllegalArgumentException(
					"Resource should not have a peg revision.");
		}
		return vFileTable.get(resource);
	}
}
