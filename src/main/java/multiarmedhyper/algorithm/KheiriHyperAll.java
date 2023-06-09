
package multiarmedhyper.algorithm;

import java.util.List;
import multiarmedhyper.data.ParameterReader;

public class KheiriHyperAll extends KheiriHyper {
    
    public KheiriHyperAll(ParameterReader param) {
        super(param);
    }
    
    @Override
    public String algorithmName() {
        return "KheiriHyperAll[" + delta + "]";
    }
    
    @Override
    void updateSuccess(List<Integer> sequence) {}
    
    @Override
    void updateTmp(List<Integer> sequence) {
        
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
}
