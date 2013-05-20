package com.uncovet.research.comparator;

import net.semanticmetadata.lire.DocumentBuilderFactory;
import net.semanticmetadata.lire.impl.ChainedDocumentBuilder;
import net.semanticmetadata.lire.utils.FileUtils;
import net.semanticmetadata.lire.utils.LuceneUtils;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * Created with IntelliJ IDEA.
 * User: vera
 * Date: 5/19/13
 * Time: 5:34 PM
 * To change this template use File | Settings | File Templates.
 */

public class Indexer {
    public static void main(String[] args) throws IOException {
        // Checking if arg[0] is there and if it is a directory.
        boolean passed = false;
        if (args.length > 0) {
            File f = new File(args[0]);
            System.out.println("Indexing images in " + args[0]);
            if (f.exists() && f.isDirectory()) passed = true;
        }
        if (!passed) {
            System.out.println("No directory given as first argument.");
            System.out.println("Run \"Indexer <directory>\" to index files of a directory.");
            System.exit(1);
        }
        // Getting all images from a directory and its sub directories.
        ArrayList<String> images = FileUtils.getAllImages(new File(args[0]), true);

        // Use multiple DocumentBuilder instances:
        ChainedDocumentBuilder builder = new ChainedDocumentBuilder();
        builder.addBuilder(DocumentBuilderFactory.getCEDDDocumentBuilder());
        builder.addBuilder(DocumentBuilderFactory.getEdgeHistogramBuilder());
        builder.addBuilder(DocumentBuilderFactory.getOpponentHistogramDocumentBuilder());

        // Creating an Lucene IndexWriter
        IndexWriterConfig conf = new IndexWriterConfig(LuceneUtils.LUCENE_VERSION,
                new WhitespaceAnalyzer(LuceneUtils.LUCENE_VERSION));
        IndexWriter iw = new IndexWriter(FSDirectory.open(new File("/Users/vera/work/images/index")), conf);
        // Iterating through images building the low level features
        for (Iterator<String> it = images.iterator(); it.hasNext(); ) {
            String imageFilePath = it.next();
            System.out.println("Indexing " + imageFilePath);
            try {

                BufferedImage img = ImageIO.read(new FileInputStream(imageFilePath));
                Document document = builder.createDocument(img, imageFilePath);
                iw.addDocument(document);
            } catch (Exception e) {
                System.err.println("Error reading image or indexing it.");
                e.printStackTrace();
            }
        }
        // closing the IndexWriter
        iw.close();
        System.out.println("Finished indexing.");
    }
}
