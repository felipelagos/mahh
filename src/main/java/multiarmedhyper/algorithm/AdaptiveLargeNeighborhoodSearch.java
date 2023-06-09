
package multiarmedhyper.algorithm;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import multiarmedhyper.data.ParameterReader;

public class AdaptiveLargeNeighborhoodSearch implements Algorithm {
    
    // parameters
    final ParameterReader param;
    boolean set = true;
    double timeLimit = 90.0;
    int iterLimit = (int) timeLimit;
    
    // heuristics
    int nRemoval = 9;
    int nInsertion = 10;
    
    // solution
    Solution best;
    boolean status;
    double bestValue;
    
    // running 
    double time;
    String output = "";
    int app;
    Random rand;
    
    // matrixes and utils
    Map<Integer, Double> removal;
    Map<Integer, Double> insertion;
    Double[] sigma = new Double[] {33.0, 9.0, 13.0};
    List<Integer> hashValues;
    double r = 0.1;
    
    public AdaptiveLargeNeighborhoodSearch(ParameterReader param) {
        this.param = param;
        status = false;
        rand = new Random();
    }
    
    @Override
    public void setSeed(long seed) {
        rand.setSeed(seed);
    }
    
    @Override
    public ParameterReader getParam() {
        return param;
    }
    
    @Override
    public void setTimeLimit(double timeLimit) {
        this.timeLimit = timeLimit;
    }
    
    @Override
    public void setIterLimit(int iterLimit) {
        this.iterLimit = iterLimit;
    }
    
    @Override
    public void setDelta(double delta) { }
    
    @Override
    public void solve() {
        
        // best solution
        best = new Solution(param);
        best.setRandom(rand);
        
        // print basic info
        print(param.info() + "\n\n");
        print(algorithmLine() + "\n");
        print("running " + algorithmName() + " \n");
        print(algorithmLine() + "\n\n");
        
        // start with saving method
        best.savingsMethod();
        bestValue = best.getTotalCost();
        Solution current = new Solution(best);
        double currentCost = current.getTotalCost();
        double tempe = -bestValue * 0.05 / 100 / Math.log(0.5);
        
        // initial values
        initWeights();
        Map<String, Integer> totals = new HashMap<>();
        Map<String, Double> scores = new HashMap<>();
        initMapping(totals, scores);
        hashValues = new ArrayList<>();
        hashValues.add(current.hashValue());
        
        // table header
        print(toTable("time", 12) + "|" + toTable("apply", 10) 
                + "|" + toTable("best", 18) + "|" + toTable("current", 18)
                + "|" + toTable("tmp", 18) + "|" + toTable("sequence", 20) + "\n");
        print("-".repeat(92) + "\n");
        
        // starting time and number of sequences applications
        long starting = System.currentTimeMillis();
        time = 0.0;
        app = 0;
        
        // iterate while time < limit
        while (time < timeLimit && app < iterLimit) {
            
            // a sequence is applied
            app++;

            // tmp and current solutions
            Solution tmp = new Solution(current);
            Integer[] indexes = applyHeuristics(tmp);
            double tmpCost = tmp.getTotalCost();
            Integer code = tmp.hashValue();
            int type = -1;

            // check if new best solution is found
            if (tmpCost < bestValue) {
                best = new Solution(tmp);
                bestValue = tmpCost;
                current = new Solution(tmp);
                currentCost = current.getTotalCost();
                type = 0;
            } else if (tmpCost < currentCost) {

                // update solution 
                current = new Solution(tmp);
                currentCost = current.getTotalCost();
                if (!hashValues.contains(code)) {
                    type = 1;
                }
                hashValues.add(current.hashValue());
            } else {

                // accept solution with prob
                double prob = Math.exp(-1.0 / tempe * (tmpCost - bestValue));
                if (rand.nextDouble() < prob) {
                    
                    // update solution
                    current = new Solution(tmp);
                    currentCost = current.getTotalCost();
                    if (!hashValues.contains(code)) {
                        type = 2;
                    }

                    hashValues.add(current.hashValue());
                }
            }
            updateScores(indexes, type, totals, scores);
            
            time = (System.currentTimeMillis() - starting) / 1E3;
            print(toTable(Double.toString(time), 12) + "|" 
                    + toTable(Integer.toString(app), 10) 
                    + "|" + toTable(Double.toString(Math.round(bestValue * 1E2) / 1E2), 18) 
                    + "|" + toTable(Double.toString(Math.round(currentCost * 1E2) / 1E2), 18) 
                    + "|" + toTable(Double.toString(Math.round(tmpCost * 1E2) / 1E2), 18) 
                    + "|" + toTable(Arrays.toString(indexes), 20) + "\n");
            
            // temperature 
            tempe = tempe * 0.99975;
            
            // update scores
            if ((app + 1) % 100 == 0) {
                updateProbabilities(totals, scores);
                
                // new values
                initMapping(totals, scores);
            }
        }
        
        status = best.isFeasible();
        
        // print solution
        print("-".repeat(92) + "\n\n");
        printSolution();
    }
    
