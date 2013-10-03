package se.repos.vfile.test;

import static org.junit.Assert.assertNotNull;

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
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNClientManager;
import org.tmatesoft.svn.core.wc.SVNRevision;
import org.tmatesoft.svn.core.wc.admin.SVNLookClient;
import org.w3c.dom.Document;

import se.repos.vfile.VFileCalculatorImpl;
import se.repos.vfile.VFileCommitHandler;
import se.repos.vfile.VFileCommitItemHandler;
import se.repos.vfile.store.VFileStore;
import se.repos.vfile.store.VFileStoreMemory;
import se.simonsoft.cms.backend.svnkit.CmsRepositorySvn;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsChangesetReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsContentsReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.SvnlookClientProviderStateless;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;

/**
 * Try to mimic the runtime scenario in webapp. Volume testing of the actual
 * algorithm might be better placed in a more isolated test using test files
 * directly.
 */
public class VFileSvnTest {

    // set to false to examine repository after test
    private boolean doCleanup = true;

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
        this.testDir = File.createTempFile("test-" + this.getClass().getName(), "");
        this.testDir.delete();
        this.repoDir = new File(this.testDir, "repo");
        this.repoUrl = SVNRepositoryFactory.createLocalRepository(this.repoDir, true,
                false);
        // for low level operations
        // SVNRepository repo = SVNRepositoryFactory.create(repoUrl);
        this.wc = new File(this.testDir, "wc");
        System.out.println("Running local fs repository " + this.repoUrl);
        this.clientManager = SVNClientManager.newInstance();
    }

    @After
    public void tearDown() throws IOException {
        if (this.doCleanup) {
            FileUtils.deleteDirectory(this.testDir);
        } else {
            System.out.println("Test data kept at: " + this.testDir.getAbsolutePath());
        }
    }

    private void svncheckout() throws SVNException {
        this.clientManager.getUpdateClient().doCheckout(this.repoUrl, this.wc,
                SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, false);
    }

    private RepoRevision svncommit(String comment) throws SVNException {
        long rev = this.clientManager
                .getCommitClient()
                .doCommit(new File[] { this.wc }, false, comment, null, null, false,
                        false, SVNDepth.INFINITY).getNewRevision();
        Date d = this.svnlookProvider.get().doGetDate(this.repoDir,
                SVNRevision.create(rev));
        return new RepoRevision(rev, d);
    }

    private void svnadd(File... paths) throws SVNException {
        this.clientManager.getWCClient().doAdd(paths, true, false, false,
                SVNDepth.INFINITY, true, true, true);
    }

    @Test
    public void testBasic() throws Exception {
        InputStream b1 = this.getClass().getClassLoader()
                .getResourceAsStream("se/repos/vfile/basic_1.xml");
        InputStream b2 = this.getClass().getClassLoader()
                .getResourceAsStream("se/repos/vfile/basic_2.xml");
        InputStream b3 = this.getClass().getClassLoader()
                .getResourceAsStream("se/repos/vfile/basic_3.xml");

        CmsRepositorySvn repository = new CmsRepositorySvn("/anyparent", "anyname",
                this.repoDir);
        CmsContentsReaderSvnkitLook contentsReader = new CmsContentsReaderSvnkitLook();
        contentsReader.setSVNLookClientProvider(this.svnlookProvider);
        CmsChangesetReaderSvnkitLook changesetReader = new CmsChangesetReaderSvnkitLook();
        changesetReader.setSVNLookClientProvider(this.svnlookProvider);

        this.svncheckout();

        File f1 = new File(this.wc, "basic.xml");
        IOUtils.copy(b1, new FileOutputStream(f1));
        this.svnadd(f1);
        RepoRevision r1 = this.svncommit("first");
        assertNotNull("should commit", r1);
        IOUtils.copy(b2, new FileOutputStream(f1));
        RepoRevision r2 = this.svncommit("second");
        IOUtils.copy(b3, new FileOutputStream(f1));
        RepoRevision r3 = this.svncommit("third");

        VFileStore store = new VFileStoreMemory();
        VFileCalculatorImpl calculator = new VFileCalculatorImpl(store);

        // supporting infrastructure
        VFileCommitItemHandler itemHandler = new VFileCommitItemHandler(calculator,
                contentsReader);
        VFileCommitHandler commitHandler = new VFileCommitHandler(repository, itemHandler)
                .setCmsChangesetReader(changesetReader);

        CmsItemId testID = new CmsItemIdUrl(repository, new CmsItemPath("/basic.xml"));
        commitHandler.onCommit(r1);
        Document v1 = store.get(testID);
        assertNotNull("V-file calculation should have stored a something", v1);
        // TODO Assert that structure of index is correct.

        commitHandler.onCommit(r2);
        Document v2 = store.get(testID);
        assertNotNull("V-file should still exist", v2);

        commitHandler.onCommit(r3);
        Document v3 = store.get(testID);
        assertNotNull(v3);
    }

    @Test
    public void testTechdocDemo1() throws Exception {
        InputStream b1 = this.getClass().getClassLoader()
                .getResourceAsStream("se/repos/vfile/techdoc-demo1/900108_A.xml");
        InputStream b2 = this.getClass().getClassLoader()
                .getResourceAsStream("se/repos/vfile/techdoc-demo1/900108_B.xml");
        InputStream b3 = this.getClass().getClassLoader()
                .getResourceAsStream("se/repos/vfile/techdoc-demo1/900108_C.xml");

        CmsRepositorySvn repository = new CmsRepositorySvn("/anyparent", "anyname",
                this.repoDir);
        CmsContentsReaderSvnkitLook contentsReader = new CmsContentsReaderSvnkitLook();
        contentsReader.setSVNLookClientProvider(this.svnlookProvider);
        CmsChangesetReaderSvnkitLook changesetReader = new CmsChangesetReaderSvnkitLook();
        changesetReader.setSVNLookClientProvider(this.svnlookProvider);

        this.svncheckout();

        File f1 = new File(this.wc, "900108.xml");
        IOUtils.copy(b1, new FileOutputStream(f1));
        this.svnadd(f1);
        RepoRevision r1 = this.svncommit("first");
        assertNotNull("should commit", r1);
        IOUtils.copy(b2, new FileOutputStream(f1));
        RepoRevision r2 = this.svncommit("second");
        IOUtils.copy(b3, new FileOutputStream(f1));
        RepoRevision r3 = this.svncommit("third");

        VFileStore store = new VFileStoreMemory();
        VFileCalculatorImpl calculator = new VFileCalculatorImpl(store);

        // supporting infrastructure
        VFileCommitItemHandler itemHandler = new VFileCommitItemHandler(calculator,
                contentsReader);
        VFileCommitHandler commitHandler = new VFileCommitHandler(repository, itemHandler)
                .setCmsChangesetReader(changesetReader);

        CmsItemId testID = new CmsItemIdUrl(repository, new CmsItemPath("/900108.xml"));
        commitHandler.onCommit(r1);
        Document v1 = store.get(testID);
        assertNotNull("V-file calculation should have stored a something", v1);
        // TODO Assert that structure of index is correct.

        commitHandler.onCommit(r2);
        Document v2 = store.get(testID);
        assertNotNull("V-file should still exist", v2);

        commitHandler.onCommit(r3);
        Document v3 = store.get(testID);
        assertNotNull(v3);
    }
}
