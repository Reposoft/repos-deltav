package se.repos.deltav;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.inject.Inject;
import org.xml.sax.InputSource;
import se.simonsoft.cms.item.CmsItemId;
import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.impl.CmsItemIdUrl;
import se.simonsoft.cms.item.inspection.CmsContentsReader;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

/**
 * Draft.
 * One other way of invoking the calculator.
 */
public class VfileCommitItemHandler {

	private VfileCalculatorImpl calculator; 
	private CmsContentsReader contentsReader;
	
	@Inject
	public VfileCommitItemHandler(VfileCalculatorImpl calculator, CmsContentsReader contentsReader) {
		this.calculator = calculator;
		this.contentsReader = contentsReader;
	}
	
	public void onCommit(CmsRepositoryInspection repository, CmsChangesetItem changesetItem) {
		RepoRevision revision = changesetItem.getRevision();
		CmsItemId itemId = new CmsItemIdUrl(repository, changesetItem.getPath()).withPegRev(revision.getNumber());
		
		// buffers 
		ByteArrayOutputStream current = new ByteArrayOutputStream();
		contentsReader.getContents(repository, revision, itemId.getRelPath(), current);
		InputSource sourceCurrent = new InputSource(new ByteArrayInputStream(current.toByteArray()));
		
		RepoRevision revisionPrevious = changesetItem.getRevisionObsoleted();
		InputSource sourcePrevious = null;
		if (revisionPrevious != null) {
			ByteArrayOutputStream previous = new ByteArrayOutputStream();
			contentsReader.getContents(repository, revisionPrevious, itemId.getRelPath(), previous);
			sourcePrevious = new InputSource(new ByteArrayInputStream(previous.toByteArray()));
		}
		
		calculator.increment(itemId, revisionPrevious, sourcePrevious, revision, sourceCurrent);
	}
	
	
}
