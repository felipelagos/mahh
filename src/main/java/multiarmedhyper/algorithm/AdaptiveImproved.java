
package multiarmedhyper.algorithm;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import multiarmedhyper.data.ParameterReader;

public class AdaptiveImproved extends AdaptiveLargeNeighborhoodSearch {
    
    Map<Integer, Double> improvement;
    int nImprove = 7;
    
    public AdaptiveImproved(ParameterReader param) {
        super(param);
    }
    
    @Override
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
        
        // initial values improvement
        Double total3 = 0.0;
        for (int i = 0; i < nImprove; i++) {
            double value = improvement.get(i);
            if (totals.get("P" + i) > 0) {
                value = scores.get("P" + i) / totals.get("P" + i);
            }
            double weight = (1 - r) * improvement.get(i) + r * value;
            improvement.put(i, weight);
            total3 += weight;
        }
        for (int i = 0; i < nImprove; i++) {
            improvement.put(i, improvement.get(i) / total3);
        }
    }
    
    @Override
    void updateScores(Integer[] indexes, int type,
            Map<String, Integer> totals, Map<String, Double> scores) {
        
        // mapping names
        String rem = "R" + indexes[0];
        String ins = "I" + indexes[1];
        String imp = "P" + indexes[2];

        // update totals
        totals.put(rem, totals.get(rem) + 1);
        totals.put(ins, totals.get(ins) + 1);
        totals.put(imp, totals.get(imp) + 1);
        
        // update scores
        if (type > -1) {
            scores.put(rem, scores.get(rem) + sigma[type]);
            scores.put(ins, scores.get(ins) + sigma[type]);
            scores.put(imp, scores.get(imp) + sigma[type]);
        }
    }
    
    @Override
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
        for (int i = 0; i < nImprove; i++) {
            totals.put("P" + i, 0);
            scores.put("P" + i, 0.0);
        }
    }
    
    @Override
    void initWeights() {
        
        // matrixes 
        removal = new HashMap<>();
        insertion = new HashMap<>();
        improvement = new HashMap<>();
        
        // initial values removal
        for (int i = 0; i < nRemoval; i++) {
            removal.put(i, 1.0 / nRemoval);
        }
        
        // initial values insertion
        for (int i = 0; i < nInsertion; i++) {
            insertion.put(i, 1.0 / nInsertion);
        }
        
        // initial values improvement
        for (int i = 0; i < nImprove; i++) {
            improvement.put(i, 1.0 / nImprove);
        }
    }
    
    @Override
    Integer[] applyHeuristics(Solution current) {
        
        // indexes
        Integer[] indexes = new Integer[3];
        
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
        
        // find improvement
        double unif3 = rand.nextDouble();
        double ac3 = 0;
        for (int i = 0; i < nImprove; i++) {
            ac3 += improvement.get(i);
            if (unif3 < ac3) {
                indexes[2] = i;
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
        
        // improve solution
        switch (indexes[2]) {
            case 0 -> current.searchShift();
            case 1 -> current.searchInterchange();
            case 2 -> current.searchOpt2();
            case 3 -> current.crossExchange(7);
            case 4 -> current.search2OptInter();
            case 5 -> current.pathRelocation();
            case 6 -> current.orOpt();
        }
        
        return indexes;
    }
    
    @Override
    public String algorithmName() {
        return "AdaptiveImproved";
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
        
        // improve
        for (int i = 0; i < nImprove; i++) {
            matrix.put(2, i, Math.round(improvement.get(i) * 1E10) / 1E10);
        }
        
        return matrix;
    }
}
