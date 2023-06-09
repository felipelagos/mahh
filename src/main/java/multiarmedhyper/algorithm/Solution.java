
package multiarmedhyper.algorithm;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import multiarmedhyper.data.ParameterReader;

public class Solution {
    
    // solution routes
    final List<List<Integer>> routes;
    
    // consequences
    private List<List<Double[]>> routesTimes;
    private List<Double> routesLoads;
    private List<Double> routesCosts;
    private double totalCost;
    private boolean feasible;
    private List<Boolean> feasibleRoutes;
    
    // global values
    final private double constFeas;
    final private int nLowLevel = 27;
    final private int maxSetSize = 40;
    Random rand;
    
    // parameters
    final ParameterReader param;
    final int depot;
    final private Table<Integer, Integer, Double> times;
    final private Map<Integer, Double[]> windows;
    final private Map<Integer, Double> service;
    final private Map<Integer, Double> demand;
    
    // utils
    private Map<Integer, LinkedHashMap<Integer, Double>> savings;
    
    // constructor with parameter
    Solution(ParameterReader param) {
        this.param = param;
        this.depot = param.getDepot();
        
        // get parameters
        times = param.getTime();
        windows = param.getTimeWindows();
        service = param.getService();
        demand = param.getDemand();
        
        // for each vehicle add a dummy route
        routes = new ArrayList<>();
        for (int i = 0; i < param.getNVehicles(); i++) {
            
            // dummy route
            List<Integer> route = new ArrayList<>();
            route.add(depot);
            route.add(depot);
            routes.add(route);
        }
        
        // compute constant
        double total = 1;
        Set<Integer> nodes = param.getNodes();
        nodes.remove(depot);
        for (Integer node : nodes) {
            total += 2 * times.get(depot, node);
        }
        
        // mult by size
        constFeas = total * (nodes.size() + 1);
        
        // starting values 
        totalCost = constFeas;
        feasible = false;
        
        // lists
        feasibleRoutes = new ArrayList<>();
        routesTimes = new ArrayList<>();
        routesLoads = new ArrayList<>();
        routesCosts = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            routesCosts.add(constFeas);
            routesLoads.add(0.0);
            feasibleRoutes.add(false);
            routesTimes.add(null);
        }
    }
    
    Solution(Solution tmp) {
        this(tmp.param);
        rand = tmp.rand;
        copySolution(tmp);
    }
    
    public void setRandom(Random rand) {
        this.rand = rand;
    }
    
    final void copySolution(Solution tmp) {
        
        // copy routes
        routes.clear();
        for (int i = 0; i < tmp.routes.size(); i++) {
            routes.add(new ArrayList<>(tmp.routes.get(i)));
        }
        
        // copy solution 
        totalCost = tmp.totalCost;
        feasible = tmp.feasible;
        
        // copy lists
        feasibleRoutes.clear();
        routesTimes.clear();
        routesLoads.clear();
        routesCosts.clear();
        for (int i = 0; i < tmp.routes.size(); i++) {
            routesCosts.add(tmp.routesCosts.get(i));
            routesLoads.add(tmp.routesLoads.get(i));
            feasibleRoutes.add(tmp.feasibleRoutes.get(i));
            if (tmp.routesTimes.get(i) != null) {
                routesTimes.add(new ArrayList<>(tmp.routesTimes.get(i)));
            } else {
                routesTimes.add(null);
            }
        }
    }
    
    void updateSolution() {
        
        // remove empty route
        for (int i = routes.size() - 1; i >= 0; i--) {
            if (routes.get(i).size() < 3) {
                routes.remove(i);
            }
        }
        
        // add additional one empty route if there are vehicles available
        if (param.getNVehicles() - routes.size() > 0) {
            List<Integer> tmp = new ArrayList<>();
            tmp.add(depot);
            tmp.add(depot);
            routes.add(tmp);
        }
        
        // starting values 
        totalCost = 0.0;
        feasible = true;
        
        // lists
        feasibleRoutes = new ArrayList<>();
        routesTimes = new ArrayList<>();
        routesLoads = new ArrayList<>();
        routesCosts = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            routesCosts.add(0.0);
            routesLoads.add(0.0);
            feasibleRoutes.add(true);
            routesTimes.add(null);
        }
        
        // update routes
        for (int i = 0; i < routes.size(); i++) {
            updateRoute(i);
        }
        
        // check number of vehicles
        if (param.getNVehicles() < routes.size()) {
            feasible = false;
            totalCost += constFeas * (routes.size() - param.getNVehicles());
        }
        
        // check all routes
        for (int i = 0; i < routes.size(); i++) {
            totalCost += routesCosts.get(i);
            if (!feasibleRoutes.get(i)) {
                feasible = false;
                totalCost += constFeas;
            }
        }
        
        // nodes must be visited exactly once 
        Set<Integer> nodes = param.getNodes();
        nodes.remove(depot);
        for (int i = 0; i < routes.size(); i++) {
            List<Integer> route = routes.get(i);
            for (int j = 1; j < route.size() - 1; j++) {
                int node = route.get(j);
                if (!nodes.contains(node)) {
                    feasible = false;
                    totalCost += constFeas;
                }
                nodes.remove(node);
            }
        }
        
        if (!nodes.isEmpty()) {
            feasible = false;
            totalCost += constFeas * nodes.size();
        }
    }
    
    private void updateRoute(int index) {
        
        // route
        List<Integer> route = routes.get(index);
        
        // total cost
        double cost = 0;

        int last = route.get(0);
        double time = service.get(last);
        double quantity = 0;
        List<Double[]> locTimes = new ArrayList<>();
        locTimes.add(new Double[] {time, time, time});
        for (int i = 1; i < route.size(); i++) {
            
            // times
            Double[] vector = new Double[3];
            
            // get node 
            int node = route.get(i);
            cost += times.get(last, node);
            
            // consider the arrival time to check time window
            time += times.get(last, node);
            vector[0] = time;
            if (windows.get(node)[1] < time) {
                cost += constFeas;
            }
            
            // wait if too early
            if (windows.get(node)[0] > time) {
                time = windows.get(node)[0];
            }
            vector[1] = time;
            
            // check quantity
            quantity += demand.get(node);
            if (quantity > param.getCapacity()) {
                cost += constFeas;
            }
            
            // finally sum service time
            time += service.get(node);
            vector[2] = time;
            locTimes.add(vector);
            last = node;
        }
        
        // keep values
        if (routesCosts.size() > index) {
            routesCosts.set(index, cost);
            routesLoads.set(index, quantity);
            feasibleRoutes.set(index, cost < constFeas);
            routesTimes.set(index, locTimes);
        } else {
            routesCosts.add(index, cost);
            routesLoads.add(index, quantity);
            feasibleRoutes.add(index, cost < constFeas);
            routesTimes.add(index, locTimes);
        }
        
    }
    
    // check whether the route in index is feasible or not
    private boolean feasible(List<Integer> route) {
        
        int last = route.get(0);
        double time = service.get(last);
        double quantity = 0;
        for (int i = 1; i < route.size(); i++) {
            
            // get node 
            int node = route.get(i);
            
            // consider the arrival time to check time window
            time += times.get(last, node);
            if (windows.get(node)[1] < time) {
                return false;
            }
            
            // wait if too early
            if (windows.get(node)[0] > time) {
                time = windows.get(node)[0];
            }
            
            // check quantity
            quantity += demand.get(node);
            if (quantity > param.getCapacity()) {
                return false;
            }
            
            // finally sum service time
            time += service.get(node);
            last = node;
        }
        
        return true;
    }
    
    // return total cost of the solution
    double getTotalCost() {
        return totalCost;
    }
    
    List<List<Integer>> getRoutes() {
        List<List<Integer>> tmp = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            List<Integer> route = routes.get(i);
            if (route.size() > 2) {
                tmp.add(route);
            }
        }
        
        return tmp;
    }
    
    Integer hashValue() {
        return this.routes.hashCode();
    }
    
    // returns whether the solution is feasible or not
    boolean isFeasible() {
        return feasible;
    }
    
    List<List<Double>> getRouteTimes() {
        
        // compute arrival times
        List<List<Double>> arrival = new ArrayList<>();
        for (int i = 0; i < routesTimes.size(); i++) {
            
            // check size
            List<Double[]> vector = routesTimes.get(i);
            if (vector.size() < 3) {
                continue;
            }
            
            // add times
            List<Double> tmp = new ArrayList<>();
            for (int j = 0; j < vector.size(); j++) {
                tmp.add(vector.get(j)[0]);
            }
            
            // include info
            arrival.add(tmp);
        }
        
        return arrival;
    }
    
    private void outAndBackRoutes() {
        
        // initialize routes out-and-back
        routes.clear();
        Set<Integer> nodes = param.getNodes();
        nodes.remove(depot);
        for (Integer node : nodes) {
            List<Integer> route = new ArrayList<>();
            route.add(depot);
            route.add(node);
            route.add(depot);
            routes.add(route);
        }
        
        updateSolution();
    }
    
    void savingsMethod() {
        
        // initialize routes out-and-back, then merge
        outAndBackRoutes();
        mergeRoutes();
        updateSolution();
    }
    
    private void mergeRoutes() {
        
        // create savings
        computeSavings();
        
        // keep merging
        boolean improv = true;
        while (improv) {
            improv = false;
            
            // map with candidates
            Map<Integer, Integer> check = new HashMap<>();
            for (int i = 0; i < routes.size(); i++) {

                // route
                List<Integer> route = routes.get(i);
                if (route.size() < 3) {
                    continue;
                }

                check.put(route.get(1), i);
            }
            
            // check route in random order 
            List<Integer> indexes = IntStream.rangeClosed(0, routes.size() - 1)
                    .boxed().collect(Collectors.toList());
            Collections.shuffle(indexes, new Random(rand.nextLong()));

            for (int index1 : indexes) {

                // route
                List<Integer> route = routes.get(index1);
                if (route.size() < 3) {
                    continue;
                }

                // check last node
                int node = route.get(route.size() - 2);
                List<Integer> options = new ArrayList<>(savings.get(node).keySet());
                for (Integer con : options) {
                    savings.get(node).remove(con);
                    if (!check.containsKey(con) || check.get(con) == index1) {
                        continue;
                    }
                    
                    // check load
                    int index2 = check.get(con);
                    double load1 = routesLoads.get(index1);
                    double load2 = routesLoads.get(index2);
                    if (load1 + load2 > param.getCapacity()) {
                        continue;
                    }
                    
                    // check time
                    List<Integer> route1 = routes.get(index1);
                    List<Integer> route2 = routes.get(index2);
                    double time = routesTimes.get(index1).get(route1.size() - 2)[2];
                    Integer last = node;
                    boolean timeFeasible = true;
                    for (int j = 1; j < route2.size(); j++) {
                        
                        int nextNode = route2.get(j);
                        time += times.get(last, nextNode);
                        if (windows.get(nextNode)[1] < time) {
                            timeFeasible = false;
                            break;
                        }
                        
                        if (windows.get(nextNode)[0] > time) {
                            time = windows.get(nextNode)[0];
                        }
                        time += service.get(nextNode);
                        last = nextNode;
                    }
                    
                    // route infeasible
                    if (!timeFeasible) {
                        continue;
                    }

                    // new route
                    List<Integer> tmp = new ArrayList<>();
                    tmp.addAll(route1.subList(0, route1.size() - 1));
                    tmp.addAll(route2.subList(1, route2.size()));

                    // update solution
                    routes.set(index1, tmp);
                    routes.set(index2, new ArrayList<>());
                    updateRoute(index1);
                    
                    // update lists and continue
                    check.remove(con);
                    if (savings.containsKey(con)) {
                        savings.get(con).remove(node);
                    }
                    improv = true;
                    break;
                }
            }
            updateSolution();
        }
    }
    
    private void computeSavings() {
        
        // create map
        savings = new HashMap<>();
        
        for (int i = 0; i < routes.size(); i++) {
            
            // if empty route continue
            List<Integer> route1 = routes.get(i);
            if (route1.size() < 3) {
                continue;
            }
            
            // create map
            int node1 = route1.get(route1.size() - 2);
            Map<Integer, Double> map = new HashMap<>();
            for (int j = 0; j < routes.size(); j++) {

                // compute savings for different nodes
                List<Integer> route2 = routes.get(j);
                if (i == j || route2.size() < 3) {
                    continue;
                }
                int node2 = route2.get(1);
                double value = times.get(node1, depot) + times.get(depot, node2) 
                        - times.get(node1, node2);
                map.put(j, value);
            }
            
            // sort table
            LinkedHashMap<Integer, Double> sorted = sortMap(map, true);
            savings.put(node1, sorted);
        }
    }
    
    // move one node from index route to another route
    private void searchShiftOther(int index) {
        
        // get route 
        List<Integer> route = routes.get(index);
        
        // check number of nodes
        if (route.size() < 3) {
            return;
        }
        
        // info 
        double capacity = param.getCapacity();
        
        // all nodes
        for (int k = 1; k < route.size() - 1; ) {
            
            // get nodes
            Integer node = route.get(k);
            double nodeLoad = demand.get(node);
            double lastest = windows.get(node)[1];
            Integer n1 = route.get(k - 1);
            Integer n2 = route.get(k + 1);
            
            // check routes in random order
            List<Integer> indexes = getRandomIndexes();
            
            // get list all routes
            boolean inserted = false;
            for (int i = 0; i < indexes.size() && !inserted; i++) {
                
                // check load and size
                int index2 = indexes.get(i);
                double load = routesLoads.get(index2);
                if (index2 == index || load + nodeLoad > capacity) {
                    continue;
                }
                
                // check all positions
                List<Integer> review = new ArrayList<>(routes.get(index2));
                for (int j = 1; j < review.size(); j++) {
                    
                    // nodes 
                    Integer m1 = review.get(j - 1);
                    Integer m2 = review.get(j);
                    
                    // compute diff
                    double org = times.get(n1, node) + times.get(node, n2) 
                            + times.get(m1, m2);
                    double change = times.get(n1, n2) + times.get(m1, node) 
                            + times.get(node, m2);
                    if (org - change <= 0) {
                        continue;
                    }
                    
                    // check time
                    double lastTime = routesTimes.get(index2).get(j - 1)[2];
                    if (lastTime + times.get(m1, node) > lastest) {
                        break;
                    }
                      
                    // new route
                    List<Integer> tmp2 = new ArrayList<>(review);
                    tmp2.add(j, node);
                    if (feasible(tmp2)) {
                        
                        // update route
                        routes.set(index2, tmp2);
                        updateRoute(index2);

                        // remove node
                        List<Integer> tmp1 = new ArrayList<>(route);
                        tmp1.remove(node);
                        routes.set(index, tmp1);
                        updateRoute(index);

                        // update
                        route = new ArrayList<>(routes.get(index));
                        inserted = true;
                        break;
                    }
                }
            }
            
            // in this case increase k 
            if (!inserted) {
                k++;
            }
        }
    }
    
    private void searchShiftSame(int index) {
        
        // get route 
        List<Integer> route = routes.get(index);
        
        // check number of nodes
        if (route.size() < 4) {
            return;
        }
        
        // all nodes
        for (int k = 1; k < route.size() - 1; ) {
            
            // get nodes
            Integer node = route.get(k);
            double lastest = windows.get(node)[1];
            Integer n1 = route.get(k - 1);
            Integer n2 = route.get(k + 1);
            
            int pos = -1;
            
            // check all positions
            List<Integer> review = new ArrayList<>(route);
            review.remove(node);
            for (int j = 1; j < review.size(); j++) {

                // nodes 
                Integer m1 = review.get(j - 1);
                Integer m2 = review.get(j);

                // compute diff
                double org = times.get(n1, node) + times.get(node, n2) 
                        + times.get(m1, m2);
                double change = times.get(n1, n2) + times.get(m1, node) 
                        + times.get(node, m2);
                if (org - change <= 0) {
                    continue;
                }

                // check time
                double lastTime = routesTimes.get(index).get(j - 1)[2];
                if (lastTime + times.get(m1, node) > lastest) {
                    break;
                }

                // new route
                List<Integer> tmp = new ArrayList<>(review);
                tmp.add(j, node);
                if (feasible(tmp)) {
                    
                    // update route
                    routes.set(index, tmp);
                    updateRoute(index);

                    // update
                    route = new ArrayList<>(routes.get(index));
                    pos = j;
                    break;
                }
            }
            
            // in this case increase k 
            if (pos <= k) {
                k++;
            }
        }
    }
    
    void searchShift() {
        
        // check all combinations vehicles/nodes in random order
        List<Integer> indexes = getRandomIndexes();
        for (int i = 0; i < indexes.size(); i++) {
            
            int index = indexes.get(i);
            searchShiftOther(index);
            searchShiftSame(index);
        }
        
        updateSolution();
    }
    
    List<Integer> getRandomIndexes() {
        
        List<Integer> indexes = IntStream.rangeClosed(0, routes.size() - 1)
                .boxed().collect(Collectors.toList());
        Collections.shuffle(indexes, new Random(rand.nextLong()));
        int total = Math.min(indexes.size(), maxSetSize);
        
        return indexes.subList(0, total);
    }
    
    private void crossExchange(int index, int len) {
        
        // get route 
        List<Integer> route1 = routes.get(index);
        boolean feas1 = feasible(route1);
        
        // check number of nodes
        if (route1.size() < 3) {
            return;
        }
        
        // check routes randomly
        List<Integer> indexes = getRandomIndexes();
        for (int k : indexes) {
            if (k == index) {
                continue;
            }
            List<Integer> route2 = new ArrayList<>(routes.get(k));
            boolean feas2 = feasible(route2);
            
            // check combinations
            for (int i1 = 0; i1 < route1.size() - 2; i1++) {
                for (int i2 = 0; i2 < route2.size() - 2; i2++) {
                    for (int j1 = i1; j1 < route1.size() - 1 && j1 < i1 + len; j1++) {
                        for (int j2 = i2; j2 < route2.size() - 1 && j2 < i2 + len; j2++) {
                            
                            // get nodes route1
                            int w1 = route1.get(i1);
                            int x1 = route1.get(i1 + 1);
                            int y1 = route1.get(j1);
                            int z1 = route1.get(j1 + 1);
                            
                            // get nodes route2
                            int w2 = route2.get(i2);
                            int x2 = route2.get(i2 + 1);
                            int y2 = route2.get(j2);
                            int z2 = route2.get(j2 + 1); 
                            
                            // compute gain
                            double org = times.get(w1, x1) + times.get(w2, x2) 
                                    + times.get(y1, z1) + times.get(y2, z2);
                            double change = times.get(w1, x2) + times.get(w2, x1) 
                                    + times.get(y1, z2) + times.get(y2, z1);
                            double diff = org - change;
                            if (diff > 0 || !feas1 || !feas2) {
                                
                                // create routes
                                List<Integer> tmp1 = new ArrayList<>(route1.subList(0, i1 + 1));
                                tmp1.addAll(route2.subList(i2 + 1, j2 + 1));
                                tmp1.addAll(route1.subList(j1 + 1, route1.size()));
                                
                                List<Integer> tmp2 = new ArrayList<>(route2.subList(0, i2 + 1));
                                tmp2.addAll(route1.subList(i1 + 1, j1 + 1));
                                tmp2.addAll(route2.subList(j2 + 1, route2.size()));
                                 
                                if ((!feas1 || feasible(tmp1)) && (!feas2 || feasible(tmp2))) {
                                    routes.set(index, tmp1);
                                    routes.set(k, tmp2);
                                    return;
                                }
                            }
                        }
                    }
                }
            }
        }
        
    }
    
    void crossExchange(int len) {
        
        // check routes randomly
        List<Integer> indexes = getRandomIndexes();
        for (int i = 0; i < indexes.size(); i++) {
            crossExchange(indexes.get(i), len);
        }
        
        updateSolution();
    }
    
    void searchOpt2() {
        
        for (int i = 0; i < routes.size(); i++) {
            searchOpt2(i);
        }
        updateSolution();
    }
    
    // reorder the sequence of certain nodes within route index
    private void searchOpt2(int index) {
        
        // get route 
        List<Integer> route = routes.get(index);
        int len = route.size();
        boolean feas = feasibleRoutes.get(index);
        
        // check number of nodes
        if (len < 3) {
            return;
        }
        
        // iterate while there is improvement
        boolean improv = true;
        while (improv) {
            improv = false;
            
            // check all combinations 0 < i < j < len - 1
            for (int i = 1; i < len - 2; i++) {
                boolean nextNode = false;
                for (int j = i + 1; j < len - 1 && !nextNode; j++) {
                    
                    // compute times
                    double arrival = routesTimes.get(index).get(i)[2];
                    
                    // new route
                    List<Integer> tmp = new ArrayList<>(route.subList(0, i));
                    for (int k = j; k >= i; k--) {
                        tmp.add(route.get(k));
                        
                        // check time window
                        arrival += times.get(route.get(k - 1), route.get(k));
                        if (windows.get(route.get(k))[0] > arrival) {
                            arrival = windows.get(route.get(k))[0];
                        }
                        
                        // break 
                        if (windows.get(route.get(k))[1] < arrival) {
                            nextNode = true;
                            break;
                        }
                    }
                    tmp.addAll(route.subList(j + 1, route.size()));

                    // if better and feasible, keep it
                    double org = times.get(route.get(i - 1), route.get(i)) 
                            + times.get(route.get(j), route.get(j + 1));
                    double change = times.get(route.get(i - 1), route.get(j)) 
                            + times.get(route.get(i), route.get(j + 1));
                    double diff = org - change;
                    if (diff > 0 && !nextNode) {
                        
                        // new solution with changes
                        if (!feas || feasible(tmp)) {
                            
                            // update 
                            routes.set(index, tmp);
                            updateRoute(index);
                            route = new ArrayList<>(tmp);
                            feas = feasibleRoutes.get(index);
                            
                            // keep iterating
                            improv = true;
                        }
                    }
                }
            }
        }
    }
    
    void orOpt() {
        
        // check all vehicles
        for (int i = 0; i < routes.size(); i++) {
            orOpt(i);
        }
        
        updateSolution();
    }
    
    private void orOpt(int index) {
        
        // get route 
        List<Integer> route = routes.get(index);
        
        // check number of nodes
        if (route.size() < 6) {
            return;
        }
        
        // all nodes
        for (int i = 1; i < route.size() - 4; i++) {
            Integer n1 = route.get(i);
            Integer n2 = route.get(i + 1);
            for (int j = i + 1; j < route.size() - 3; j++) {
                Integer n3 = route.get(j);
                Integer n4 = route.get(j + 1);
                for (int k = j + 1; k < route.size() - 2; k++) {
                    Integer n5 = route.get(k);
                    Integer n6 = route.get(k + 1);
                    
                    // compute diff
                    double org = times.get(n1, n2) +  times.get(n3, n4) + times.get(n5, n6);
                    double change = times.get(n1, n4) + times.get(n5, n2) + times.get(n3, n6);
                    if (org - change <= 0) {
                        continue;
                    }
                    
                    // new route
                    List<Integer> tmp = new ArrayList<>(route.subList(0, i + 1));
                    tmp.addAll(route.subList(j + 1, k + 1));
                    tmp.addAll(route.subList(i + 1, j + 1));
                    tmp.addAll(route.subList(k + 1, route.size()));
                    if (feasible(tmp)) {
                        
                        // update route
                        routes.set(index, tmp);
                        updateRoute(index);
                        return;
                    }
                }
            }
        }
    }
    
    // interchange two nodes within two given routes
    private void searchInterchange(int index1, int index2) {
        
        // less than 1 route, return
        if (routes.size() < 2) {
            return;
        }
        
        // info 
        double capacity = param.getCapacity();
        
        // get the two routes 
        List<Integer> route1 = routes.get(index1);
        List<Integer> route2 = routes.get(index2);
        int len1 = route1.size();
        int len2 = route2.size();
        boolean feas1 = feasibleRoutes.get(index1);
        boolean feas2 = feasibleRoutes.get(index2);
        
        if (len1 < 3 || len2 < 3) {
            return;
        }

        // all combinatios of i and j
        for (int i = 1; i < len1 - 1; i++) {
            for (int j = 1; j < len2 - 1; j++) {
                
                // check load 
                double load1 = routesLoads.get(index1) - demand.get(route1.get(i)) 
                        + demand.get(route2.get(j));
                double load2 = routesLoads.get(index2) - demand.get(route2.get(j)) 
                        + demand.get(route1.get(i));
                if (load1 > capacity || load2 > capacity) {
                    continue;
                }
                
                // check times
                double time1 = routesTimes.get(index1).get(i - 1)[2] 
                        + times.get(route1.get(i - 1), route2.get(j));
                double time2 = routesTimes.get(index2).get(j - 1)[2] 
                        + times.get(route2.get(j - 1), route1.get(i));
                if (time1 > windows.get(route2.get(j))[1] 
                        || time2 > windows.get(route1.get(i))[1]) {
                    continue;
                }

                // exchange nodes 
                List<Integer> tmp1 = new ArrayList<>(route1);
                tmp1.set(i, route2.get(j));
                List<Integer> tmp2 = new ArrayList<>(route2);
                tmp2.set(j, route1.get(i));

                // if total cost is less than best and both feasible, then keep them
                double org = times.get(route1.get(i - 1), route1.get(i)) 
                        + times.get(route1.get(i), route1.get(i + 1))
                        + times.get(route2.get(j - 1), route2.get(j)) 
                        + times.get(route2.get(j), route2.get(j + 1));
                double change = times.get(route1.get(i - 1), route2.get(j)) 
                        + times.get(route2.get(j), route1.get(i + 1))
                        + times.get(route2.get(j - 1), route1.get(i)) 
                        + times.get(route1.get(i), route2.get(j + 1));
                double diff = org - change;
                if (diff > 0) {
                    
                    // new solution
                    if ((!feas1 || feasible(tmp1)) && (!feas2 || feasible(tmp2))) {
                        
                        // update
                        routes.set(index1, tmp1);
                        routes.set(index2, tmp2);
                        updateRoute(index1);
                        updateRoute(index2);
                        route1 = new ArrayList<>(tmp1);
                        route2 = new ArrayList<>(tmp2);
                        feas1 = feasibleRoutes.get(index1);
                        feas2 = feasibleRoutes.get(index2);
                    }
                }
            }
        }
    }
    
    void searchInterchange() {
        
        List<Integer> indexes = getRandomIndexes();
        for (int i = 0; i < indexes.size(); i++) {
            for (int j = i + 1; j < indexes.size(); j++) {
                searchInterchange(indexes.get(i), indexes.get(j));
            }
        }
        
        updateSolution();
    }
    
    void pathRelocation() {
        
        List<Integer> indexes = getRandomIndexes();
        for (int i = 0; i < indexes.size(); i++) {
            for (int j = i + 1; j < indexes.size(); j++) {
                pathRelocation(indexes.get(i), indexes.get(j));
            }
        }
        
        updateSolution();
    }
    
    private void pathRelocation(int index1, int index2) {
        
        // info 
        double capacity = param.getCapacity();
        
        // get the two routes 
        List<Integer> route1 = routes.get(index1);
        List<Integer> route2 = routes.get(index2);
        double load1 = routesLoads.get(index1);
        double load2 = routesLoads.get(index2);
        
        if (route1.size() < 3 || route2.size() < 3) {
            return;
        }
        
        // all combinatios of i and j
        for (int i = 1; i < route1.size() - 2; i++) {
            for (int j = i + 1; j < route1.size() - 1; j++) {
                
                // check load 
                double load = 0;
                for (int l = i + 1; l < j + 1; l++) {
                    load += demand.get(route1.get(l));
                }
                if (load + load2 > capacity) {
                    break;
                }
                
                // nodes route 1
                Integer n1 = route1.get(i);
                Integer n2 = route1.get(i + 1);
                Integer n3 = route1.get(j);
                Integer n4 = route1.get(j + 1);
                
                // check all positions in route2 
                for (int k = 1; k < route2.size() - 1; k++) {
                    
                    // nodes route 2
                    Integer n5 = route2.get(k);
                    Integer n6 = route2.get(k + 1);
                    
                    // compute diff
                    double org = times.get(n1, n2) + times.get(n3, n4) + times.get(n5, n6);
                    double change = times.get(n1, n4) + times.get(n5, n2) + times.get(n3, n6);
                    if (org - change <= 0) {
                        continue;
                    }
                    
                    // new routes
                    List<Integer> tmp1 = new ArrayList<>(route1.subList(0, i + 1));
                    tmp1.addAll(route1.subList(j + 1, route1.size()));
                    List<Integer> tmp2 = new ArrayList<>(route2.subList(0, k + 1));
                    tmp2.addAll(route1.subList(i + 1, j + 1));
                    tmp2.addAll(route2.subList(k + 1, route2.size()));
                    if (feasible(tmp2)) {
                        
                        // update
                        routes.set(index1, tmp1);
                        routes.set(index2, tmp2);
                        updateRoute(index1);
                        updateRoute(index2);
                        return;
                    }
                }
            }
        }
    }
    
    void search2OptInter() {
        
        List<Integer> indexes = getRandomIndexes();
        for (int i = 0; i < indexes.size(); i++) {
            for (int j = i + 1; j < indexes.size(); j++) {
                search2OptInter(indexes.get(i), indexes.get(j));
            }
        }
        
        updateSolution();
    }
    
    private void search2OptInter(int index1, int index2) {
        
        // less than 1 route, return
        if (routes.size() < 2) {
            return;
        }
        
        // info 
        double capacity = param.getCapacity();
        
        // get the two routes 
        List<Integer> route1 = routes.get(index1);
        List<Integer> route2 = routes.get(index2);
        boolean feas1 = feasibleRoutes.get(index1);
        boolean feas2 = feasibleRoutes.get(index2);
        
        if (route1.size() < 3 || route2.size() < 3) {
            return;
        }

        // all combinatios of i and j
        double total1 = 0;
        for (int i = 0; i < route1.size() - 1; i++) {
            
            // keep track of volume
            total1 += demand.get(route1.get(i));
            double total2 = 0;
            for (int j = 0; j < route2.size() - 1; j++) {
                
                // check load 
                total2 += demand.get(route2.get(j));
                double load1 = routesLoads.get(index2) - total2 + total1;
                double load2 = routesLoads.get(index1) - total1 + total2;
                if (load1 > capacity || load2 > capacity) {
                    continue;
                }
                
                // check times
                double time1 = routesTimes.get(index1).get(i)[2] 
                        + times.get(route1.get(i), route2.get(j + 1));
                double time2 = routesTimes.get(index2).get(j)[2] 
                        + times.get(route2.get(j), route1.get(i + 1));
                if (time1 > windows.get(route2.get(j + 1))[1] 
                        || time2 > windows.get(route1.get(i + 1))[1]) {
                    continue;
                }

                // exchange nodes 
                List<Integer> tmp1 = new ArrayList<>(route1.subList(0, i + 1));
                tmp1.addAll(route2.subList(j + 1, route2.size()));
                List<Integer> tmp2 = new ArrayList<>(route2.subList(0, j + 1));
                tmp2.addAll(route1.subList(i + 1, route1.size()));

                // if total cost is less than best and both feasible, then keep them
                double org = times.get(route1.get(i), route1.get(i + 1)) 
                        + times.get(route2.get(j), route2.get(j + 1));
                double change = times.get(route1.get(i), route2.get(j + 1)) 
                        + times.get(route2.get(j), route1.get(i + 1));
                double diff = org - change;
                if (diff > 0) {
                    
                    // new solution
                    if ((!feas1 || feasible(tmp1)) && (!feas2 || feasible(tmp2))) {
                        
                        // update
                        routes.set(index1, tmp1);
                        routes.set(index2, tmp2);
                        updateRoute(index1);
                        updateRoute(index2);
                        route1 = new ArrayList<>(tmp1);
                        route2 = new ArrayList<>(tmp2);
                        feas1 = feasibleRoutes.get(index1);
                        feas2 = feasibleRoutes.get(index2);
                    }
                }
            }
        }
    }
    
    LinkedHashMap<Integer, Double> relatednessMeasure(int node) {
        
        // measure parameters
        double phi = 9.0;
        double xi = 3.0;
        double psi = 2.0;
        
        // map with values
        Map<Integer, Double> values = new HashMap<>();
        Map<Integer, Double> arrival = new HashMap<>();
        
        // find route
        List<Integer> route = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).contains(node)) {
                route = routes.get(i);
            }
        }
        if (route.isEmpty()) {
            return null;
        }
        
        // compute time 
        int last = route.get(0);
        double time = service.get(last);
        for (int i = 0; i < route.size(); i++) {
            
            // consider the arrival time to check time window
            int nd = route.get(i);
            time += times.get(last, nd);
            
            // wait if too early
            if (windows.get(nd)[0] > time) {
                time = windows.get(nd)[0];
            }
            
            // compute time upto node
            if (node == nd) {
                break;
            }
            
            // finally sum service time
            time += service.get(nd);
            last = nd;
        }
        
        // compute time distance for all nodes
        for (int i = 0; i < routes.size(); i++) {
            
            // compute time 
            route = routes.get(i);
            last = route.get(0);
            double arr = service.get(last);
            for (int j = 1; j < route.size() - 1; j++) {

                // consider the arrival time to check time window
                Integer nd = route.get(j);
                arr += times.get(last, nd);

                // wait if too early
                if (windows.get(nd)[0] > arr) {
                    arr = windows.get(nd)[0];
                }
                
                // include value for node
                arrival.put(nd, Math.abs(arr - time));

                // finally sum service time
                arr += service.get(nd);
                last = nd;
            }
        }
        
        // normalize values
        double maxDistance = Collections.max(times.row(node).values());
        double maxArrival = Collections.max(arrival.values());
        
        // compute values
        for (Integer nd : arrival.keySet()) {
            double value = phi * times.get(node, nd) / maxDistance 
                    + xi * arrival.get(nd) / maxArrival 
                    + psi * Math.abs(demand.get(nd) - demand.get(node));
            values.put(nd, value);
        }
        
        // sort values
        LinkedHashMap<Integer, Double> relate = sortMap(values, false);
        
        return relate;
    }
    
    Set<Integer> shawRemoval() {
        
        // nodes
        List<Integer> nodes = new ArrayList<>(param.getNodes());
        nodes.remove((Integer) depot);
        
        // parameters
        int p = 6;
        int limit = (int) Math.round(nodes.size() * 0.4);
        int q = 4 + rand.nextInt(Math.min(97, limit + 1));
        
        // random request
        int index = rand.nextInt(nodes.size());
        Integer node = nodes.get(index);
        
        // list of nodes to remove
        List<Integer> remove = new ArrayList<>();
        remove.add(node);
        Map<Integer, LinkedHashMap<Integer, Double>> values = new HashMap<>();
        while (remove.size() < q) {
            index = rand.nextInt(remove.size());
            node = remove.get(index);
            
            // create list if it not in the map
            LinkedHashMap<Integer, Double> map;
            if (values.containsKey(node)) {
                map = values.get(node);
            } else {
                map = relatednessMeasure(node);
            }
            
            // remove values in the set
            for (Integer tmp : remove) {
                map.remove(tmp);
            }
            values.put(node, map);
            
            // sample position
            double y = rand.nextDouble();
            Integer pos = (int) Math.round(map.size() * Math.pow(y, p));
            pos = pos > 0 ? pos - 1 : 0;
            List<Integer> list = new ArrayList<>(map.keySet());
            remove.add(list.get(pos));
        }
        
        // remove all nodes in the list
        for (Integer nd : remove) {
            
            // find route
            List<Integer> route = new ArrayList<>();
            int ind = -1;
            for (int i = 0; i < routes.size(); i++) {
                List<Integer> tmp = new ArrayList<>(routes.get(i));
                if (tmp.contains(nd)) {
                    tmp.remove(nd);
                    route = new ArrayList<>(tmp);
                    ind = i;
                    break;
                }
            }
            
            // set route
            routes.set(ind, route);
            updateRoute(ind);
        }
        
        return new HashSet<>(remove);
    }
    
    Set<Integer> randomRemoval() {
        
        // nodes
        List<Integer> nodes = new ArrayList<>(param.getNodes());
        nodes.remove((Integer) depot);
        
        // parameters
        int total = nodes.size();
        int limit = (int) Math.round(total * 0.4);
        int q = 4 + rand.nextInt(Math.min(97, limit + 1));
        
        // list of nodes to remove
        Set<Integer> remove = new HashSet<>();
        while (remove.size() < q) {
            
            // random request
            int index = rand.nextInt(total);
            remove.add(nodes.get(index));
        }
        
        // remove all nodes in the list
        for (Integer nd : remove) {
            
            // find route
            List<Integer> route = new ArrayList<>();
            int ind = -1;
            for (int i = 0; i < routes.size(); i++) {
                List<Integer> tmp = new ArrayList<>(routes.get(i));
                if (tmp.contains(nd)) {
                    tmp.remove(nd);
                    route = new ArrayList<>(tmp);
                    ind = i;
                    break;
                }
            }
            
            // set route
            routes.set(ind, route);
            updateRoute(ind);
        }
        
        return remove;
    }
    
    Set<Integer> worstRemoval() {
        
        // nodes
        List<Integer> nodes = new ArrayList<>(param.getNodes());
        nodes.remove((Integer) depot);
        
        // parameters
        int p = 6;
        int limit = (int) Math.round(nodes.size() * 0.4);
        int q = 4 + rand.nextInt(Math.min(97, limit + 1));
        
        // compute values
        Map<Integer, Integer> relation = new HashMap<>();
        Map<Integer, Double> values = new LinkedHashMap<>();
        for (int i = 0; i < routes.size(); i++) {
            List<Integer> route = routes.get(i);
            for (int j = 1; j < route.size() - 1; j++) {
                Integer node = route.get(j);
                Integer n1 = route.get(j - 1);
                Integer n2 = route.get(j + 1);
                values.put(node, times.get(n1, node) + times.get(node, n2) - times.get(n1, n2));
                relation.put(node, i);
            }
        }
        
        // sort map
        values = sortMap(values, true);
        
        // list of nodes to remove
        List<Integer> remove = new ArrayList<>();
        while (remove.size() < q) {
            
            // sample position
            double y = rand.nextDouble();
            Integer pos = (int) Math.round(values.size() * Math.pow(y, p));
            pos = pos > 0 ? pos - 1 : 0;
            List<Integer> tmp = new ArrayList<>(values.keySet());
            Integer node = tmp.get(pos);
            remove.add(node);
            
            // remove node from route
            int index = relation.get(node);
            List<Integer> route = routes.get(index);
            route.remove(node);
            routes.set(index, route);
            updateRoute(index);
            
            // update map
            values.remove(node);
            for (int i = 1; i < route.size() - 1; i++) {
                Integer nd = route.get(i);
                Integer n1 = route.get(i - 1);
                Integer n2 = route.get(i + 1);
                values.put(nd, times.get(n1, nd) + times.get(nd, n2) - times.get(n1, n2));
            }
            values = sortMap(values, true);
        }
        
        return new HashSet<>(remove);
    }
    
    private LinkedHashMap<Integer, Double> sortMap(Map<Integer, Double> values, boolean reverse) {
        
        // sort map
        List<Entry<Integer, Double>> list = new ArrayList<>(values.entrySet());
        if (reverse) {
            list.sort(new Comparator<Entry<Integer, Double>>() {
            
                // new comparator for cases with equal value
                @Override
                public int compare(Entry<Integer, Double> m1, Entry<Integer, Double> m2) {
                    if (m1.getValue().equals(m2.getValue())) {
                        return (m1.getKey() < m2.getKey()) ? -1 : 1;
                    }
                    return (m1.getValue() > m2.getValue()) ? -1 : 1;
                }
            });
        } else {
            list.sort(new Comparator<Entry<Integer, Double>>() {

                // new comparator for cases with equal value
                @Override
                public int compare(Entry<Integer, Double> m1, Entry<Integer, Double> m2) {
                    if (m1.getValue().equals(m2.getValue())) {
                        return (m1.getKey() < m2.getKey()) ? -1 : 1;
                    }
                    return (m1.getValue() < m2.getValue()) ? -1 : 1;
                }
            });
        }
        
        LinkedHashMap<Integer, Double> relate = new LinkedHashMap<>();
        for (Entry<Integer, Double> entry : list) {
            relate.put(entry.getKey(), entry.getValue());
        }
        
        return relate;
    }
    
    Set<Integer> routeRemoval() {
        
        // geometric parameter
        double rho = 0.25;
        
        // compute distance
        Map<Integer, Double> distance = new HashMap<>();
        for (int i = 0; i < routes.size(); i++) {
            
            List<Integer> route = routes.get(i);
            if (route.size() < 3) {
                continue;
            }
            
            // compute distance for each node
            double total = 0;
            for (int j = 1; j < route.size() - 1; j++) {
                Integer node = route.get(j);
                
                // find min value 
                double center = Double.MAX_VALUE;
                for (int k = 0; k < routes.size(); k++) {
                    if (k == i) {
                        continue;
                    }
                    List<Integer> tmp = routes.get(k);
                    double value = 0;
                    for (int l = 1; l < tmp.size() - 1; l++) {
                        value += times.get(tmp.get(l), node);
                    }
                    value = value / tmp.size();
                    if (value < center) {
                        center = value;
                    }
                }
                total += center;
            }
            distance.put(i, total);
        }
        
        if (distance.isEmpty()) {
            return new HashSet<>();
        }
        
        // sort values
        LinkedHashMap<Integer, Double> map = sortMap(distance, false);
        
        // find index
        int index = map.keySet().iterator().next();
        for (Integer ind : map.keySet()) {
            if (rand.nextDouble() < rho) {
                index = ind;
                break;
            }
        }
        Set<Integer> remove = new HashSet<>(routes.get(index));
        remove.remove(depot);
        
        // remove all nodes
        List<Integer> tmp = new ArrayList<>();
        tmp.add(depot);
        tmp.add(depot);
        routes.set(index, tmp);
        updateRoute(index);
        
        return remove;
    }
    
    Set<Integer> windowRemoval() {
        
        // nodes
        List<Integer> nodes = new ArrayList<>(param.getNodes());
        nodes.remove((Integer) depot);
        
        // parameters
        int p = 6;
        int total = nodes.size();
        int limit = (int) Math.round(total * 0.4);
        int q = 4 + rand.nextInt(Math.min(97, limit + 1));
        
        // compute diff with window 
        Map<Integer, Double> values = new HashMap<>();
        for (int i = 0; i < routes.size(); i++) {
            List<Integer> route = routes.get(i);
            for (int j = 1; j < route.size() - 1; j++) {
                Integer node = route.get(j);
                Double[] vector = routesTimes.get(i).get(j);
                double value = vector[1] - vector[0];
                if (value > 0) {
                    values.put(node, value);
                }
            }
        }
        
        // sort values
        LinkedHashMap<Integer, Double> diff = sortMap(values, true);
        
        // list of nodes to remove
        Set<Integer> remove = new HashSet<>();
        while (remove.size() < q && remove.size() < diff.size()) {
            
            // sample position
            double y = rand.nextDouble();
            Integer pos = (int) Math.round(diff.size() * Math.pow(y, p));
            pos = pos > 0 ? pos - 1 : 0;
            List<Integer> tmp = new ArrayList<>(diff.keySet());
            remove.add(tmp.get(pos));
        }
        
        // remove all nodes in the list
        for (Integer nd : remove) {
            
            // find route
            List<Integer> route = new ArrayList<>();
            int ind = -1;
            for (int i = 0; i < routes.size(); i++) {
                List<Integer> tmp = new ArrayList<>(routes.get(i));
                if (tmp.contains(nd)) {
                    tmp.remove(nd);
                    route = new ArrayList<>(tmp);
                    ind = i;
                    break;
                }
            }
            
            // set route
            routes.set(ind, route);
        }
        
        updateSolution();
        return remove;
    }
    
    Set<Integer> timeRadialRuin(double div) {
        
        // pick a request at random
        List<Integer> nodes = new ArrayList<>(param.getNodes());
        nodes.remove((Integer) depot);
        int index = rand.nextInt(nodes.size());
        Integer node = nodes.get(index);
        
        // find route
        List<Integer> route = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).contains(node)) {
                route = routes.get(i);
            }
        }
        if (route.isEmpty()) {
            return new HashSet<>();
        }
        
        // compute time 
        int last = route.get(0);
        double time = service.get(last);
        for (int i = 0; i < route.size(); i++) {
            
            // consider the arrival time to check time window
            int nd = route.get(i);
            time += times.get(last, nd);
            
            // wait if too early
            if (windows.get(nd)[0] > time) {
                time = windows.get(nd)[0];
            }
            
            // compute time upto node
            if (node.equals(nd)) {
                break;
            }
            
            // finally sum service time
            time += service.get(nd);
            last = nd;
        }
        
        // threshold
        double limit = windows.get(depot)[1] / div;
        double[] limits = new double[] {time - limit, time + limit};
        
        // find all nodes within the proximity
        Set<Integer> list = new HashSet<>(node);
        Map<Integer, List<Integer>> map = new HashMap<>();
        for (int i = 0; i < routes.size(); i++) {
            
            // create map
            map.put(i, new ArrayList<>());
            
            // compute time 
            route = routes.get(i);
            last = route.get(0);
            time = service.get(last);
            for (int j = 1; j < route.size() - 1; j++) {

                // consider the arrival time to check time window
                Integer nd = route.get(j);
                time += times.get(last, nd);

                // wait if too early
                if (windows.get(nd)[0] > time) {
                    time = windows.get(nd)[0];
                }
                
                // check time
                if (limits[0] <= time && time <= limits[1]) {
                    list.add(nd);
                    map.get(i).add(nd);
                }
                if (time > limits[1]) {
                    break;
                }

                // finally sum service time
                time += service.get(nd);
                last = nd;
            }
        }
        
        // remove all nodes in the list
        for (Integer ind : map.keySet()) {
            route = routes.get(ind);
            for (Integer nd : map.get(ind)) {
                route.remove(nd);
            }
            routes.set(ind, route);
        }
        
        return list;
    }
    
    Set<Integer> distanceRadialRuin(double div) {
        
        // pick a request at random
        List<Integer> nodes = new ArrayList<>(param.getNodes());
        nodes.remove((Integer) depot);
        int index = rand.nextInt(nodes.size());
        Integer node = nodes.get(index);
        
        // find route
        List<Integer> route = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).contains(node)) {
                route = routes.get(i);
            }
        }
        if (route.isEmpty()) {
            return new HashSet<>();
        }
        
        // compute threshold
        double max = 0;
        nodes.add(depot);
        for (Integer n1 : nodes) {
            for (Integer n2 : nodes) {
                
                double dist = times.get(n1, n2);
                if (dist > max) {
                    max = dist;
                }
            }
        }
        
        // find nodes
        Set<Integer> list = new HashSet<>(node);
        double limit = max / div;
        for (int i = 0; i < routes.size(); i++) {
            List<Integer> tmp = new ArrayList<>(routes.get(i));
            for (int j = 1; j < tmp.size() - 1; j++) {
                Integer nd = tmp.get(j);
                double dist = times.get(node, nd);
                if (dist < limit) {
                    list.add(nd);
                }
            }
        }
        
        // remove nodes
        for (Integer nd : list) {
            for (int i = 0; i < routes.size(); i++) {
                List<Integer> tmp = routes.get(i);
                if (tmp.contains(nd)) {
                    tmp.remove(nd);
                }
            }
        }
        
        return list;
    }
    
    void greedyHeuristic(Set<Integer> list, boolean noise) {
        
        // noise interval
        double max = -1;
        Set<Integer> nodes = param.getNodes();
        for (Integer n1 : nodes) {
            for (Integer n2 : nodes) {
                double value = times.get(n1, n2);
                if (value > max) {
                    max = value;
                }
            }
        }
        double plus = max * 0.025;
        
        // tables
        Table<Integer, Integer, Double> cost = HashBasedTable.create();
        Table<Integer, Integer, Integer> locations = HashBasedTable.create();
        
        // routes
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            
            // do not check this route if it is not feasible
            if (feasibleRoutes.get(i)) {
                indexes.add(i);
            }
        }
        
        // iterate until list is empty
        while (!list.isEmpty()) {
            
            // find the best place
            double min = Double.MAX_VALUE;
            Integer insert = -1;
            Integer location = -1;
            
            // check previous values
            for (Integer node : list) {
                
                Map<Integer, Double> row = cost.row(node);
                for (Integer index : row.keySet()) {
                    if (min > row.get(index)) {
                        min = row.get(index);
                        insert = node;
                        location = index;
                    }
                }
            }
            
            // check modified routes 
            List<Integer> check = new ArrayList<>(indexes);
            for (Integer i : check) {
                List<Integer> route = routes.get(i);
                indexes.remove(i);

                for (Integer node : list) {
                    
                    // check if feasible
                    if (cost.contains(node, i) && cost.get(node, i) >= constFeas) {
                        continue;
                    }
                    
                    // find the best position for this node
                    double ins = Double.MAX_VALUE;
                    for (int j = 0; j < route.size() - 1; j++) {

                        // insert in position j + 1
                        double value = times.get(route.get(j), node) 
                                + times.get(node, route.get(j + 1));
                        
                        // check noise
                        if (noise) {
                            double unif = plus - rand.nextDouble() * 2 * plus;
                            value = Math.max(value + unif, 0);
                        }
                        
                        // new route
                        List<Integer> tmp = new ArrayList<>(route);
                        tmp.add(j + 1, node);

                        // check value and feasibility
                        if (ins > value && feasible(tmp)) {
                            ins = value;
                            cost.put(node, i, value);
                            locations.put(node, i, j + 1);
                            
                            // if min, update insert
                            if (value < min) {
                                min = value;
                                insert = node;
                                location = i;
                            }
                        }
                    }
                    
                    // add large value when infeasible
                    if (ins > constFeas) {
                        cost.put(node, i, constFeas);
                    }
                }
            }
            
            // insert node if found a feasible option
            if (min < constFeas) {
                
                // insert best node at the min position 
                int pos = locations.get(insert, location);
                List<Integer> tmp = new ArrayList<>(routes.get(location));
                tmp.add(pos, insert);
                routes.set(location, tmp);
                updateRoute(location);
                
                // remove this node
                list.remove(insert);
                Set<Integer> row = new HashSet<>(cost.row(insert).keySet());
                for (Integer ind : row) {
                    cost.remove(insert, ind);
                    locations.remove(insert, ind);
                }
                
                // check this route again
                Set<Integer> col = new HashSet<>(cost.rowKeySet());
                indexes.add(location);
                for (Integer node : col) {
                    if (cost.get(node, location) < constFeas) {
                        cost.remove(node, location);
                    }
                }
            } else {
                
                // create empty route for a node
                Iterator<Integer> iter = (new ArrayList<>(list)).iterator();
                Integer node = iter.next();
                
                // out-and-back route
                List<Integer> tmp = new ArrayList<>();
                tmp.add(depot);
                tmp.add(node);
                tmp.add(depot);
                int index = routes.size() -1;
                routes.add(index, tmp);
                updateRoute(index);

                // remove node and check this route again
                list.remove(node);
                Set<Integer> row = new HashSet<>(cost.row(insert).keySet());
                for (Integer ind : row) {
                    cost.remove(insert, ind);
                    locations.remove(insert, ind);
                }
                indexes.add(index);
            }
        }
        
        updateSolution();
    }
    
    void regretHeuristic(int len, Set<Integer> list, boolean noise) {
        
        // number of routes
        if (routes.size() < 2) {
            greedyHeuristic(list, noise);
            return;
        }
        
        // check len
        if (len < 2) {
            return;
        }
        
        // noise interval
        double maxDist = -1;
        Set<Integer> nodes = param.getNodes();
        for (Integer n1 : nodes) {
            for (Integer n2 : nodes) {
                double value = times.get(n1, n2);
                if (value > maxDist) {
                    maxDist = value;
                }
            }
        }
        double plus = maxDist * 0.025;
        
        // tables
        Table<Integer, Integer, Double> cost = HashBasedTable.create();
        Table<Integer, Integer, Integer> locations = HashBasedTable.create();
        
        // routes
        List<Integer> indexes = new ArrayList<>();
        for (int i = 0; i < routes.size(); i++) {
            
            // do not check this route if it is not feasible
            if (feasibleRoutes.get(i)) {
                indexes.add(i);
            }
        }
        
        // iterate until list is empty
        while (!list.isEmpty()) {
            
            // check modified routes 
            List<Integer> check = new ArrayList<>(indexes);
            for (Integer i : check) {
                List<Integer> route = routes.get(i);
                indexes.remove(i);

                for (Integer node : list) {
                    
                    // check if feasible
                    if (cost.contains(node, i) && cost.get(node, i) >= constFeas) {
                        continue;
                    }
                    
                    // find the best position for this node
                    double ins = Double.MAX_VALUE;
                    for (int j = 0; j < route.size() - 1; j++) {

                        // insert in position j + 1
                        double value = times.get(route.get(j), node) 
                                + times.get(node, route.get(j + 1));
                        
                        // check noise
                        if (noise) {
                            double unif = plus - rand.nextDouble() * 2 * plus;
                            value = Math.max(value + unif, 0);
                        }
                        
                        // new route
                        List<Integer> tmp = new ArrayList<>(route);
                        tmp.add(j + 1, node);

                        // check value and feasibility
                        if (ins > value && feasible(tmp)) {
                            ins = value;
                            cost.put(node, i, value);
                            locations.put(node, i, j + 1);
                        }
                    }
                    
                    // add large value if not feaible
                    if (!cost.contains(node, i)) {
                        cost.put(node, i, constFeas);
                    }
                }
            }
            
            // find the best place
            double max = -Double.MAX_VALUE;
            Integer insert = -1;
            Integer location = -1;
            
            // check previous values
            for (Integer node : cost.rowKeySet()) {
                
                // sort values
                Map<Integer, Double> row = cost.row(node);
                List<Entry<Integer, Double>> tmp = new ArrayList<>(row.entrySet());
                tmp.sort(new Comparator<Entry<Integer, Double>>() {

                    // new comparator for cases with equal value
                    @Override
                    public int compare(Entry<Integer, Double> m1, Entry<Integer, Double> m2) {
                        if (m1.getValue().equals(m2.getValue())) {
                            return (m1.getKey() < m2.getKey()) ? -1 : 1;
                        }
                        return (m1.getValue() < m2.getValue()) ? -1 : 1;
                    }
                });
                
                // compute regret
                double best = constFeas;
                double value = 0;
                int cnt = 0;
                int loc = -1;
                for (Entry<Integer, Double> entry : tmp) {
                    
                    // best position
                    if (cnt == 0) {
                        best = entry.getValue();
                        loc = entry.getKey();
                    } else {
                        
                        // regret
                        value += entry.getValue() - best;
                    }
                    
                    cnt++;
                    if (cnt == len) {
                        break;
                    }
                }
                
                // keep the node with the max regret
                if (max < value && best < constFeas) {
                    max = value;
                    insert = node;
                    location = loc;
                }
            }
            
            // insert node if found a feasible option
            if (insert != -1) {
                
                // insert best node at the min position 
                int pos = locations.get(insert, location);
                List<Integer> tmp = new ArrayList<>(routes.get(location));
                tmp.add(pos, insert);
                routes.set(location, tmp);
                updateRoute(location);
                
                // remove this node
                list.remove(insert);
                Set<Integer> row = new HashSet<>(cost.row(insert).keySet());
                for (Integer ind : row) {
                    cost.remove(insert, ind);
                    locations.remove(insert, ind);
                }
                
                // check this route again
                Set<Integer> col = new HashSet<>(cost.rowKeySet());
                indexes.add(location);
                for (Integer node : col) {
                    if (cost.contains(node, location) 
                            && cost.get(node, location) < constFeas) {
                        cost.remove(node, location);
                    }
                }
            } else {
                
                // create empty route for a node
                Iterator<Integer> iter = (new ArrayList<>(list)).iterator();
                Integer node = iter.next();
                
                // out-and-back route
                List<Integer> tmp = new ArrayList<>();
                tmp.add(depot);
                tmp.add(node);
                tmp.add(depot);
                int index = routes.size() -1;
                routes.add(index, tmp);
                updateRoute(index);

                // remove node and check this route again
                list.remove(node);
                Set<Integer> row = new HashSet<>(cost.row(insert).keySet());
                for (Integer ind : row) {
                    cost.remove(insert, ind);
                    locations.remove(insert, ind);
                }
                indexes.add(index);
            }
        }
        
        updateSolution();
    }
    
    void regretHeuristicAll(Set<Integer> list, boolean noise) {
        regretHeuristic(routes.size(), list, noise);
    }
    
    int nLowLevel() {
        return nLowLevel;
    }
    
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
            default -> {
            }
        }
    }
}
