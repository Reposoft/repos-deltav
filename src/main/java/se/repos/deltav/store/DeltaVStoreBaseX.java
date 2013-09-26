package se.repos.deltav.store;

import org.w3c.dom.Document;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

public class DeltaVStoreBaseX implements DeltaVStore {

	@Override
	public void put(CmsItemId resource, Document deltav) {
		throw new UnsupportedOperationException("BaseX storage implementation deferred");
	}

	@Override
	public boolean has(CmsItemId resource) {
		throw new UnsupportedOperationException("BaseX storage implementation deferred");
	}

	@Override
	public RepoRevision getHighestCalculated(CmsItemId resource) {
		throw new UnsupportedOperationException("BaseX storage implementation deferred");
	}

	@Override
	public Document get(CmsItemId resource) {
		throw new UnsupportedOperationException("BaseX storage implementation deferred");
	}

}
