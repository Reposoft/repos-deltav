package se.repos.deltav;

import static org.junit.Assert.*;

import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import se.simonsoft.cms.backend.svnkit.commit.CmsCommitSvnkitEditor;
import se.simonsoft.cms.item.CmsItemPath;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.commit.CmsCommit;
import se.simonsoft.cms.item.commit.CmsCommitChangeset;
import se.simonsoft.cms.item.commit.FileAdd;
import se.simonsoft.cms.item.commit.FileModification;
import se.simonsoft.cms.testing.svn.CmsTestRepository;
import se.simonsoft.cms.testing.svn.SvnTestSetup;

/**
 * Try to mimic the runtime scenario in webapp.
 * Volume testing of the actual algorithm might be better placed in a more isolated test using test files directly.
 */
public class DeltaVSvnTest {

	private CmsTestRepository repository;
	private CmsCommit commit;
	
	@Before
	public void setUp() {
		repository = SvnTestSetup.getInstance().getRepository();
		commit = new CmsCommitSvnkitEditor(repository, repository.getSvnkitProvider());
	}
	
	@After
	public void tearDown() {
		SvnTestSetup.getInstance().tearDown();
	}
	
	@Test
	public void testBasic_SvnHttp() {
		InputStream b1 = this.getClass().getClassLoader().getResourceAsStream("se/repos/deltav/basic_1.xml");
		assertNotNull("Should find test resouces is src/test/resources classpath entry", b1);
		CmsCommitChangeset c1 = new CmsCommitChangeset();
		c1.add(new FileAdd(new CmsItemPath("/basic.xml"), new RepoRevision(0, null), b1));
		RepoRevision r1 = commit.run(c1);
		
		InputStream b2 = this.getClass().getClassLoader().getResourceAsStream("se/repos/deltav/basic_2.xml");
		CmsCommitChangeset c2 = new CmsCommitChangeset();
		c2.add(new FileModification(new CmsItemPath("/basic.xml"), r1, b1, b2));
		RepoRevision r2 = commit.run(c2);
		
		InputStream b3 = this.getClass().getClassLoader().getResourceAsStream("se/repos/deltav/basic_2.xml");
		CmsCommitChangeset c3 = new CmsCommitChangeset();
		c3.add(new FileModification(new CmsItemPath("/basic.xml"), r2, b2, b3));
		RepoRevision r3 = commit.run(c3);		
		
		repository.keep();
		
		// Now we have a repository, build the changeset
	}

}
