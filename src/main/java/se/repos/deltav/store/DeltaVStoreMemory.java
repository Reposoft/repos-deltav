package se.repos.deltav.store;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import org.tmatesoft.svn.core.SVNException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.simonsoft.cms.item.CmsItemId;
import xmlindexer.XMLIndexBackend;

public class DeltaVStoreMemory implements DeltaVStore {
	@Override
	public void put(CmsItemId resource, String indexLocation) {
		try {
			XMLIndexBackend back = new XMLIndexBackend(indexLocation);
			back.setRepo(resource.getRepository().getUrl());
			back.indexFile(resource.getRelPath().toString());
		} catch (SVNException | IOException | SAXException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public boolean has(CmsItemId resource, String indexLocation) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		return back.hasIndexFile(indexLocation);
	}

	@Override
	public long getHighestCalculated(CmsItemId resource, String indexLocation) {
		try {
			XMLIndexBackend back = new XMLIndexBackend(indexLocation);
			return back.parseIndex(indexLocation).getDocumentVersion();
		} catch (SVNException | IOException | SAXException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	@Override
	public void get(CmsItemId resource, String indexLocation, OutputStream indexStream) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		File indexFile = back.getIndexFile(indexLocation);
		// TODO Skriv indexFile till indexStream.
	}

	@Override
	public Document get(CmsItemId resource, String indexLocation) {
		try {
			XMLIndexBackend back = new XMLIndexBackend(indexLocation);
			return back.parseIndex(indexLocation).toDocument();
		} catch (SVNException | IOException | SAXException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
}
