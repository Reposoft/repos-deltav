package se.repos.deltav.store;

import org.w3c.dom.Document;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

public class DeltaVStoreBaseX implements DeltaVStore {

	@Override
	public void put(CmsItemId resource, Document deltav) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean has(CmsItemId resouce) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public RepoRevision getHighestCalculated(CmsItemId resource) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Document get(CmsItemId resource) {
		// TODO Auto-generated method stub
		return null;
	}

}
