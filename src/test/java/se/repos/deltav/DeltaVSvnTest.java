package se.repos.deltav;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import javax.inject.Provider;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNPropertyValue;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.w3c.dom.Document;

import se.repos.deltav.store.DeltaVStore;
import se.repos.deltav.store.DeltaVStoreMemory;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsChangesetReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsContentsReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.SvnlookClientProviderStateless;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangeset;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

/**
 * Try to mimic the runtime scenario in webapp.
 * Volume testing of the actual algorithm might be better placed in a more isolated test using test files directly.
 */
public class DeltaVSvnTest {

	private boolean doCleanup = true; // set to false to examine repository after test
	
	private File testDir = null;
	private File repoDir = null;
	private SVNURL repoUrl;
	private File wc = null;
	
	private SVNClientManager clientManager = null;
	private Provider<SVNLookClient> svnlookProvider = new SvnlookClientProviderStateless();
	
	static {
		FSRepositoryFactory.setup();
	}
	
	@Before
	public void setUp() throws IOException, SVNException {
		testDir = File.createTempFile("test-" + this.getClass().getName(), "");
		testDir.delete();
		repoDir = new File(testDir, "repo");
		repoUrl = SVNRepositoryFactory.createLocalRepository(repoDir, true, false);
		// SVNRepository repo = SVNRepositoryFactory.create(repoUrl); // for low level operations
		wc = new File(testDir, "wc");
		System.out.println("Running local fs repository " + repoUrl);
		clientManager = SVNClientManager.newInstance();
	}
	
	@After
	public void tearDown() throws IOException {
		if (doCleanup) {
			FileUtils.deleteDirectory(testDir);
		} else {
			System.out.println("Test data kept at: " + testDir.getAbsolutePath());
		}
	}
	
	private void svncheckout() throws SVNException {
		clientManager.getUpdateClient().doCheckout(repoUrl, wc, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
	}
	
	private void svnupdate() throws SVNException {
		clientManager.getUpdateClient().doUpdate(wc, SVNRevision.HEAD, SVNDepth.INFINITY, false, true);
	}
	
	private RepoRevision svncommit(String comment) throws SVNException {
		long rev = clientManager.getCommitClient().doCommit(
				new File[]{wc}, false, comment, null, null, false, false, SVNDepth.INFINITY).getNewRevision();
		Date d = svnlookProvider.get().doGetDate(repoDir, SVNRevision.create(rev));
		return new RepoRevision(rev, d);
	}
	
	private RepoRevision svncommit() throws SVNException {
		return svncommit("");
	}
	
	private void svnpropset(File path, String propname, String propval) throws SVNException {
		clientManager.getWCClient().doSetProperty(path, propname, SVNPropertyValue.create(propval), false, SVNDepth.EMPTY, null, null);
	}

	private void svnadd(File... paths) throws SVNException {
		clientManager.getWCClient().doAdd(
				paths, true, false, false, SVNDepth.INFINITY, true, true, true);
	}
	
	@Test
	public void testBasic() throws Exception {
		InputStream b1 = this.getClass().getClassLoader().getResourceAsStream("se/repos/deltav/basic_1.xml");
		InputStream b2 = this.getClass().getClassLoader().getResourceAsStream("se/repos/deltav/basic_2.xml");
		InputStream b3 = this.getClass().getClassLoader().getResourceAsStream("se/repos/deltav/basic_3.xml");

		CmsRepositoryInspection repository = new CmsRepositoryInspection("/anyparent", "anyname", repoDir);
		
		svncheckout();
		
		File f1 = new File(wc, "basic.xml");
		IOUtils.copy(b1, new FileOutputStream(f1));
		svnadd(f1);
		RepoRevision r1 = svncommit("first");
		IOUtils.copy(b2, new FileOutputStream(f1));
		RepoRevision r2 = svncommit("second");
		IOUtils.copy(b3, new FileOutputStream(f1));
		RepoRevision r3 = svncommit("third");
		
		DeltaVStore store = new DeltaVStoreMemory();
		
		// TODO instantiate Delta-V calculator, inject CmsChangesetReader
		// trigger calculation for revision 1, should produce a delta-v file in storage
		VfileCalculatorImpl calculator = new VfileCalculatorImpl(store);
		
		// supporting infrastructure
		CmsContentsReaderSvnkitLook contentsReader = new CmsContentsReaderSvnkitLook();
		contentsReader.setSVNLookClientProvider(svnlookProvider);
		VfileCommitItemHandler itemHandler = new VfileCommitItemHandler(calculator, contentsReader);
		
		CmsChangesetReader reader = new CmsChangesetReaderSvnkitLook().setSVNLookClientProvider(svnlookProvider);
		CmsChangeset c1 = reader.read(repository, r1);
		for (CmsChangesetItem changesetItem : c1.getItems()) {
			itemHandler.onCommit(repository, changesetItem);
		}
		
		// now expect r1 to have been caclulated and stored
		Document v1 = store.get(new CmsItemIdUrl(repository, new CmsItemPath("/basic.xml")));
		assertNotNull("V-file calculation should have stored a something", v1);
		// TODO assert structure. Use XmlUnit, jsoup or jdom?
		
		for (CmsChangesetItem changesetItem : reader.read(repository, r2).getItems()) itemHandler.onCommit(repository, changesetItem);
		
		Document v2 = store.get(new CmsItemIdUrl(repository, new CmsItemPath("/basic.xml")));
		assertNotNull("V-file should still exist", v2);
		
		for (CmsChangesetItem changesetItem : reader.read(repository, r3).getItems()) itemHandler.onCommit(repository, changesetItem);
		
		Document v3 = store.get(new CmsItemIdUrl(repository, new CmsItemPath("/basic.xml")));
		assertNotNull(v3);
	}

}
