package se.repos.deltav.store;

import java.io.File;
import java.io.OutputStream;
import org.w3c.dom.Document;
import se.simonsoft.cms.item.CmsItemId;
import xmlindexer.XMLIndexBackend;

public class DeltaVStoreMemory implements DeltaVStore {
	@Override
	public void put(CmsItemId resource, String indexLocation) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		back.setRepo(resource.getRepository().getUrl());
		back.indexFile(resource.getRelPath().toString());
	}

	@Override
	public boolean has(CmsItemId resource, String indexLocation) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		return back.hasIndexFile(indexLocation);
	}

	@Override
	public long getHighestCalculated(CmsItemId resource, String indexLocation) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		return back.parseIndex(indexLocation).getDocumentVersion();
	}

	@Override
	public void get(CmsItemId resource, String indexLocation, OutputStream indexStream) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		File indexFile = back.getIndexFile(indexLocation);
		// TODO Skriv indexFile till indexStream.
	}

	@Override
	public Document get(CmsItemId resource, String indexLocation) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		return back.parseIndex(indexLocation).toDocument();
	}
}
