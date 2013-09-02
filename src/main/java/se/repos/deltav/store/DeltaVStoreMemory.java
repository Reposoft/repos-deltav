package se.repos.deltav.store;

import java.io.OutputStream;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

public class DeltaVStoreMemory implements DeltaVStore {

	@Override
	public void put(CmsItemId resource, Object deltav) {
		// TODO pending API design
		
	}

	@Override
	public boolean has(CmsItemId resouce) {
		// TODO pending API design
		return false;
	}

	@Override
	public RepoRevision getHighestCalculated(CmsItemId resource) {
		// TODO pending API design
		return null;
	}

	@Override
	public void get(CmsItemId resouces, OutputStream deltav) {
		// TODO pending API design
		
	}

	@Override
	public Object get(CmsItemId resource) {
		// TODO pending API design
		return null;
	}

}
