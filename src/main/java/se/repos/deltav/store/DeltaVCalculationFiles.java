package se.repos.deltav.store;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.w3c.dom.Document;
import se.simonsoft.cms.item.CmsItemId;
import xmlindexer.XMLIndexBackend;

public class DeltaVCalculationFiles {
	
	public void put(CmsItemId resource, String indexLocation) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		back.setRepo(resource.getRepository().getUrl());
		back.indexFile(resource.getRelPath().toString());
	}

	public boolean has(CmsItemId resource, String indexLocation) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		return back.hasIndexFile(indexLocation);
	}

	public long getHighestCalculated(CmsItemId resource, String indexLocation) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		return back.parseIndex(indexLocation).getDocumentVersion();
	}

	public void get(CmsItemId resource, String indexLocation, OutputStream indexStream) {
		try {
			XMLIndexBackend back = new XMLIndexBackend(indexLocation);
			Path indexFilePath = back.getIndexFile(indexLocation).toPath();
			Files.copy(indexFilePath, indexStream);
		} catch (IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public Document get(CmsItemId resource, String indexLocation) {
		XMLIndexBackend back = new XMLIndexBackend(indexLocation);
		return back.parseIndex(indexLocation).toDocument();
	}
}
