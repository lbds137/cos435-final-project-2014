import java.io.FileReader;
import java.util.Scanner;
import java.util.ArrayList;

public class Data {

    private int D;
    private int W;
    private int NNZ;
    private int[][] docs; // row is documentID, column is wordID
    private String[] vocab;
    //private ArrayList<ArrayList<Integer>> invertedIndex;
    private double[][] sims;
    
    public Data(String docwordFilename, String vocabFilename){
        parseDocwordFile(docwordFilename);
        parseVocabFile(vocabFilename);
        //buildInvertedIndex();
        buildDocSims();
    }

    private void parseDocwordFile(String filename) {
        try {
            Scanner sc = new Scanner(new FileReader(filename));
            D = sc.nextInt();
            W = sc.nextInt();
            NNZ = sc.nextInt(); // we don't do anything with this currently
            
            docs = new int[D][W];
                
            while (sc.hasNextInt()) {
                int docID = sc.nextInt() - 1; // IDs in given data set begin at 1, not 0
                int wordID = sc.nextInt() - 1; // IDs in given data set begin at 1, not 0
                int count = sc.nextInt();
                
                docs[docID][wordID] = count;
            }
            sc.close();
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
    private void parseVocabFile(String filename) {
        try {
            Scanner sc = new Scanner(new FileReader(filename));
            vocab = new String[W];
            for (int i = 0; sc.hasNextLine(); i++) vocab[i] = sc.nextLine();
            sc.close();
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }
    /*
    private void buildInvertedIndex() {
        invertedIndex = new ArrayList<ArrayList<Integer>>(vocab.length);
        for (int i = 0; i < W; i++) {
            invertedIndex.add(new ArrayList<Integer>());
            for (int j = 0; j < D; j++) {
                if (docs[j][i] != 0) invertedIndex.get(i).add(j);
            }
        }
    }
    */
    // determine document similarities using cosine similarity
    private void buildDocSims() {
        double[] magnitudes = new double[D];
        for (int i = 0; i < D; i++) {
            double sumSquares = 0;
            for (int j = 0; j < W; j++) {
                sumSquares += (docs[i][j] * docs[i][j]);
            }
            magnitudes[i] = Math.sqrt(sumSquares);
        }
        
        sims = new double[D][D];
        for (int i = 0; i < D; i++) {
            for (int j = 0; j < D; j++) {
                if (sims[i][j] != 0) continue; // already computed similarity, so continue
                double dotProduct = 0;
                double sim = 0;
                for (int k = 0; k < W; k++) {
                    dotProduct += (docs[i][k] * docs[j][k]);
                }
                sim = (dotProduct / magnitudes[i]) / magnitudes[j]; // magnitudes should never be zero
                sims[i][j] = sim;
                sims[j][i] = sim;
            }
        }
    }
    
    /* Getters */
    
    public int[][] getDocs() {
        return docs;
    }
    public String[] getVocab() {
        return vocab;
    }
    /*
    public ArrayList<ArrayList<Integer>> getInvertedIndex() {
        return invertedIndex;
    }
    */
    public double[][] getSims() {
        return sims;
    }
    
    /* Debugging / print methods */
    
    public void printDocVectors() {
        for (int i = 0; i < D; i++) {
            System.out.println("-------");
            System.out.println("Document " + i + " vector:");
            System.out.print("{ ");
            for (int j = 0; j < W; j++) {
                System.out.print(docs[i][j] + " ");
            }
            System.out.print(" }\n");
            System.out.println("-------");
        }
    }
    
    /* Testing */
    
    public static void main(String[] args) {
        Data d = new Data("docword.nips.txt", "vocab.nips.txt");
        int[][] docs = d.getDocs();
        String[] vocab = d.getVocab();
        
        //d.printDocVectors();
        
        /*
        int randDocID = (int) (Math.random() * docs.length);
        int randWordID = (int) (Math.random() * docs[0].length);
        System.out.println("Random document ID " + randDocID + " and random word ID " + randWordID + ":");
        System.out.println(docs[randDocID][randWordID]);
        System.out.println(vocab[randWordID]);
        
        ArrayList<ArrayList<Integer>> invertedIndex = d.getInvertedIndex();
        System.out.println("Inverted index for word:");
        for (Integer i : invertedIndex.get(randWordID)) {
            System.out.println("doc ID: " + i + "; count: " + docs[i][randWordID]);
        }
        
        // print out some similarities
        int randDocID2 = (int) (Math.random() * docs.length);
        for (int i = 0; i < 10; i++) {
            randDocID = (int) (Math.random() * docs.length);
            randDocID2 = (int) (Math.random() * docs.length);
            System.out.println("Similarity of doc1 " + randDocID + " and doc2 " + randDocID2 + " is: " + d.getSims()[randDocID][randDocID2]);
        }
        // verify that self-similarity is 1
        System.out.println("Similarity of doc " + randDocID + " to itself is: " + d.getSims()[randDocID][randDocID]);
        */
    }
}
