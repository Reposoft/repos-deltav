package xmlindexer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.*;
import java.util.*;
import javax.xml.parsers.*;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.tmatesoft.svn.core.*;
import org.tmatesoft.svn.core.io.*;
import org.custommonkey.xmlunit.*;
import org.w3c.dom.*;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**	The backend to the Indexer.
 * 	Responsible for fetching the history of a file from SVN and sending
 *	it to Index for Index creation.
 * @author Hugo Svallfors <keiter@lavabit.com>
 */
public class XMLIndexBackend {

	private SVNRepository repo = null;
	private String indexLocation;
	private DocumentBuilder db;

	/**
	 * The constructor for a backend.
	 * 
	 * @param indexLocation
	 *            A filepath in the local filesystem where the index files will
	 *            be put.
	 */
	public XMLIndexBackend(String indexLocation) {
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

	public SVNRepository getRepo() {
		return this.repo;
	}

	public void setRepo(SVNRepository repo) {
		this.repo = repo;
	}

	public void setRepo(String repoURL) {
		try {
			repo = SVNRepositoryFactory.create(SVNURL.parseURIEncoded(repoURL));
		} catch (SVNException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	public File getIndexFile(String path) {
		return FileSystems.getDefault().getPath(indexLocation, path).toFile();
	}

	public boolean hasIndexFile(String path) {
		return getIndexFile(path).exists();
	}

	/**
	 * Fetches a single file from the SVN repository, given by path, and creates
	 * a similarly named file in the index folder, containing a v-file of that
	 * file's history. If such a file already exists, it will be parsed as an
	 * index, and indexation will resume from that indexes docVersion.
	 * 
	 * @param The
	 *            path to index.
	 */
	public void indexFile(String path) {
		try {
			System.out.println("Indexing " + path);
			ArrayList<SVNFileRevision> fileRevisions = new ArrayList<>();
			File indexFile = getIndexFile(path);
			Index index;
			if (!indexFile.exists()) {
				// Create new index file from first version.
				System.out.print("Fetching repository history...");
				repo.getFileRevisions(path, fileRevisions, 0,
						repo.getLatestRevision());
				long firstRev = fileRevisions.get(0).getRevision();
				Document doc = fetchDocumentVersion(path, firstRev);
				System.out.println("done.");
				System.out
						.print("Normalizing first version of " + path + "...");
				index = Index.normalizeDocument(doc, firstRev);
				fileRevisions.remove(0);
				writeToFile(indexFile, index);
				System.out.println("done.");
			} else {
				System.out.println("Parsing index at " + path + "...");
				index = parseIndex(path);
				long lastRevision = index.getDocumentVersion();
				System.out.println("Parsed version " + lastRevision
						+ " of index.");
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
				System.out.print("Indexing version " + newRevision + " of " + path + "...");
				Document newDocument = fetchDocumentVersion(path, newRevision);
				index.update(newDocument, newRevision);
				writeToFile(indexFile, index);
				System.out.println("done.");
			}
			writeToFile(indexFile, index);
			System.out.println("Finished indexing " + path + "!");
		} catch (SVNException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Fetches the file given by path in the SVN repository at the version
	 * number indicated, parses it as XML and returns the XML Document.
	 * @param path The path to fetch.
	 * @param version The version to fetch path at.
	 * @return The file in question as an XML tree.
	 */
	private Document fetchDocumentVersion(String path, long version) {
		try {
			ByteArrayOutputStream fileContents = new ByteArrayOutputStream();
			repo.getFile(path, version, null, fileContents);

			InputSource input = new InputSource(new StringReader(
					fileContents.toString()));
			Document doc = db.parse(input);
			return doc;
		} catch (SVNException | SAXException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Reads the file provided as a v-file, fetches the docVersion of the file
	 * indexed, and returns the index parsed. attributes it should have.
	 * 
	 * @return The index that was parsed.
	 */
	public Index parseIndex(String path) {
		try {
			String message = "Malformed index at: " + path;
			Document doc;
			doc = db.parse(getIndexFile(path));
			doc.normalizeDocument();
			Element root = doc.getDocumentElement();
			if (root == null) {
				throw new IllegalArgumentException(message);
			} else {
				String versionString = root
						.getAttribute(StringConstants.DOCVERSION);
				if (versionString == null) {
					throw new IllegalArgumentException(message);
				}
				long version = Long.parseLong(versionString);
				return new Index(doc, fetchDocumentVersion(path, version));
			}
		} catch (SAXException | IOException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

	/**
	 * Writes the indexDocument to the given file.
	 */
	public void writeToFile(File file, Index index) {
		try {
			File parentFolder = file.getParentFile();
			if (!parentFolder.exists() && !parentFolder.mkdirs()) {
				throw new RuntimeException(
						"Failed to create index file location:" + parentFolder);
			} else if (!file.exists() && !file.createNewFile()) {
				throw new RuntimeException("Failed to create index file:"
						+ file);
			}
			Source source = new DOMSource(index.toDocument());
			Result result = new StreamResult(file);
			Transformer xformer = TransformerFactory.newInstance()
					.newTransformer();
			xformer.transform(source, result);
		} catch (TransformerException | TransformerFactoryConfigurationError | IOException ex) {
			throw new RuntimeException(ex.getMessage());
		}
	}
}
