package se.repos.deltav;

import javax.inject.Inject;

import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.ChangesetEventListener;
import se.simonsoft.cms.item.events.change.CmsChangeset;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;

/**
 * Draft.
 * One way of invoking the calculator.
 */
public class VfileCommitHandler implements ChangesetEventListener {

	private CmsChangesetReader changesetReader;
	private VfileCommitItemHandler itemHandler;
	
	@Inject
	public VfileCommitHandler(CmsChangesetReader changesetReader, VfileCommitItemHandler itemHandler) {
		this.changesetReader = changesetReader;
		this.itemHandler = itemHandler;
	}
	
	@Override
	public void onCommit(CmsChangeset changeset) {
		RepoRevision revision = changeset.getRevision();
		
		
	}

}
