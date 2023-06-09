
package multiarmedhyper.algorithm;

import java.util.ArrayList;
import java.util.List;
import multiarmedhyper.data.ParameterReader;

public class TestKheiriHyper extends KheiriHyperAll {
    
    public TestKheiriHyper(ParameterReader param) {
        super(param);
    }
    
    @Override
    public void solve() {
        
        // best solution
        best = new TestSolution(param);
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
        TestSolution current = new TestSolution(best);
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
            if (apply == 1) {
                
                // a sequence is applied
                app++;
                
                // tmp and current solutions
                TestSolution tmp = new TestSolution(current);
                applySequence(tmp, sequence);
                double tmpCost = tmp.getTotalCost();
                
                // if tmp cost is less than current, then consider it success
                if (tmpCost < currentCost) {
                    updateTmp(sequence);
                }
                
                // if tmp solution is accepted, then update current solution
                if (acceptSolution(tmpCost)) {
                    current = new TestSolution(tmp);
                    currentCost = current.getTotalCost();
                }
                
                // check if new best solution is found
                if (tmpCost < bestValue) {
                    best = new TestSolution(tmp);
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
    public String algorithmName() {
        return "TestKheiriHyper[" + delta + "]";
    }
}
