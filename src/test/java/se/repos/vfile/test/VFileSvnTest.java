package se.repos.vfile.test;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathNotExists;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

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
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.tmatesoft.svn.core.SVNCommitInfo;
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
import se.repos.vfile.VFileDocumentBuilderFactory;
import se.repos.vfile.gen.VFile;
import se.repos.vfile.store.VFileStore;
import se.repos.vfile.store.VFileStoreDisk;
import se.simonsoft.cms.backend.svnkit.CmsRepositorySvn;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsChangesetReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.CmsContentsReaderSvnkitLook;
import se.simonsoft.cms.backend.svnkit.svnlook.SvnlookClientProviderStateless;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.CmsRepository;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;

/**
 * Try to mimic the runtime scenario in webapp. Volume testing of the actual
 * algorithm might be better placed in a more isolated test using test files
 * directly.
 */
@SuppressWarnings("deprecation")
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

    @BeforeClass
    public static void setUpXMLUnit() {
        DocumentBuilderFactory dbf = new VFileDocumentBuilderFactory();
        org.custommonkey.xmlunit.XMLUnit.setTestDocumentBuilderFactory(dbf);
        org.custommonkey.xmlunit.XMLUnit.setControlDocumentBuilderFactory(dbf);

        XMLUnit.setCompareUnmatched(false);
        XMLUnit.setExpandEntityReferences(true);
        XMLUnit.setIgnoreComments(false);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalize(true);
        XMLUnit.setNormalizeWhitespace(false);
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
        SVNCommitInfo info = this.clientManager.getCommitClient().doCommit(
                new File[] { this.wc }, false, comment, null, null, false, false,
                SVNDepth.INFINITY);
        long revision = info.getNewRevision();
        if (revision < 0L) {
            // TODO This is thrown by test5k11revs.
            throw new RuntimeException("SVN returned negative version number!");
        }
        return new RepoRevision(revision, info.getDate());
    }

    private void svnadd(File... paths) throws SVNException {
        this.clientManager.getWCClient().doAdd(paths, true, false, false,
                SVNDepth.INFINITY, true, true, true);
    }

    /*
     * Takes a series of file paths, runs unit test that asserts they can be
     * v-filed.
     */
    private VFileStore testVFiling(CmsItemId testID, File folder, String... filePaths)
            throws Exception {

        // Parse the files as Documents for data integrity checking.
        DocumentBuilder db = new VFileDocumentBuilderFactory().newDocumentBuilder();
        ArrayList<Document> documents = new ArrayList<Document>();
        for (String filePath : filePaths) {
            Document d = db.parse(new File(folder, filePath));
            documents.add(d);
        }

        CmsRepositorySvn repository = new CmsRepositorySvn(testID.getRepository()
                .getParentPath(), testID.getRepository().getName(), this.repoDir);
        CmsContentsReaderSvnkitLook contentsReader = new CmsContentsReaderSvnkitLook();
        contentsReader.setSVNLookClientProvider(this.svnlookProvider);
        CmsChangesetReaderSvnkitLook changesetReader = new CmsChangesetReaderSvnkitLook();
        changesetReader.setSVNLookClientProvider(this.svnlookProvider);

        this.svncheckout();

        ArrayList<RepoRevision> revisions = new ArrayList<RepoRevision>();

        File testFile = new File(this.wc, testID.getRelPath().getPath());
        boolean addedToSVN = false;

        /*
         * Commits all the files to SVN, saving the RepoRevisions of each
         * commit.
         */
        Transformer trans = TransformerFactory.newInstance().newTransformer();
        for (int i = 0; i < documents.size(); i++) {
            Document d = documents.get(i);
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

        VFileCommitItemHandler itemHandler = new VFileCommitItemHandler(calculator,
                contentsReader);
        VFileCommitHandler commitHandler = new VFileCommitHandler(repository, itemHandler)
                .setCmsChangesetReader(changesetReader);

        /*
         * For each revision, call V-Filing on the new file version, and assert
         * that the V-File is equal to the saved document.
         */
        for (int i = 0; i < documents.size(); i++) {
            commitHandler.onCommit(revisions.get(i));
            VFile v = new VFile(store.get(testID));
            assertNotNull("V-file calculation should have stored something", v);
            Document d = documents.get(i);
            v.matchDocument(d);
        }

        return store;
    }

    @Test
    public void testBasic() throws Exception {
        CmsRepository repository = new CmsRepository("/anyparent", "anyname");
        CmsItemId testID = new CmsItemIdUrl(repository, new CmsItemPath("/basic.xml"));
        VFileStore store = this.testVFiling(testID, new File(
                "src/test/resources/se/repos/vfile"), "/basic_1.xml", "basic_2.xml",
                "basic_3.xml");

        Document document = store.get(testID);
        assertNotNull("Result should be available through VFileStore", document);

        // this document has no inline nodes and should therefore not have such
        // history info
        assertXpathNotExists("//mixtext", document);
    }

    @Test
    public void testBasicInline() throws Exception {
        CmsRepository repository = new CmsRepository("/anyparent", "anyname");
        CmsItemId testID = new CmsItemIdUrl(repository, new CmsItemPath(
                "/basic-inline.xml"));
        this.testVFiling(testID, new File("src/test/resources/se/repos/vfile"),
                "basic_1.xml", "basic_2.xml", "basic_3_inline.xml");
    }

    @Test
    public void testTechdocDemo1Rids() throws Exception {
        CmsRepository repository = new CmsRepository("/anyparent", "anyname");
        CmsItemId testID = new CmsItemIdUrl(repository, new CmsItemPath(
                "/900108-rids.xml"));
        VFileStore store = this.testVFiling(testID, new File(
                "src/test/resources/se/repos/vfile/techdoc-demo1"), "900108_A.xml",
                "900108_B.xml", "900108_C.xml");
        Document document = store.get(testID);
        assertNotNull("Result should be available through VFileStore", document);
    }

    @Test
    public void testTechdocDemo1Norid() throws Exception {
        CmsRepository repository = new CmsRepository("/anyparent", "anyname");
        CmsItemId testID = new CmsItemIdUrl(repository, new CmsItemPath(
                "/900108-norid.xml"));
        this.testVFiling(testID, new File(
                "src/test/resources/se/repos/vfile/techdoc-demo1-norid"),
                "/900108_A.xml", "900108_B.xml", "900108_C.xml");
    }

    @Test
    public void test5k11revs() throws Exception {
        CmsRepository repository = new CmsRepository("/anyparent", "anyname");
        CmsItemId testID = new CmsItemIdUrl(repository, new CmsItemPath("/5k-11revs.xml"));
        this.testVFiling(testID, new File("src/test/resources/se/repos/vfile/5k-11revs"),
                "mo_0915.xml", "mo_0967.xml", "mo_1008.xml", "mo_1032.xml",
                "mo_1072.xml", "mo_1110.xml", "mo_1170.xml", "mo_1214.xml",
                "mo_1228.xml", "mo_1235.xml", "mo_1330.xml");
    }

    @Test
    public void test50k27revs() throws Exception {
        CmsRepository repository = new CmsRepository("/anyparent", "anyname");
        CmsItemId testID = new CmsItemIdUrl(repository,
                new CmsItemPath("/50k-27revs.xml"));
        this.testVFiling(testID,
                new File("src/test/resources/se/repos/vfile/50k-27revs"), "ca_0053.xml",
                "ca_0057.xml", "ca_0066.xml", "ca_0068.xml", "ca_0069.xml",
                "ca_0097.xml", "ca_0153.xml", "ca_0154.xml", "ca_0155.xml",
                "ca_0156.xml", "ca_0582.xml", "ca_0708.xml", "ca_0798.xml",
                "ca_0803.xml", "ca_0818.xml", "ca_1068.xml", "ca_1069.xml",
                "ca_1128.xml", "ca_1293.xml", "ca_1294.xml", "ca_1300.xml",
                "ca_1301.xml", "ca_1303.xml", "ca_1344.xml", "ca_1355.xml",
                "ca_1388.xml", "ca_1399.xml");
    }

    @Test
    @Ignore
    /*
     * This is an edge case with embedded DTD and entities that can't be parsed
     * for now.
     */
    public void test200kManyRevs() throws Exception {
        CmsRepository repository = new CmsRepository("/anyparent", "anyname");
        CmsItemId testID = new CmsItemIdUrl(repository, new CmsItemPath(
                "/200k-manyrevs.xml"));
        this.testVFiling(testID, new File(
                "src/test/resources/se/repos/vfile/200k-manyrevs"), "ed_0005.xml",
                "ed_0083.xml", "ed_0249.xml", "ed_0571.xml", "ed_0601.xml",
                "ed_0602.xml", "ed_0610.xml", "ed_0615.xml", "ed_0621.xml",
                "ed_1184.xml", "ed_1185.xml", "ed_1186.xml", "ed_1188.xml",
                "ed_1189.xml", "ed_1400.xml");
    }
}