    void updateProbabilities(Map<String, Integer> totals, Map<String, Double> scores) {
        
        // removal 
        Double total1 = 0.0;
        for (int i = 0; i < nRemoval; i++) {

            double value = removal.get(i);
            if (totals.get("R" + i) > 0) {
                value = scores.get("R" + i) / totals.get("R" + i);
            }
            double weight = (1 - r) * removal.get(i) + r * value;
            removal.put(i, weight);
            total1 += weight;
        }
        for (int i = 0; i < nRemoval; i++) {
            removal.put(i, removal.get(i) / total1);
        }

        // insertion 
        Double total2 = 0.0;
        for (int i = 0; i < nInsertion; i++) {
            double value = insertion.get(i);
            if (totals.get("I" + i) > 0) {
                value = scores.get("I" + i) / totals.get("I" + i);
            }
            double weight = (1 - r) * insertion.get(i) + r * value;
            insertion.put(i, weight);
            total2 += weight;
        }
        for (int i = 0; i < 10; i++) {
            insertion.put(i, insertion.get(i) / total2);
        }
    }
    
    void updateScores(Integer[] indexes, int type,
            Map<String, Integer> totals, Map<String, Double> scores) {
        
        // mapping names
        String rem = "R" + indexes[0];
        String ins = "I" + indexes[1];

        // update totals
        totals.put(rem, totals.get(rem) + 1);
        totals.put(ins, totals.get(ins) + 1);
        
        // update scores
        if (type > -1) {
            scores.put(rem, scores.get(rem) + sigma[type]);
            scores.put(ins, scores.get(ins) + sigma[type]);
        }
    }
    
    void initMapping(Map<String, Integer> totals, Map<String, Double> scores) {
        
        totals.clear();
        scores.clear();
        
        // add values
        for (int i = 0; i < nRemoval; i++) {
            totals.put("R" + i, 0);
            scores.put("R" + i, 0.0);
        }
        for (int i = 0; i < nInsertion; i++) {
            totals.put("I" + i, 0);
            scores.put("I" + i, 0.0);
        }
    }
    
    void initWeights() {
        
        // matrixes 
        removal = new HashMap<>();
        insertion = new HashMap<>();
        
        // initial values removal
        for (int i = 0; i < nRemoval; i++) {
            removal.put(i, 1.0 / nRemoval);
        }
        
        // initial values insertion
        for (int i = 0; i < nInsertion; i++) {
            insertion.put(i, 1.0 / nInsertion);
        }
    }
    
