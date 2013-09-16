/**
 * @author Hugo Svallfors <keiter@lavabit.com>
 */
package xmlindexer;

import java.io.IOException;
import org.tmatesoft.svn.core.SVNException;
import org.xml.sax.SAXException;

public class XMLIndex {

    /**
     * @param args The commandline arguments.
     * The first elements of args must be the URL to the SVN server,
     * the second is the location to build the index and
     * any subsequent ones are files to be indexed.
     * @throws IllegalArgumentException If there are less than three arguments.
     * @throws SVNException If the SVN repository could not be read.
     * @throws IOException If the index file or index folder could not be read/written.
     * @throws SAXException If an XML document could not be parsed.
     */
    public static void main(String[] args)
            throws SVNException, IOException, SAXException {
        if (args.length < 3) {
            throw new IllegalArgumentException("Must have at least 3 arguments.");
        }
        XMLIndexBackend idx = new XMLIndexBackend(args[1]);
        idx.setRepo(args[0]);
        for (int i = 2; i < args.length; i++) {
            idx.indexFile(args[i]);
        }
    }
}
