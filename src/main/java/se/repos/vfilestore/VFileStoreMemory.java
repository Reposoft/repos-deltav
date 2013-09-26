package se.repos.vfilestore;

import org.w3c.dom.Document;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import xmlindexer.Index;

import java.util.HashMap;

public class VFileStoreMemory implements VFileStore {
	private HashMap<CmsItemId, Document> vFileTable;
	
	public VFileStoreMemory() {
		vFileTable = new HashMap<>();
	}

	@Override
	public void put(CmsItemId resource, Document vfile) {
		vFileTable.put(resource, vfile);
	}

	@Override
	public boolean has(CmsItemId resource) {
		return vFileTable.containsKey(resource);
	}

	@Override
	public RepoRevision getHighestCalculated(CmsItemId resource) {
		if(!this.has(resource)) {
			return null;
		}
		String docVersion = new Index(vFileTable.get(resource)).getDocumentVersion();
		// TODO Convert docVersion to a RepoRevision?
		return null;
	}

	@Override
	public Document get(CmsItemId resource) {
		return vFileTable.get(resource);
	}
	
}
