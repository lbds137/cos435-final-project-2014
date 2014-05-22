import java.io.FileReader;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;

public class HDC {

    private Data d;
    private int N;
    private double[][] docSims; // initial document similarities
    private ArrayList<ArrayList<Integer>> clusters;
    private int numClusters;
    private int numIterations;
    
    public HDC(Data d, int numClusters, int numIterations){
        this.d = d;
        N = d.getDocs().length;
        
        docSims = d.getSims();
        clusters = new ArrayList<ArrayList<Integer>>(numClusters);
        for (int i = 0; i < numClusters; i++) {
            // initialize empty clusters
            clusters.add(new ArrayList<Integer>());
        }
        for (int i = 0; i < N; i++) {
            // add all documents to 0th cluster to start
            clusters.get(0).add(i);
        }
        this.numClusters = 1; // start with one big cluster
        this.numIterations = numIterations;
        
        while (this.numClusters < numClusters) {
            int clusterToSplit = chooseSplit();
            System.out.println("cluster to split: " + clusterToSplit);
            split(clusterToSplit);
        }
    }
    
    // choose cluster to split: cluster with lowest average similarity
    private int chooseSplit() {
        ArrayList<Double> avgSims = new ArrayList<Double>(numClusters);
        for (int i = 0; i < numClusters; i++) {
            avgSims.add(getAvgSim(clusters.get(i)));
        }
        int clusterToSplit = 0;
        for (int i = 0; i < numClusters; i++) {
            if (avgSims.get(i) < avgSims.get(clusterToSplit)) clusterToSplit = i;
        }
        return clusterToSplit;
    }
    private double getAvgSim(ArrayList<Integer> cluster) {
        double avgSim = 0;
        for (int j = 0; j < cluster.size(); j++) {
            int docJ = cluster.get(j);
            for (int k = 0; k < cluster.size(); k++) {
                //if (j == k) continue;
                int docK = cluster.get(k);
                avgSim += docSims[docJ][docK];
            }
        }
        avgSim /= (cluster.size() * cluster.size()); // take double counting into account
        //if (cluster.size() == 1) avgSim = 1; // corner case with only one document
        return avgSim;
    }
    private void split(int clusterToSplit) {
        ArrayList<Integer> cluster = clusters.get(clusterToSplit);
        ArrayList<ArrayList<ArrayList<Integer>>> possibleSplits = new ArrayList<ArrayList<ArrayList<Integer>>>(numIterations);
        ArrayList<Double> splitCosts = new ArrayList<Double>(numIterations);
        for (int i = 0; i < numIterations; i++) {
            ArrayList<ArrayList<Integer>> possibleSplit = new ArrayList<ArrayList<Integer>>(2);
            possibleSplit.add(new ArrayList<Integer>());
            possibleSplit.add(new ArrayList<Integer>());
            while (possibleSplit.get(0).size() == 0 || possibleSplit.get(1).size() == 0) {
                int centroidOne = (int) (Math.random() * cluster.size());
                int centroidTwo = (int) (Math.random() * cluster.size());
                while (centroidOne == centroidTwo) { // we want the centroids to be distinct
                    centroidTwo = (int) (Math.random() * cluster.size());
                }
                possibleSplit = getSplit(cluster, centroidOne, centroidTwo);
            }
            possibleSplits.add(possibleSplit);
            splitCosts.add(getSplitCost(possibleSplits.get(i)));
        }
        int bestSplit = 0;
        for (int i = 0; i < numIterations; i++) {
            if (splitCosts.get(i) < splitCosts.get(bestSplit)) bestSplit = i;
        }
        System.out.println("best split cost is " + splitCosts.get(bestSplit));
        // now that we have the best split, perform the actual split
        clusters.set(numClusters, possibleSplits.get(bestSplit).get(1));
        cluster.removeAll(possibleSplits.get(bestSplit).get(1));
        numClusters += 1;
    }
    private ArrayList<ArrayList<Integer>> getSplit(ArrayList<Integer> cluster, int centroidOne, int centroidTwo) {
        ArrayList<ArrayList<Integer>> split = new ArrayList<ArrayList<Integer>>(2);
        split.add(new ArrayList<Integer>());
        split.add(new ArrayList<Integer>());
        for (int i = 0; i < cluster.size(); i++) {
            double simOne = docSims[cluster.get(i)][centroidOne];
            double simTwo = docSims[cluster.get(i)][centroidTwo];
            if (simOne > simTwo) split.get(0).add(cluster.get(i));
            else split.get(1).add(cluster.get(i));
        }
        
        /*
        // debug (splits work, yay!)
        System.out.println("printing split of sizes " + split.get(0).size() + " and " + split.get(1).size());
        for (ArrayList<Integer> c : split) {
            System.out.print("{ ");
            for (Integer i : c) {
                System.out.print(i + " ");
            }
            System.out.print("} \n");
        }*/
        
        return split;
    }
    private double getSplitCost(ArrayList<ArrayList<Integer>> split) {
        double intraCostOne = getAvgSim(split.get(0));
        double intraCostTwo = getAvgSim(split.get(1));
        double cutCost = 0; // cutcosts are the same since only splitting in two clusters
        for (int i = 0; i < split.get(0).size(); i++) {
            for (int j = 0; j < split.get(1).size(); j++) {
                cutCost += docSims[split.get(0).get(i)][split.get(1).get(j)];
            }
        }
        cutCost /= (split.get(0).size() * split.get(1).size()); // normalize
        double splitCost = ((cutCost / intraCostOne) + (cutCost / intraCostTwo));
        System.out.println("split cost is " + splitCost + ", where split sizes are " + split.get(0).size() + " and " + split.get(1).size());
        return splitCost;
    }
    
    /* Debugging / print methods */
    
    public void printClusters() {
        for (int i = 0; i < numClusters; i++) {
            ArrayList<Integer> cluster = clusters.get(i);
            Collections.sort(cluster);
            System.out.println("-------");
            System.out.println("Cluster " + i + " contains the following document IDs:");
            System.out.print("{ ");
            for (int j = 0; j < clusters.get(i).size(); j++) {
                System.out.print(clusters.get(i).get(j) + " ");
            }
            System.out.print("}");
            System.out.print("\n");
            System.out.println("-------");
        }
    }
    
    /* Testing */
    
    public static void main(String[] args) {
        Data d = new Data("docword.nips.txt", "vocab.nips.txt");
        HDC h = new HDC(d, 10, 50);
        h.printClusters();
    }
}
