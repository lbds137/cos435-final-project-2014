import java.io.FileReader;
import java.util.Scanner;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

/* this HAC implementation uses the complete-link similarity measure */
public class HAC {

    private Data d;
    private int N;
    private double[][] docSims; // initial document similarities
    private double[][] clusterSims; // similarity matrix C
    private ArrayList<ArrayList<Integer>> clusters;
    private ArrayList<ArrayList<Integer>> merges; // rows ordered by temporal order of merges
    private int numClusters;
    
    public HAC(Data d, int numClusters){
        this.d = d;
        N = d.getDocs().length;
        
        docSims = d.getSims();
        clusterSims = new double[N][N];
        clusters = new ArrayList<ArrayList<Integer>>(N);
        merges = new ArrayList<ArrayList<Integer>>();
        for (int i = 0; i < N; i++) {
            // initialize singleton clusters
            clusters.add(new ArrayList<Integer>()); 
            clusters.get(i).add(i);
            clusterSims[i] = Arrays.copyOf(docSims[i], N); // make a copy so we don't mess up original doc sims
        }
        this.numClusters = N;
        
        while (this.numClusters > numClusters) {
            ArrayList<Integer> indices = identifyMerge();
            merge(indices);
            updateSims(indices);
        }
    }
    private void merge(ArrayList<Integer> indices) {
        int toIndex = indices.get(0);
        int fromIndex = indices.get(1);
        ArrayList<Integer> to = clusters.get(toIndex);
        ArrayList<Integer> from = clusters.get(fromIndex);
        //System.out.println("to size: " + to.size() + " ; from size: " + from.size());
        
        // do merge
        while (from.size() > 0) {
            to.add(from.remove(0));
        }
        //System.out.println("to size: " + to.size() + " ; from size: " + from.size());
        
        // update merges data structure
        merges.add(indices);
        numClusters -= 1;
        //System.out.println("merge done!");
    }
    private ArrayList<Integer> identifyMerge() {
        ArrayList<Integer> indices = new ArrayList<Integer>(2);
        int iLargestSim = 0;
        int jLargestSim = 1;
        double largestSim = -1;
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (i == j) continue;
                // tie-breaking: first spotted instance wins
                if (clusterSims[i][j] > clusterSims[iLargestSim][jLargestSim]) {
                    iLargestSim = i;
                    jLargestSim = j;
                    largestSim = clusterSims[i][j];
                }
            }
        }
        // ordering of merges: merge smaller cluster into larger cluster, or 
        // if equal size merge higher index into lower index
        if (clusters.get(iLargestSim).size() > clusters.get(iLargestSim).size()) {
            indices.add(iLargestSim);
            indices.add(jLargestSim);
        }
        else if (clusters.get(iLargestSim).size() < clusters.get(iLargestSim).size()) {
            indices.add(jLargestSim);
            indices.add(iLargestSim);
        }
        else {
            if (iLargestSim < jLargestSim) {
                indices.add(iLargestSim);
                indices.add(jLargestSim);
            }
            else {
                indices.add(jLargestSim);
                indices.add(iLargestSim);
            }
        }
        //System.out.println("about to merge clusters " + indices.get(0) + " and " + indices.get(1) + " with similarity score " + largestSim);
        return indices;
    }
    private void updateSims(ArrayList<Integer> indices) {
        int toIndex = indices.get(0);
        int fromIndex = indices.get(1);
        // "zero out" (i.e. set to -1) similarities for clusters that no longer exist
        for (int i = 0; i < N; i++) {
            if (i == fromIndex) continue;
            clusterSims[i][fromIndex] = -1;
            clusterSims[fromIndex][i] = -1;
        }
        for (int i = 0; i < N; i++) {
            if (i == toIndex) continue;
            double smallestSim = 1; // complete-link similarity: want the smallest
            for (int j = 0; j < clusters.get(toIndex).size(); j++) {
                int docJ = clusters.get(toIndex).get(j);
                for (int k = 0; k < clusters.get(i).size(); k++) { // won't run for empty clusters
                    int docK = clusters.get(i).get(k);
                    if (docSims[docJ][docK] < smallestSim) smallestSim = docSims[docJ][docK];
                }
            }
            if (smallestSim == 1) smallestSim = -1; // should only happen for empty clusters
            clusterSims[i][toIndex] = smallestSim;
            clusterSims[toIndex][i] = smallestSim;
            //System.out.println("smallest sim for " + i + " and " + toIndex + " is " + smallestSim);
        }
        //System.out.println("updated similarities!");
    }
    
    /* Getters */
    
    public ArrayList<ArrayList<Integer>> getMerges() {
        return merges;
    }
    public ArrayList<ArrayList<Integer>> getClusters() {
        ArrayList<ArrayList<Integer>> nonEmptyClusters = new ArrayList<ArrayList<Integer>>();
        for (ArrayList<Integer> cluster : clusters) {
            if (cluster.size() > 0) {
                nonEmptyClusters.add(cluster);
                Collections.sort(cluster);
            }
        }
        return nonEmptyClusters;
    }
    
    /* Debugging / print methods */
    
    public void printClusters() {
        for (int i = 0; i < N; i++) {
            ArrayList<Integer> cluster = clusters.get(i);
            if (cluster.size() > 0) {
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
    }
    public void printMerges() {
        for (int i = 0; i < merges.size(); i++) {
            System.out.println(i + "th merge: cluster " + merges.get(i).get(1) + " merged into cluster " + merges.get(i).get(0));
        }
    }
    
    /* Testing */
    
    public static void main(String[] args) {
        Data d = new Data("docword.nips.txt", "vocab.nips.txt");
        HAC h = new HAC(d, 10);
        h.printMerges();
        h.printClusters();
    }
}
