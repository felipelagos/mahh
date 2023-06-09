
package multiarmedhyper.algorithm;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import multiarmedhyper.data.ParameterReader;

public class KheiriHyper extends MultiArmedHyperHeuristic {
    
    // matrixes
    Table<Integer, Integer, Integer> mP;
    Table<Integer, Integer, Integer> mQ;
    
    public KheiriHyper(ParameterReader param) {
        super(param);
        
        // delta
        delta = 0.01;
    }
    
    @Override
    public String algorithmName() {
        return "KheiriHyper[" + delta + "]";
    }
    
    @Override
    int nextLowLevel(int last) {
        return sampleValue(last, mP);
    }
    
    @Override
    int apply(int next) {
        return sampleValue(next, mQ);
    }
    
    @Override
    void algorithmStart() {
        
        // matrixes 
        mP = HashBasedTable.create();
        mQ = HashBasedTable.create();
        
        // initial values
        for (int i = 0; i < nLowLevel; i++) {
            
            // application matrix
            mQ.put(i, 0, 1);
            mQ.put(i, 1, 1);
            
            // creation matrix
            for (int j = 0; j < nLowLevel; j++) {
                mP.put(i, j, 1);
            }
        }
    }
    
    @Override
    void updateTmp(List<Integer> sequence) {}
    
    @Override
    void updateSuccess(List<Integer> sequence) {
        
        // update suc parameters
        for (int i = 1; i < sequence.size(); i++) {

            int index1 = sequence.get(i - 1);
            int index2 = sequence.get(i);
            mP.put(index1, index2, mP.get(index1, index2) + 1);
            mQ.put(index1, 0, mQ.get(index1, 0) + 1);
        }
        int index = sequence.get(sequence.size() - 1);
        mQ.put(index, 1, mQ.get(index, 1) + 1);
    }
    
    private int sampleValue(int last, Table<Integer, Integer, Integer> matrix) {
        
        // sample uniform
        double unif = rand.nextDouble();
        Map<Integer, Integer> values = matrix.row(last);
        
        // compute total
        int total = 0;
        for (Integer val : values.keySet()) {
            total += values.get(val);
        }
        
        // find value
        double acum = 0.0;
        for (Integer val : values.keySet()) {
            acum += values.get(val);
            
            // check unif
            if (acum / total > unif) {
                return val;
            }
        }
        
        return -1;
    }
    
    @Override
    public Table<Integer, Integer, Double> getTransitions() {
        Table<Integer, Integer, Double> matrix = HashBasedTable.create();
        
        for (int i = 0; i < nLowLevel; i++) {
            Map<Integer, Integer> values = mP.row(i);
        
            // compute total
            double total = 0.0;
            for (Integer val : values.keySet()) {
                total += values.get(val);
            }
            
            // add probabilities
            for (Integer val : values.keySet()) {
                matrix.put(i, val, values.get(val) / total);
            }
        }
        
        return matrix;
    }
    
    @Override
    public Map<Integer, Double> getApply() {
        Map<Integer, Double> matrix = new HashMap<>();
        
        for (int i = 0; i < nLowLevel; i++) {
            Map<Integer, Integer> values = mQ.row(i);
        
            // compute total
            double total = 0.0;
            for (Integer val : values.keySet()) {
                total += values.get(val);
            }
            
            // add probabilities
            matrix.put(i, values.get(1) / total);
        }
        
        return matrix;
    }
}