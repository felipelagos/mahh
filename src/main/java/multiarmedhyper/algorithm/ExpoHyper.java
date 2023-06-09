
package multiarmedhyper.algorithm;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import multiarmedhyper.data.ParameterReader;

public class ExpoHyper extends MultiArmedHyperHeuristic {
    
    // matrixes
    Table<Integer, Integer, Double> mP;
    Table<Integer, Integer, Double> mQ;
    
    // learning rate
    double eta = 0.5;
    double alpha = 0.01;
    double beta = 0.1;
    
    public ExpoHyper(ParameterReader param, double eta) {
        super(param);
        this.eta = eta;
    }
    
    public ExpoHyper(ParameterReader param) {
        this(param, 0.5);
    }
    
    public void setEta(double eta) {
        this.eta = eta;
    }
    
    public void setAlpha(double alpha) {
        this.alpha = alpha;
    }
    
    public void setBeta(double beta) {
        this.beta = beta;
    }
    
    @Override
    public String algorithmName() {
        return "ExpoHyper[" + eta + "," + delta + "]";
    }
    
    @Override
    int nextLowLevel(int last) {
        
        // find value
        double unif = rand.nextDouble();
        Map<Integer, Double> prob = getProbability(last);
        Double acum = 0.0;
        int next = -1; 
        for (Integer val : prob.keySet()) {
            acum += prob.get(val);
            
            // check unif
            if (acum > unif) {
                return val;
            }
            next = val;
        }
        
        return next;
    }
    
    @Override
    int apply(int next) {
        
        // unif
        double unif = rand.nextDouble();
        double prob = getAccept(next);
        
        return (unif < prob) ? 1 : 0;
    }
    
    @Override
    void algorithmStart() {
        
        // matrixes 
        mP = HashBasedTable.create();
        mQ = HashBasedTable.create();
        
        // initial values
        for (int i = 0; i < nLowLevel; i++) {
            
            // application matrix
            mQ.put(i, 0, 0.0);
            mQ.put(i, 1, 0.0);
            
            // creation matrix
            for (int j = 0; j < nLowLevel; j++) {
                mP.put(i, j, 0.0);
            }
        }
    }
    
    @Override
    void updateTmp(List<Integer> sequence) {
        
        // update suc parameters
        for (int i = 1; i < sequence.size(); i++) {

            int index1 = sequence.get(i - 1);
            int index2 = sequence.get(i);
            double value = getProbability(index1).get(index2);
            mP.put(index1, index2, 
                    mP.get(index1, index2) - 1.0 / value);
            mQ.put(index1, 0, mQ.get(index1, 0) - 1.0 / (1.0 - getAccept(index1)));
        }
        int index = sequence.get(sequence.size() - 1);
        mQ.put(index, 1, mQ.get(index, 1) - 1.0 / getAccept(index));
    }
    
    Map<Integer, Double> getProbability(int index) {
        
        // compute all probabilities
        Map<Integer, Double> map = new HashMap<>();
        Double total = 0.0;
        for (int i = 0; i < nLowLevel; i++) {
            Double value = Math.exp(-1.0 * eta * mP.get(index, i));
            
            // check math limits
            value = (value > Double.MAX_VALUE / nLowLevel) 
                    ? Double.MAX_VALUE / nLowLevel : value; 
            map.put(i, value);
            total += value;
        }
        
        // check values 
        List<Integer> below = new ArrayList<>();
        double div = total;
        for (int i = 0; i < nLowLevel; i++) {
            Double value = map.get(i);
            
            // check limit
            if (map.get(i) / div < alpha) {
                total = total - value;
                below.add(i);
            }
        }
        
        // divide by total 
        Double cor = (1 - alpha * below.size());
        for (int i = 0; i < nLowLevel; i++) {
            if (below.contains(i)) {
                map.put(i, alpha);
            } else {
                map.put(i, map.get(i) / total * cor);
            }
            
        }
        
        return map;
    }
    
    Double getAccept(int index) {
        
        Double value1 = Math.exp(-1.0 * eta * mQ.get(index, 0));
        value1 = (value1 > Double.MAX_VALUE / nLowLevel) 
                    ? Double.MAX_VALUE / nLowLevel : value1;
        Double value2 = Math.exp(-1.0 * eta * mQ.get(index, 1));
        value2 = (value2 > Double.MAX_VALUE / nLowLevel) 
                    ? Double.MAX_VALUE / nLowLevel : value2;
        Double total = value1 + value2;
        Double prob = value2 / total;
        
        // check limits
        if (prob < beta) {
            return beta;
        }
        if (prob > 1 - beta) {
            return 1 - beta;
        }
        
        return prob;
    }
    
    @Override
    public Table<Integer, Integer, Double> getTransitions() {
        Table<Integer, Integer, Double> matrix = HashBasedTable.create();
        
        for (int i = 0; i < nLowLevel; i++) {
            Map<Integer, Double> map = getProbability(i);
            for (Integer j : map.keySet()) {
                matrix.put(i, j, map.get(j));
            }
        }
        
        return matrix;
    }
    
    @Override
    public Map<Integer, Double> getApply() {
        Map<Integer, Double> matrix = new HashMap<>();
        
        for (int i = 0; i < nLowLevel; i++) {
            matrix.put(i, getAccept(i));
        }
        
        return matrix;
    }
}