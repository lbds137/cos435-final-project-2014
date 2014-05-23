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
    
    /* Utility methods */
    
    public double getAvgSim(ArrayList<Integer> cluster) {
        double avgSim = 0;
        for (int j = 0; j < cluster.size(); j++) {
            int docJ = cluster.get(j);
            for (int k = 0; k < cluster.size(); k++) {
                //if (j == k) continue;
                int docK = cluster.get(k);
                avgSim += sims[docJ][docK];
            }
        }
        avgSim /= (cluster.size() * cluster.size()); // take average (i.e. normalize)
        return avgSim;
    }
    // returns <positive agreement, negative agreement>
    public ArrayList<Double> clusteringAgreement(ArrayList<ArrayList<Integer>> cOne, ArrayList<ArrayList<Integer>> cTwo) {
        if (cOne.size() != cTwo.size()) return null; // clusterings must be same size
        int numClusters = cOne.size();
        int numPairsPosAgree = 0;
        int numPairsNegAgree = 0;
        double numPairsTotal = D * D;
        for (int i = 0; i < D; i++) {
            for (int j = 0; j < D; j++) {
                int iIndexOne = -1;
                int jIndexOne = -1;
                int iIndexTwo = -1;
                int jIndexTwo = -1;
                for (int k = 0; k < numClusters; k++) {
                    if (iIndexOne == -1 && jIndexOne == -1) {
                        iIndexOne = cOne.get(k).indexOf(i);
                        jIndexOne = cOne.get(k).indexOf(j);
                    }
                    if (iIndexTwo == -1 && jIndexTwo == -1) {
                        iIndexTwo = cTwo.get(k).indexOf(i);
                        jIndexTwo = cTwo.get(k).indexOf(j);
                    }
                }
                boolean cOneDiff = ((iIndexOne > -1 && jIndexOne == -1) || (iIndexOne == -1 && jIndexOne > -1));
                boolean cTwoDiff = ((iIndexTwo > -1 && jIndexTwo == -1) || (iIndexTwo == -1 && jIndexTwo > -1));
                boolean cOneSame = ((iIndexOne > -1 && jIndexOne > -1) || (iIndexOne == -1 && jIndexOne == -1));
                boolean cTwoSame = ((iIndexTwo > -1 && jIndexTwo > -1) || (iIndexTwo == -1 && jIndexTwo == -1));
                if (cOneSame && cTwoSame) numPairsPosAgree += 1;
                else if (cOneDiff && cTwoDiff) numPairsNegAgree += 1;
            }
        }
        ArrayList<Double> agreements = new ArrayList<Double>(2);
        agreements.add(numPairsPosAgree / numPairsTotal);
        agreements.add(numPairsNegAgree / numPairsTotal);
        return agreements;
    }
    
    /* Testing */
    
    public static void main(String[] args) {
        boolean useArgs = false;
        if (args.length > 0) {
            try {
                Integer.parseInt(args[0]);
                Integer.parseInt(args[1]);
                Boolean.parseBoolean(args[2]);
                useArgs = true;
            }
            catch (Exception e) {
            }
        }
        int numClusters;
        int numIterations;
        boolean normalize;
        if (useArgs) {
            numClusters = Integer.parseInt(args[0]);
            numIterations = Integer.parseInt(args[1]); 
            normalize = Boolean.parseBoolean(args[2]);
        }
        else {
            numClusters = 100;
            numIterations = 5;
            normalize = true;
        }
        Data d = new Data("docword.nips.txt", "vocab.nips.txt");
        HAC hac = new HAC(d, numClusters);
        HDC hdc = new HDC(d, numClusters, numIterations, normalize);
        
        // print clusters for HAC and HDC
        System.out.println("-------");
        hac.printClusters();
        System.out.println("-------");
        hdc.printClusters();
        System.out.println("-------");
        
        ArrayList<Double> hacAvgClusterSims = hac.getAvgClusterSims();
        ArrayList<Double> hdcAvgClusterSims = hdc.getAvgClusterSims();
        double hacAvgSim = 0;
        double hdcAvgSim = 0;
        for (int i = 0; i < numClusters; i++) {
            hacAvgSim += hacAvgClusterSims.get(i);
            hdcAvgSim += hdcAvgClusterSims.get(i);
        }
        hacAvgSim /= numClusters;
        hdcAvgSim /= numClusters;
        System.out.println("HAC average of average cluster similarities: " + hacAvgSim);
        System.out.println("HDC average of average cluster similarities: " + hdcAvgSim);
        ArrayList<Double> agreements = d.clusteringAgreement(hac.getClusters(), hdc.getClusters());
        double posAgreement = agreements.get(0);
        double negAgreement = agreements.get(1);
        double agreement = posAgreement + negAgreement;
        double disagreement = 1 - agreement;
        System.out.println("Clustering positive agreement: " + posAgreement);
        System.out.println("Clustering negative agreement: " + negAgreement);
        System.out.println("Clustering agreement composite: " + agreement);
        System.out.println("Clustering disagreement: " + disagreement);
    }
}
