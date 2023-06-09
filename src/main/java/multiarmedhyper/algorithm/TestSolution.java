
package multiarmedhyper.algorithm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import multiarmedhyper.data.ParameterReader;

public class TestSolution extends Solution {
    
    final private int nLowLevel = 31;
    
    TestSolution(ParameterReader param) {
        super(param);
    }
    
    TestSolution(Solution tmp) {
        this(tmp.param);
        rand = tmp.rand;
        copySolution(tmp);
    }
    
    void destroyAllRoutes() {
        
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).size() > 3) {
                destroyRoute(i);
            }
        }
        updateSolution();
    }
    
    void destroySingle() {
        List<Integer> indexes = getRandomIndexes();
        
        for (int i = 0; i < indexes.size(); i++) {
            int index = indexes.get(i); 
            if (routes.get(index).size() >= 3) {
                destroyRoute(index);
                updateSolution();
                return;
            }
        }
    }
    
    void destroyRoute(int index) {
        
        // get route 
        List<Integer> route = routes.get(index);
        for (int i = 1; i < route.size() - 1; i++) {
            
            // dummy route
            int node = route.get(i);
            List<Integer> tmp = new ArrayList<>();
            tmp.add(depot);
            tmp.add(node);
            tmp.add(depot);
            routes.add(tmp);
        }
        
        // remove this route
        List<Integer> tmp = new ArrayList<>();
        tmp.add(depot);
        tmp.add(depot);
        routes.set(index, tmp);
    }
    
    @Override
    int nLowLevel() {
        return nLowLevel;
    }
    
    @Override
    void applyLowLevel(int index) {
        
        switch (index) {
            case 0 -> searchShift();
            case 1 -> searchInterchange();
            case 2 -> searchOpt2();
            case 3 -> crossExchange(7);
            case 4 -> search2OptInter();
            case 5 -> pathRelocation();
            case 6 -> orOpt();
            case 7 -> {
                Set<Integer> list = timeRadialRuin(15.0);
                regretHeuristic(2, list, false);
            }
            case 8 -> {
                Set<Integer> list = distanceRadialRuin(15.0);
                regretHeuristic(2, list, false);
            }
            case 9 -> {
                Set<Integer> list = timeRadialRuin(20.0);
                regretHeuristic(3, list, false);
            }
            case 10 -> {
                Set<Integer> list = distanceRadialRuin(20.0);
                regretHeuristic(3, list, false);
            }
            case 11 -> {
                Set<Integer> list = shawRemoval();
                regretHeuristic(3, list, false);
            }
            case 12 -> {
                Set<Integer> list = shawRemoval();
                regretHeuristic(3, list, true);
            }
            case 13 -> {
                Set<Integer> list = shawRemoval();
                regretHeuristic(4, list, false);
            }
            case 14 -> {
                Set<Integer> list = shawRemoval();
                regretHeuristic(4, list, true);
            }
            case 15 -> {
                Set<Integer> list = shawRemoval();
                regretHeuristicAll(list, false);
            }
            case 16 -> {
                Set<Integer> list = shawRemoval();
                greedyHeuristic(list, false);
            }
            case 17 -> {
                Set<Integer> list = randomRemoval();
                regretHeuristic(4, list, false);
            }
            case 18 -> {
                Set<Integer> list = randomRemoval();
                regretHeuristic(4, list, true);
            }
            case 19 -> {
                Set<Integer> list = randomRemoval();
                greedyHeuristic(list, false);
            }
            case 20 -> {
                Set<Integer> list = randomRemoval();
                regretHeuristicAll(list, false);
            }
            case 21 -> {
                Set<Integer> list = randomRemoval();
                regretHeuristicAll(list, true);
            }
            case 22 -> {
                Set<Integer> list = worstRemoval();
                regretHeuristic(4, list, false);
            }
            case 23 -> {
                Set<Integer> list = worstRemoval();
                regretHeuristic(4, list, true);
            }
            case 24 -> {
                Set<Integer> list = windowRemoval();
                regretHeuristic(3, list, false);
            }
            case 25 -> {
                Set<Integer> list = routeRemoval();
                regretHeuristicAll(list, true);
            }
            case 26 -> {
                Set<Integer> list = routeRemoval();
                regretHeuristicAll(list, false);
            }
            
            // dummy low-levels 
            case 27 -> {}
            case 28 -> {
                destroySingle();
            }
            case 29 -> {
                destroyAllRoutes();
            }
            case 30 -> {
                searchShift();
            }
            default -> {
            }
        }
    }
}
