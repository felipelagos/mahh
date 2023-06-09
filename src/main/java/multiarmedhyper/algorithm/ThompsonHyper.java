
package multiarmedhyper.algorithm;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import multiarmedhyper.data.ParameterReader;
import org.apache.commons.math3.distribution.BetaDistribution;

public class ThompsonHyper extends MultiArmedHyperHeuristic {
    
    // matrixes and maps
    Table<Integer, Integer, Integer> mN;
    Table<Integer, Integer, Integer> mP;
    Map<Integer, Integer> mM;
    Map<Integer, Integer> mQ;
    
    public ThompsonHyper(ParameterReader param) {
        super(param);
    }
    
    @Override
    public String algorithmName() {
        return "ThompsonHyper[" + delta + "]";
    }
    
    @Override
    int nextLowLevel(int last) {
        
        int next = -1;
        double max = -1;
        for (int i = 0; i < nLowLevel; i++) {

            // sample prob
            double theta = sampleBeta(mP.get(last, i), mN.get(last, i));
            if (theta > max) {
                max = theta;
                next = i;
            }
        }
        
        return next;
    }
    
    @Override
    int apply(int next) {
        
        double theta0 = sampleBeta(mM.get(next) - mQ.get(next), mM.get(next));
        double theta1 = sampleBeta(mQ.get(next), mM.get(next));
        return (theta0 < theta1) ? 1 : 0;
    }
    
    @Override
    void algorithmStart() {
        
        // matrixes sequence creation
        mN = HashBasedTable.create();
        mP = HashBasedTable.create();
        
        // maps for sequences application
        mM = new HashMap<>();
        mQ = new HashMap<>();
        
        // initial values
        for (int i = 0; i < nLowLevel; i++) {
            
            // application matrixes
            mM.put(i, 2);
            mQ.put(i, 1);
            
            // creation matrixes
            for (int j = 0; j < nLowLevel; j++) {
                mN.put(i, j, 2);
                mP.put(i, j, 1);
                mN.put(i, j, 2);
                mP.put(i, j, 1);
            }
        }
    }
    
    @Override
    void updateTmp(List<Integer> sequence) {
        
        // update totals
        for (int i = 0; i < sequence.size(); i++) {

            int index = sequence.get(i);
            for (int j = 0; j < nLowLevel; j++) {
                mN.put(index, j, mN.get(index, j) + 1);
            }
            mM.put(index, mM.get(index) + 1);
        }

        // update suc parameters
        for (int i = 1; i < sequence.size(); i++) {

            int index1 = sequence.get(i - 1);
            int index2 = sequence.get(i);
            mP.put(index1, index2, mP.get(index1, index2) + 1);
        }
        int index = sequence.get(sequence.size() - 1);
        mQ.put(index, mQ.get(index) + 1);
    }
    
    double sampleBeta(double n1, double n2) {
        double number = n2 - n1 < 1 ? 1 : n2 - n1;
        BetaDistribution beta = new BetaDistribution(n1, number);
        return beta.inverseCumulativeProbability(rand.nextDouble());
    }
    
    @Override
    public Table<Integer, Integer, Double> getTransitions() {
        Table<Integer, Integer, Double> matrix = HashBasedTable.create();
        
        for (int i = 0; i < nLowLevel; i++) {
            
            // vector with probabilities
            Double[] vector = new Double[nLowLevel];
            for (int k = 0; k < vector.length; k++) {
                vector[k] = 0.0;
            }
            
            // simulate
            for (int k = 0; k < 1000; k++) {
                int next = -1;
                double max = -1;
                for (int j = 0; j < nLowLevel; j++) {

                    // sample prob
                    double theta = sampleBeta(mP.get(i, j), mN.get(i, j));
                    if (theta > max) {
                        max = theta;
                        next = j;
                    }
                }
                
                // update vector
                vector[next] = vector[next] + 1;
            }
            
            // add values
            for (int j = 0; j < vector.length; j++) {
                matrix.put(i, j, vector[j] / 1000.0);
            }
        }
        
        return matrix;
    }
    
    @Override
    public Map<Integer, Double> getApply() {
        Map<Integer, Double> matrix = new HashMap<>();
        
        for (int i = 0; i < nLowLevel; i++) {
            
            // simulate 
            double prob = 0;
            for (int j = 0; j < 1000; j++) {
                double theta0 = sampleBeta(mM.get(i) - mQ.get(i), mM.get(i));
                double theta1 = sampleBeta(mQ.get(i), mM.get(i));
                double value = (theta0 < theta1) ? 1 : 0;
                prob += value;
            }
            prob = prob / 1000;
            matrix.put(i, prob);
        }
        
        return matrix;
    }
}