package se.repos.deltav;

import javax.inject.Inject;

import se.simonsoft.cms.item.RepoRevision;
import se.simonsoft.cms.item.events.ChangesetEventListener;
import se.simonsoft.cms.item.events.change.CmsChangeset;
import se.simonsoft.cms.item.events.change.CmsChangesetItem;
import se.simonsoft.cms.item.inspection.CmsChangesetReader;
import se.simonsoft.cms.item.inspection.CmsRepositoryInspection;

/**
 * Draft.
 * One way of invoking the calculator.
 */
public class VFileCommitHandler implements ChangesetEventListener {

	private CmsRepositoryInspection repository;
	private VFileCommitItemHandler itemHandler;
	private CmsChangesetReader changesetReader = null;
	
	@Inject
	public VFileCommitHandler(CmsRepositoryInspection repository, VFileCommitItemHandler itemHandler) {
		this.repository = repository;
		this.itemHandler = itemHandler;
	}
	
	/**
	 * @param changesetReader because getting a CmsChangeset only is insufficient for VFileCalculator
	 * @return 
	 */
	@Inject
	public VFileCommitHandler setCmsChangesetReader(CmsChangesetReader changesetReader) {
		this.changesetReader  = changesetReader;
		return this;
	}
	
	@Override
	public void onCommit(CmsChangeset changeset) {
		//if (!this.repository.equals(changeset.getRepository())) {
		//	throw new IllegalArgumentException("Was initialized for repository " + this.repository + " but got " + changeset.getRepository());
		//}
		for (CmsChangesetItem item : changeset.getItems()) {
			itemHandler.onCommit(repository, item);
		}
	}
	
	public void onCommit(RepoRevision revision) {
		CmsChangeset changeset = changesetReader.read(repository, revision);
		onCommit(changeset);
	}

}
