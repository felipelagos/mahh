
package multiarmedhyper.algorithm;

import multiarmedhyper.data.ParameterReader;

public class ExpoHyperRecord extends ExpoHyper {
    
    public ExpoHyperRecord(ParameterReader param, double eta) {
        super(param, eta);
    }
    
    public ExpoHyperRecord(ParameterReader param) {
        super(param);
    }
    
    @Override
    public String algorithmName() {
        return "ExpoHyperRecord[" + eta + "," + delta + "]";
    }
    
    @Override
    boolean acceptSolution(double tmpCost) {
        return (tmpCost <  (1 + delta) * bestValue);
    }
}
