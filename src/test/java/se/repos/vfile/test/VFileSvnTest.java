package se.repos.vfile.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import javax.inject.Provider;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
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
import se.repos.vfile.gen.VFile;
import se.repos.vfile.store.VFileStore;
import se.repos.vfile.store.VFileStoreDisk;
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

    /**
     * An answer to the TODO in {@link #testTechdocDemo1()}, maybe not needed for the tests.
     */
	@BeforeClass // maybe we should just pass DOMs to XMLUnit instead
	public static void setUpXMLUnit() {
		javax.xml.parsers.DocumentBuilderFactory dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance();
		dbf.setValidating(false);
		try {
			dbf.setFeature("http://xml.org/sax/features/namespaces", false);
			dbf.setFeature("http://xml.org/sax/features/validation", false);
			dbf.setFeature(
					"http://apache.org/xml/features/nonvalidating/load-dtd-grammar",
					false);
			dbf.setFeature(
					"http://apache.org/xml/features/nonvalidating/load-external-dtd",
					false);
		} catch (Exception e) {
			e.printStackTrace();
		}

		org.custommonkey.xmlunit.XMLUnit.setTestDocumentBuilderFactory(dbf);
		org.custommonkey.xmlunit.XMLUnit.setControlDocumentBuilderFactory(dbf);
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

    /*
     * Takes a series of file paths, runs unit test that asserts they can be
     * v-filed. Puts generated v-file at testFilePath.
     */
    private void testVFiling(String testFilePath, String... filePaths) throws Exception {

        // Parse the files as Documents for data integrity checking.
        DocumentBuilder db = null;
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);
        dbf.setValidating(false);
        db = dbf.newDocumentBuilder();

        ArrayList<Document> documents = new ArrayList<Document>();
        for (String filePath : filePaths) {
            documents.add(db.parse(this.getClass().getClassLoader()
                    .getResourceAsStream(filePath)));
        }

        CmsRepositorySvn repository = new CmsRepositorySvn("/anyparent", "anyname",
                this.repoDir);
        CmsContentsReaderSvnkitLook contentsReader = new CmsContentsReaderSvnkitLook();
        contentsReader.setSVNLookClientProvider(this.svnlookProvider);
        CmsChangesetReaderSvnkitLook changesetReader = new CmsChangesetReaderSvnkitLook();
        changesetReader.setSVNLookClientProvider(this.svnlookProvider);

        this.svncheckout();

        ArrayList<RepoRevision> revisions = new ArrayList<RepoRevision>();

        File testFile = new File(this.wc, testFilePath);
        boolean addedToSVN = false;

        Transformer trans = TransformerFactory.newInstance().newTransformer();
        for (Document d : documents) {
            Source source = new DOMSource(d);
            Result result = new StreamResult(testFile);
            trans.transform(source, result);
            if (!addedToSVN) {
                this.svnadd(testFile);
                addedToSVN = true;
            }
            revisions.add(this.svncommit(""));
        }

        VFileStore store = new VFileStoreDisk("./vfilestore");
        VFileCalculatorImpl calculator = new VFileCalculatorImpl(store);

        // supporting infrastructure
        VFileCommitItemHandler itemHandler = new VFileCommitItemHandler(calculator,
                contentsReader);
        VFileCommitHandler commitHandler = new VFileCommitHandler(repository, itemHandler)
                .setCmsChangesetReader(changesetReader);

        CmsItemId testID = new CmsItemIdUrl(repository, new CmsItemPath("/"
                + testFilePath));
        for (int i = 0; i < documents.size(); i++) {
            commitHandler.onCommit(revisions.get(i));
            VFile v = new VFile(store.get(testID));
            assertNotNull("V-file calculation should have stored something", v);
            Document d = documents.get(i);
            assertTrue((v.documentEquals(d)));
        }
    }

    @Test
    public void testBasic() throws Exception {
        this.testVFiling("basic.xml", "se/repos/vfile/basic_1.xml",
                "se/repos/vfile/basic_2.xml", "se/repos/vfile/basic_3.xml");
    }

    @Test
    public void testBasicInline() throws Exception {
        this.testVFiling("basic.xml",
        		"se/repos/vfile/basic_1.xml",
                "se/repos/vfile/basic_2.xml",
                "se/repos/vfile/basic_3_inline.xml");
    }    
    
    @Test
    public void testTechdocDemo1() throws Exception {
        // TODO Why does DOM try to open DTD file even when validation is off?
        this.testVFiling("900108.xml", "se/repos/vfile/techdoc-demo1/900108_A.xml",
                "se/repos/vfile/techdoc-demo1/900108_B.xml",
                "se/repos/vfile/techdoc-demo1/900108_C.xml");
    }
}
