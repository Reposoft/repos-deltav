package se.repos.deltav.store;

import java.io.IOException;
import java.io.OutputStream;

import org.tmatesoft.svn.core.SVNException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;

/**
 * Stores Delta-V files per resource and revision.
 */
public interface DeltaVStore {

	/**
	 * @param resource the resource+revision that this deltav is calculated for
	 * @param indexLocation The location of the indexes in the file system.
	 * @throws SVNException
	 * @throws SAXException 
	 * @throws IOException 
	 */
	public void put(CmsItemId resource, String indexLocation) throws SVNException, IOException, SAXException;
	
	public boolean has(CmsItemId resource, String indexLocation);
	
	public RepoRevision getHighestCalculated(CmsItemId resource, String indexLocation);
	
	public void get(CmsItemId resource, String indexLocation, OutputStream index);
		
	public Document get(CmsItemId resource, String indexLocation);
}
