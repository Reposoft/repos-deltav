package se.repos.vfile.store;

import java.util.HashMap;

import org.w3c.dom.Document;

import se.simonsoft.cms.item.CmsItemId;

public class VFileStoreMemory extends VFileStore {
    private HashMap<CmsItemId, Document> vFileTable;

    public VFileStoreMemory() {
        this.vFileTable = new HashMap<CmsItemId, Document>();
    }

    @Override
    public void put(CmsItemId resource, Document vfile) {
        if (resource.getPegRev() != null) {
            throw new IllegalArgumentException("Resource should not have a peg revision.");
        }
        this.vFileTable.put(resource, vfile);
    }

    @Override
    public boolean has(CmsItemId resource) {
        if (resource.getPegRev() != null) {
            throw new IllegalArgumentException("Resource should not have a peg revision.");
        }
        return this.vFileTable.containsKey(resource);
    }

    @Override
    public Document get(CmsItemId resource) {
        if (resource.getPegRev() != null) {
            throw new IllegalArgumentException("Resource should not have a peg revision.");
        }
        return this.vFileTable.get(resource);
    }
}
