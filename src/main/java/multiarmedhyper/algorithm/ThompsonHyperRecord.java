
package multiarmedhyper.algorithm;

import multiarmedhyper.data.ParameterReader;

public class ThompsonHyperRecord extends ThompsonHyper {
    
    public ThompsonHyperRecord(ParameterReader param) {
        super(param);
    }
    
    @Override
    public String algorithmName() {
        return "ThompsonHyperRecord[" + delta + "]";
    }
    
    @Override
    boolean acceptSolution(double tmpCost) {
        return (tmpCost <  (1 + delta) * bestValue);
    }
}
