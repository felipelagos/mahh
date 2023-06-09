
package multiarmedhyper.algorithm;

import com.google.common.collect.Table;
import java.util.List;
import java.util.Map;
import multiarmedhyper.data.ParameterReader;

public interface Algorithm {
    
    public ParameterReader getParam();
    
    public void setTimeLimit(double timeLimit);
    
    public void setSeed(long seed);
    
    public void setIterLimit(int iterLimit);
    
    public void solve();
    
    public double getTotalCost();
    
    public String getOutput();
    
    public void setDelta(double delta);
    
    public double getRunTime();
    
    public int getIterations();
    
    public boolean isFeasible();
    
    public void setOutput(boolean set);
    
    public List<List<Integer>> getBestRoutes();
    
    public List<List<Double>> getBestTimes();
    
    public abstract String algorithmName();
    
    public abstract Table<Integer, Integer, Double> getTransitions();
    
    public abstract Map<Integer, Double> getApply();
}
