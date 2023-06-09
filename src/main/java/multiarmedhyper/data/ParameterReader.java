
package multiarmedhyper.data;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public abstract class ParameterReader {
    
    // instance global parameters
    String instance;
    int nVehicles;
    int nNodes;
    double capacity;
    
    // depot index
    int depot;
    
    // details
    Map<Integer, Double> demand;
    Map<Integer, Double[]> positions;
    Map<Integer, Double[]> windows;
    Map<Integer, Double> service;
    
    // distance between all nodes
    Table<Integer, Integer, Double> time;
    
    // distance precision
    boolean typeDouble = false;
    
    public ParameterReader(String instance) {
        this.instance = instance;
    }
    
    abstract boolean data();
    
    public boolean read() {
        
        boolean status = data();
        computeTime();
        
        return status;
    }
    
    public void setDistanceType(boolean typeDouble) {
        this.typeDouble = typeDouble;
    }
    
    private void computeTime() {
        
        // all indexes
        Set<Integer> nodes = new HashSet<>();
        nodes.addAll(positions.keySet());
        
        // create a matrix with all distances
        time = HashBasedTable.create();
        
        // distance computation, round to the second decimal digit
        for (Integer n1 : nodes) {
            
            Double[] pos1 = positions.get(n1);
            for (Integer n2 : nodes) {
                
                Double[] pos2 = positions.get(n2);
                double value = distancePrecision(
                        Math.sqrt(Math.pow(pos1[0] - pos2[0], 2) 
                                + Math.pow(pos1[1] - pos2[1], 2)));
                time.put(n1, n2, value);
            }
        }
    }
    
    private double distancePrecision(double distance) {
        
        if (typeDouble) {
            return distance;
        } else {
            return Math.floor(distance * 1E1) / 1E1;
        }
    }
    
    public Map<Integer, Double[]> getTimeWindows() {
        return new HashMap<>(windows);
    }
    
    public Table<Integer, Integer, Double> getTime() {
        return HashBasedTable.create(time);
    }
    
    public Map<Integer, Double> getService() {
        return new HashMap<>(service);
    }
    
    public Map<Integer, Double> getDemand() {
        return new HashMap<>(demand);
    }
    
    public int getNVehicles() {
        return nVehicles;
    }
    
    public double getCapacity() {
        return capacity;
    }
    
    public Set<Integer> getNodes() {
        return new HashSet<>(positions.keySet());
    }
    
    public String getInstance() {
        return instance;
    }
    
    public int getDepot() {
        return depot;
    }
    
    public void clear() {
        
        // clear maps
        demand.clear();
        positions.clear();
        windows.clear();
        service.clear();
        
        // clear matrix 
        time.clear();
        
        instance = null;
    }
    
    abstract public String info();
}
