package com.uncovet.research.comparator;
import net.semanticmetadata.lire.*;
import net.semanticmetadata.lire.imageanalysis.FCTH;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: vera
 * Date: 5/19/13
 * Time: 3:28 PM
 * To change this template use File | Settings | File Templates.
 */
public class RunLire {
    static class ImageInImageDatabase {

        public String fileName;
        public double[] fcthFeatureVector;
        public double distanceToSearchImage;
    }

    static class ImageComparator implements Comparator<ImageInImageDatabase> {

        @Override
        public int compare(ImageInImageDatabase object1, ImageInImageDatabase object2) {
            if (object1.distanceToSearchImage < object2.distanceToSearchImage) {
                return -1;
            } else if (object1.distanceToSearchImage > object2.distanceToSearchImage) {
                return 1;
            } else {
                return 0;
            }
        }
    }
    public static double[] getFCTHFeatureVector(String fullFilePath) throws FileNotFoundException, IOException {

        DocumentBuilder builder = DocumentBuilderFactory.getFCTHDocumentBuilder();

        FileInputStream istream = new FileInputStream(fullFilePath);
        Document doc = builder.createDocument(istream, fullFilePath);
        istream.close();

        FCTH fcthDescriptor = new FCTH();
        fcthDescriptor.setByteArrayRepresentation(doc.getFields().get(0).binaryValue().bytes);

        return fcthDescriptor.getDoubleHistogram();

    }

    public static double calculateEuclideanDistance(double[] vector1, double[] vector2) {

        double innerSum = 0.0;
        for (int i = 0; i < vector1.length; i++) {
            innerSum += Math.pow(vector1[i] - vector2[i], 2.0);
        }

        return Math.sqrt(innerSum);

    }

    public static void main(String[] args) throws FileNotFoundException, IOException {

        if (args.length != 2) {

            System.out.println("This application requires two parameters: "
                    + "the name of a directory containing JPEG images, and a file name of a JPEG image.");
            return;

        }
        // simple search
        // Checking if arg[0] is there and if it is an image.
        BufferedImage img = null;
        boolean passed = false;
        if (args.length > 0) {
            File f = new File(args[0]);
            if (f.exists()) {
                try {
                    img = ImageIO.read(f);
                    passed = true;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (!passed) {
            System.out.println("No image given as first argument.");
            System.out.println("Run \"Searcher <query image>\" to search for <query image>.");
            System.exit(1);
        }

        IndexReader ir = DirectoryReader.open(FSDirectory.open(new File("/Users/vera/work/images/index")));
        ImageSearcher searcher = ImageSearcherFactory.createDefaultSearcher();

        ImageSearchHits hits = searcher.search(img, ir);
        for (int i = 0; i < hits.length(); i++) {
            String fileName = hits.doc(i).getValues(DocumentBuilder.FIELD_NAME_IDENTIFIER)[0];
            System.out.println(hits.score(i) + ": \t" + fileName);
        }

        // end of search
        String imageDatabaseDirectoryName = args[1];
        String searchImageFilePath = args[0];

        double[] searchImageFeatureVector = getFCTHFeatureVector(searchImageFilePath);

        System.out.println("Search image FCTH vector: " + Arrays.toString(searchImageFeatureVector));

        List<ImageInImageDatabase> database = new ArrayList();

        File directory = new File(imageDatabaseDirectoryName);

        FilenameFilter filter = new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".jpg") || name.endsWith(".jpeg");
            }
        };

        String[] fileNames = directory.list(filter);

        for (String fileName : fileNames) {

            double[] fcthFeatureVector = getFCTHFeatureVector(imageDatabaseDirectoryName + "\\" + fileName);
            double distanceToSearchImage = calculateEuclideanDistance(fcthFeatureVector, searchImageFeatureVector);

            ImageInImageDatabase imageInImageDatabase = new ImageInImageDatabase();

            imageInImageDatabase.fileName = fileName;
            imageInImageDatabase.fcthFeatureVector = fcthFeatureVector;
            imageInImageDatabase.distanceToSearchImage = distanceToSearchImage;

            database.add(imageInImageDatabase);

        }

        Collections.sort(database, new ImageComparator());

        for (ImageInImageDatabase result : database) {

            System.out.println("Distance " + Double.toString(result.distanceToSearchImage) + ": " + result.fileName);

        }

    }
}
