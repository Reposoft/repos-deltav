package xmlindexer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;
import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.auth.*;
import org.tmatesoft.svn.core.io.*;
import org.custommonkey.xmlunit.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * @author Hugo Svallfors <keiter@lavabit.com>
 * The backend to the Indexer. Responsible for fetching the history of a file
 * from SVN and sending it to Index for Index creation.
 */
public class XMLIndexBackend {

    private SVNRepository repo;
    private String indexLocation;
    private DocumentBuilder db;

    /**
     * The constructor for a backend.
     * @param repoURL The URL to the SVN repository.
     * May use protocol HTTP or SVN.
     * @param indexLocation A filepath in the local filesystem where the
     * index files will be put.
     * @throws SVNException If the connection to SVN repository can't be initialized.
     */
    public XMLIndexBackend(String repoURL, String indexLocation)
            throws SVNException {
        repo = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(repoURL));
        this.indexLocation = indexLocation;

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        dbf.setIgnoringComments(true);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setValidating(false);
        try {
            db = dbf.newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex.getMessage());
        }

        // Sets global parameters for XMLUnit.
        XMLUnit.setCompareUnmatched(false);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLUnit.setIgnoreComments(true);
        XMLUnit.setIgnoreDiffBetweenTextAndCDATA(true);
        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setNormalize(true);
        XMLUnit.setNormalizeWhitespace(false);

    }

    /**
     * Sets authenticator for SVN repository.
     * Defaults to no authentication.
     * @param auth The authenticator to use.
     */
    public void setAuth(ISVNAuthenticationManager auth) {
        repo.setAuthenticationManager(auth);
    }

    /**
     * Gets authenticator for SVN repository.
     * Defaults to no authentication.
     * @return The authenticator currently being used.
     */
    public ISVNAuthenticationManager getAuth() {
        return repo.getAuthenticationManager();
    }

    /**
     * Fetches a single file from the SVN repository, given by path,
     * and creates a similarly named file in the index folder, containing
     * a v-file of that file's history. If such a file already exists,
     * it will be parsed as an index, and indexation will resume
     * from that indexes docVersion.
     * @param The path to index.
     * @throws SVNException If the path in question does not exist.
     * @throws IOException If the index could not be written to file.
     * @throws SAXException If either the index file or documents in the
     * SVN repo are not well-formed XML.
     */
    public void indexFile(String path)
            throws SVNException, IOException, SAXException {
        System.out.println("Indexing " + path);
        ArrayList<SVNFileRevision> fileRevisions = new ArrayList<>();

        File indexFile =
                FileSystems.getDefault().getPath(indexLocation, path).toFile();
        Index index;
        if (!indexFile.exists()) {
            // Create new index file from first version.
            System.out.print("Fetching repository history...");
            repo.getFileRevisions(path, fileRevisions, 0, repo.getLatestRevision());
            long firstRev = fileRevisions.get(0).getRevision();
            Document doc = fetchDocumentVersion(path, firstRev);
            System.out.println("done.");
            System.out.print("Normalizing first version of " + path + "...");
            index = Index.normalizeDocument(doc, firstRev);
            fileRevisions.remove(0);
            index.writeToFile(indexFile);
            System.out.println("done.");
        } else {
            System.out.println("Parsing index at " + path + "...");
            index = parseIndex(path, indexFile);
            long lastRevision = index.getDocumentVersion();
            System.out.println("Parsed version " + lastRevision + " of index.");
            System.out.print("Fetching repository history...");
            repo.getFileRevisions(path, fileRevisions, lastRevision + 1,
                    repo.getLatestRevision());
            System.out.println("done.");
        }

        for (SVNFileRevision sfr : fileRevisions) {
            long newRevision = sfr.getRevision();
            if (newRevision <= index.getDocumentVersion()) {
                continue;
            }
            System.out.print("Indexing version " + newRevision
                    + " of " + path + "...");
            Document newDocument = fetchDocumentVersion(path, newRevision);
            index.update(newDocument, newRevision);
            index.writeToFile(indexFile);
            System.out.println("done.");
        }

        index.writeToFile(indexFile);
        System.out.println("Finished indexing " + path + "!");
    }

    /**
     * Fetches the file given by path in the SVN repository at
     * the version number indicated, parses it as XML and returns the XML
     * Document.
     * @param path The path to fetch.
     * @param version The version to fetch path at.
     * @throws SVNException If the path could not be fetched at that particular
     * version.
     * @throws SAXException If the file fetched could not be parsed as XML.
     * @throws IOException If an IO error occurs while retrieveing the file.
     * @return The file in question as an XML tree.
     */
    private Document fetchDocumentVersion(String path, long version)
            throws SVNException, SAXException, IOException {
        ByteArrayOutputStream fileContents = new ByteArrayOutputStream();
        repo.getFile(path, version, null, fileContents);
        InputSource input =
                new InputSource(new StringReader(fileContents.toString()));
        Document doc = db.parse(input);
        return doc;
    }

    /**
     * Reads the file provided as a v-file, fetches the docVesrion of the
     * file indexed, and returns the index parsed.
     * @throws SVNException If the corresponding document could not be fetched.
     * @throws IOException If the file could not be read.
     * @throws SAXException If either the file or index is not well-formed XML.
     * @throws IllegalArgumentException If the index file lacks the lifetime
     * attributes it should have.
     * @return The index that was parsed.
     */
    private Index parseIndex(String path, File file)
            throws SAXException, IOException, SVNException {
        String message = "Malformed index at: " + path;
        Document doc = db.parse(file);
        doc.normalizeDocument();
        Element root = doc.getDocumentElement();
        if (root == null) {
            throw new IllegalArgumentException(message);
        } else {
            String versionString = root.getAttribute(StringConstants.DOCVERSION);
            if (versionString == null) {
                throw new IllegalArgumentException(message);
            }
            long version = Long.parseLong(versionString);
            return new Index(doc, fetchDocumentVersion(path, version));
        }
    }
}
