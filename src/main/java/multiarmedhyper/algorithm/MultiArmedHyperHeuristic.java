
package multiarmedhyper.algorithm;

import com.google.common.collect.Table;
import java.util.ArrayList;
import multiarmedhyper.data.ParameterReader;
import java.util.List;
import java.util.Map;
import java.util.Random;

public abstract class MultiArmedHyperHeuristic implements Algorithm {
    
    // parameters
    final ParameterReader param;
    boolean set = true;
    double timeLimit = 90.0;
    double delta = 0.01;
    int iterLimit = (int) timeLimit;
    int lenSeqLimit = 25;
    
    // solution
    Solution best;
    boolean status;
    double bestValue;
    int nLowLevel;
    
    // running 
    double time;
    String output = "";
    int app;
    Random rand;
    
    public MultiArmedHyperHeuristic(ParameterReader param) {
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
    
    void applySequence(Solution current, List<Integer> sequence) {
        
        for (int i = 0; i < sequence.size(); i++) {
            current.applyLowLevel(sequence.get(i));
        }
    }
    
    @Override
    public void solve() {
        
        // best solution
        best = new Solution(param);
        best.setRandom(rand);
        nLowLevel = best.nLowLevel();
        
        // print basic info
        print(param.info() + "\n\n");
        print(algorithmLine() + "\n");
        print("running " + algorithmName() + " \n");
        print("number of low-level heuristics: " + nLowLevel + "\n");
        print(algorithmLine() + "\n\n");
        
        // start with out-and-back routes
        best.savingsMethod();
        bestValue = best.getTotalCost();
        Solution current = new Solution(best);
        double currentCost = current.getTotalCost();
        
        // matrixes 
        algorithmStart();
        
        // empty list and initial llh
        List<Integer> sequence = new ArrayList<>();
        int last = nextLowLevel(0);
        
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
            
            // append next low level heuristic
            int next = nextLowLevel(last);
            sequence.add(next);
            
            // sample u
            int apply = apply(next);
            if (apply == 1 || sequence.size() >= lenSeqLimit) {
                
                // a sequence is applied
                app++;
                
                // tmp and current solutions
                Solution tmp = new Solution(current);
                applySequence(tmp, sequence);
                double tmpCost = tmp.getTotalCost();
                
                // if tmp cost is less than current, then consider it success
                if (tmpCost < currentCost) {
                    updateTmp(sequence);
                }
                
                // if tmp solution is accepted, then update current solution
                if (acceptSolution(tmpCost)) {
                    current = new Solution(tmp);
                    currentCost = current.getTotalCost();
                }
                
                // check if new best solution is found
                if (tmpCost < bestValue) {
                    best = new Solution(tmp);
                    bestValue = tmpCost;
                    
                    // update success parameters
                    updateSuccess(sequence);
                }
                
                time = (System.currentTimeMillis() - starting) / 1E3;
                print(toTable(Double.toString(time), 12) + "|" 
                        + toTable(Integer.toString(app), 10) 
                        + "|" + toTable(Double.toString(Math.round(bestValue * 1E2) / 1E2), 18) 
                        + "|" + toTable(Double.toString(Math.round(currentCost * 1E2) / 1E2), 18) 
                        + "|" + toTable(Double.toString(Math.round(tmpCost * 1E2) / 1E2), 18) 
                        + "|" + toTable(sequence.toString(), 20) + "\n");
                
                // new list
                sequence = new ArrayList<>();
                next = rand.nextInt(nLowLevel);
            }
            
            // update last 
            last = next;
            
            // update time
            time = (System.currentTimeMillis() - starting) / 1E3;
        }
        
        status = best.isFeasible();
        
        // print solution
        print("-".repeat(92) + "\n\n");
        printSolution();
    }
    
    @Override
    public void setDelta(double delta) {
        this.delta = delta;
    }
    
    boolean acceptSolution(double tmpCost) {
        double rho = !best.isFeasible() ? 1E-3 
                : 1E-5 + delta * (1.0 - time * 1.0 / timeLimit);
        return (tmpCost <  (1 + rho) * bestValue);
    }
    
    abstract int nextLowLevel(int last);
    
    abstract int apply(int next);
    
    abstract void algorithmStart();
    
    void updateSuccess(List<Integer> sequence) {}
    
    abstract void updateTmp(List<Integer> sequence);
    
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
    public abstract String algorithmName();
    
    @Override
    public abstract Table<Integer, Integer, Double> getTransitions();
    
    @Override
    public abstract Map<Integer, Double> getApply();
}