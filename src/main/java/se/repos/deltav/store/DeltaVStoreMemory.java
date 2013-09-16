package se.repos.deltav.store;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.FileSystems;
import org.tmatesoft.svn.core.SVNException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import xmlindexer.XMLIndexBackend;

public class DeltaVStoreMemory implements DeltaVStore {
	@Override
	public void put(CmsItemId resource, String indexLocation) throws SVNException, IOException, SAXException {
		XMLIndexBackend back = new XMLIndexBackend(resource.getRepository().getUrl(), indexLocation);
		back.indexFile(resource.getRelPath().toString());
	}

	@Override
	public boolean has(CmsItemId resource, String indexLocation) {
		return	getIndexFile(resource,indexLocation).exists();
	}

	@Override
	public RepoRevision getHighestCalculated(CmsItemId resource, String indexLocation) {
		// TODO Returnera docVersion.
		return null;
	}

	@Override
	public void get(CmsItemId resource, String indexLocation, OutputStream index) {
		// TODO Implementera metod.
		return;
	}

	@Override
	public Document get(CmsItemId resource, String indexLocation) {
		// TODO Implementera metod.
		return null;
	}
	
	private File getIndexFile(CmsItemId resource, String indexLocation) {
		return	FileSystems.getDefault().
				getPath(indexLocation, resource.getRelPath().toString()).
				toFile();
	}

}
