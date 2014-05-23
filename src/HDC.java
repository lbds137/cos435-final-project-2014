import java.io.FileReader;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Collections;

public class HDC {

    private Data d;
    private int N;
    private int numClusters;
    private int numIterations;
    private boolean normalize;
    private double[][] docSims; // initial document similarities
    private ArrayList<ArrayList<Integer>> clusters;
    private ArrayList<Double> avgClusterSims;
    
    public HDC(Data d, int numClusters, int numIterations, boolean normalize){
        this.d = d;
        N = d.getDocs().length;
        this.normalize = normalize;
        
        docSims = d.getSims();
        clusters = new ArrayList<ArrayList<Integer>>(numClusters);
        avgClusterSims = new ArrayList<Double>();
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
        
        // keep splitting clusters until desired number of clusters is reached
        while (this.numClusters < numClusters) {
            int clusterToSplit = chooseSplit();
            //System.out.println("cluster to split " + clusterToSplit + " of size " + clusters.get(clusterToSplit).size());
            split(clusterToSplit);
        }
        // sort each cluster and compute average sims
        for (ArrayList<Integer> cluster : clusters) {
            Collections.sort(cluster);
            avgClusterSims.add(d.getAvgSim(cluster));
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
        //System.out.println("avg sim of cluster to split: " + avgSims.get(clusterToSplit));
        return clusterToSplit;
    }
    private double getAvgSim(ArrayList<Integer> cluster) {
        return d.getAvgSim(cluster);
    }
    private double getIntraCost(ArrayList<Integer> cluster) {
        return getAvgSim(cluster) * cluster.size() * cluster.size(); // un-normalize
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
                //System.out.println("centroids: " + centroidOne + ", " + centroidTwo);
                possibleSplit = getSplit(cluster, centroidOne, centroidTwo);
                //System.out.println("possible split sizes: " + possibleSplit.get(0).size() + ", " + possibleSplit.get(1).size());
            }
            possibleSplits.add(possibleSplit);
            splitCosts.add(getSplitCost(possibleSplits.get(i)));
        }
        int bestSplit = 0;
        for (int i = 0; i < numIterations; i++) {
            if (splitCosts.get(i) < splitCosts.get(bestSplit)) bestSplit = i;
        }
        //System.out.println("best split cost is " + splitCosts.get(bestSplit));
        // now that we have the best split, perform the actual split
        clusters.set(numClusters, possibleSplits.get(bestSplit).get(1));
        cluster.removeAll(possibleSplits.get(bestSplit).get(1));
        numClusters += 1;
        //System.out.println("split complete");
    }
    private ArrayList<ArrayList<Integer>> getSplit(ArrayList<Integer> cluster, int centroidOne, int centroidTwo) {
        ArrayList<ArrayList<Integer>> split = new ArrayList<ArrayList<Integer>>(2);
        split.add(new ArrayList<Integer>());
        split.add(new ArrayList<Integer>());
        for (int i = 0; i < cluster.size(); i++) {
            double simOne = docSims[cluster.get(i)][cluster.get(centroidOne)];
            double simTwo = docSims[cluster.get(i)][cluster.get(centroidTwo)];
            if (simOne > simTwo) split.get(0).add(cluster.get(i));
            else split.get(1).add(cluster.get(i));
        }
        /*
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
        double intraCostOne;
        double intraCostTwo;
        if (normalize) {
            intraCostOne = getAvgSim(split.get(0));
            intraCostTwo = getAvgSim(split.get(1));
        }
        else {
            intraCostOne = getIntraCost(split.get(0));
            intraCostTwo = getIntraCost(split.get(1));
        }
        double cutCost = 0; // cutcosts are the same since only splitting in two clusters
        for (int i = 0; i < split.get(0).size(); i++) {
            for (int j = 0; j < split.get(1).size(); j++) {
                cutCost += docSims[split.get(0).get(i)][split.get(1).get(j)];
            }
        }
        if (normalize) {
            cutCost /= (split.get(0).size() * split.get(1).size());
        }
        double splitCost = ((cutCost / intraCostOne) + (cutCost / intraCostTwo));
        //System.out.println("split cost is " + splitCost + ", where split sizes are " + split.get(0).size() + " and " + split.get(1).size());
        return splitCost;
    }
    
    /* Getters */
    
    public ArrayList<ArrayList<Integer>> getClusters() {
        return clusters;
    }
    public ArrayList<Double> getAvgClusterSims() {
        return avgClusterSims;
    }
    
    /* Debugging / print methods */
    
    public void printClusters() {
        for (int i = 0; i < numClusters; i++) {
            System.out.print("{ ");
            for (int j = 0; j < clusters.get(i).size(); j++) {
                System.out.print(clusters.get(i).get(j) + " ");
            }
            System.out.print("}\n");
        }
    }
    
    /* Testing */
    
    public static void main(String[] args) {
        Data d = new Data("docword.nips.txt", "vocab.nips.txt");
        HDC h = new HDC(d, 1500, 10, true);
        h.printClusters();
    }
}