    Integer[] applyHeuristics(Solution current) {
        
        // indexes
        Integer[] indexes = new Integer[2];
        
        // find removal
        double unif1 = rand.nextDouble();
        double ac1 = 0;
        for (int i = 0; i < nRemoval; i++) {
            ac1 += removal.get(i);
            if (unif1 < ac1) {
                indexes[0] = i;
                break;
            }
        }
        
        // find insertion
        double unif2 = rand.nextDouble();
        double ac2 = 0;
        for (int i = 0; i < nInsertion; i++) {
            ac2 += insertion.get(i);
            if (unif2 < ac2) {
                indexes[1] = i;
                break;
            }
        }
           
        // remove nodes
        Set<Integer> nodes = new HashSet<>();
        switch (indexes[0]) {
            case 0 -> nodes = current.randomRemoval();
            case 1 -> nodes = current.shawRemoval();
            case 2 -> nodes = current.worstRemoval();
            case 3 -> nodes = current.distanceRadialRuin(15.0);
            case 4 -> nodes = current.timeRadialRuin(15.0);
            case 5 -> nodes = current.distanceRadialRuin(20.0);
            case 6 -> nodes = current.timeRadialRuin(20.0);
            case 7 -> nodes = current.windowRemoval();
            case 8 -> nodes = current.routeRemoval();
        }
        
        // insert nodes
        switch (indexes[1]) {
            case 0 -> current.greedyHeuristic(nodes, false);
            case 1 -> current.greedyHeuristic(nodes, true);
            case 2 -> current.regretHeuristic(2, nodes, false);
            case 3 -> current.regretHeuristic(2, nodes, true);
            case 4 -> current.regretHeuristic(3, nodes, false);
            case 5 -> current.regretHeuristic(3, nodes, true);
            case 6 -> current.regretHeuristic(4, nodes, false);
            case 7 -> current.regretHeuristic(4, nodes, true);
            case 8 -> current.regretHeuristicAll(nodes, false);
            case 9 -> current.regretHeuristicAll(nodes, true);
        }
        
        return indexes;
    }
    
    @Override
    public double getTotalCost() {
        if (status) {
            return Math.round(best.getTotalCost() * 1E2) / 1E2;
        }
        
        return Double.MAX_VALUE;
    }
    
    @Override
    public String getOutput() {
        return output;
    }
    
    @Override
    public double getRunTime() {
        return Math.round(time * 1E2) / 1E2;
    }
    
    @Override
    public int getIterations() {
        return app;
    }
    
    @Override
    public boolean isFeasible() {        
        return status;
    }
    
    @Override
    public void setOutput(boolean set) {
        this.set = set;
    }
    
    // string for table: input string and len (spaces)
    String toTable(String str, int len) {
        
        // add spaces if length < len
        if (str.length() < len) {
            String space = " ";
            str += space.repeat(len - str.length());
        }
        
        return str;
    }
    
    // function for printing: system output and/or output strign
    void print(String str) {
        if (set) {
            System.out.print(str);
        }
        output += str;
    }
    
    // this is a line for the output 
    String algorithmLine() {
        return "*".repeat(80);
    }
    
    void printSolution() {
        
        print("feasible solution: " + status + "\n");
        print("total cost: " + Math.round(bestValue * 1E2) / 1E2 + "\n");
        print("total time: " + Double.toString(time) + "\n\n");
        if (status) {
            for (List<Integer> route : best.getRoutes()) {
                print(route + "\n");
            }
        }
    }
    
    @Override
    public List<List<Integer>> getBestRoutes() {
        return new ArrayList<>(best.getRoutes());
    }
    
    @Override
    public List<List<Double>> getBestTimes() {
        return new ArrayList<>(best.getRouteTimes());
    }
    
    @Override
    public String algorithmName() {
        return "AdaptiveLargeNeighborhoodSearch";
    }
    
    @Override
    public Table<Integer, Integer, Double> getTransitions() {
        Table<Integer, Integer, Double> matrix = HashBasedTable.create();
        
        // removal
        for (int i = 0; i < nRemoval; i++) {
            matrix.put(0, i, Math.round(removal.get(i) * 1E10) / 1E10);
        }
        
        // insertion
        for (int i = 0; i < nInsertion; i++) {
            matrix.put(1, i, Math.round(insertion.get(i) * 1E10) / 1E10);
        }
        
        return matrix;
    }
    
    @Override
    public Map<Integer, Double> getApply() {
        return null;
    }
}
