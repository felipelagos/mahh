package multiarmedhyper.data;

import com.google.common.collect.HashBasedTable;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.StringTokenizer;

public class InstanceReader extends ParameterReader {
    
    private final String file;
    
    public InstanceReader(String file) {
        super(null);
        this.file = file;
    }
    
    @Override
    boolean data() {
        
        try {
            
            // list of customer info
            List<Double[]> ls = new ArrayList<>();
            
            // read file
            BufferedReader buffer = new BufferedReader(new FileReader(file));
            String line;
            for (int i = 0; (line = buffer.readLine()) != null; i++) {
                
                if (i == 0) {
                    instance  = line.replace(" ", "");
                } else if (i == 4) {
                    
                    StringTokenizer token = line.contains(" ") ? 
                            new StringTokenizer(line, " ") : new StringTokenizer(line, "\t");
                    
                    // number of vehicles
                    try {
                        nVehicles  = Integer.parseInt(token.nextToken());
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Error input number of vehicles.\n" 
                                + e.toString());
                    }
                    
                    // vehicle capacity
                    try {
                        capacity  = Double.parseDouble(token.nextToken());
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Error input vehicle capacity.\n" 
                                + e.toString());
                    }
                } else if (i >= 9) {
                    
                    // customers info
                    StringTokenizer token = line.contains(" ") ? 
                            new StringTokenizer(line, " ") : new StringTokenizer(line, "\t");
                    
                    // array with info
                    Double[] tm = new Double[7];
                    int j = 0;
                    for (; token.hasMoreTokens() && j < 7; j++) {
                        try {
                            tm[j] = Double.parseDouble(token.nextToken());
                            if (j != 0) {
                                tm[j] = Math.round(tm[j] * 1E2) / 1E2;
                            }
                        } catch (NumberFormatException e) {
                            throw new AssertionError("Error customer information.\n" 
                                + e.toString());
                        }
                    }
                    if (j == 7) {
                        ls.add(tm);
                    }
                }
            }
            
            buffer.close();
            
            // store information
            positions = new HashMap<>();
            service = new HashMap<>();
            demand = new HashMap<>();
            windows = new HashMap<>();

            for (int i = 0; i < ls.size(); i++) {
                int index = ls.get(i)[0].intValue();
                positions.put(index, new Double[] {ls.get(i)[1], ls.get(i)[2]});
                demand.put(index, ls.get(i)[3]);
                windows.put(index, new Double[] {ls.get(i)[4], ls.get(i)[5]});
                service.put(index, ls.get(i)[6]);
            }

            // instance is not valid
            if (ls.size() < 1) {
                return false;
            }

            nNodes = ls.size();
            depot = 0;
            
        } catch (IOException e) {
            throw new AssertionError("Reading input file error.\n" + e.toString());
        }
        
        return true;
    }
    
    public void setDistanceMatrix(String csv) {
        
        try {
            
            // all indexes
            Set<Integer> nodes = new HashSet<>();
            nodes.addAll(positions.keySet());

            // create a matrix with all distances
            time = HashBasedTable.create();
            
            // read file
            BufferedReader buffer = new BufferedReader(new FileReader(csv));
            String line;
            for (int i1 = 0; (line = buffer.readLine()) != null; i1++) {
                StringTokenizer token = new StringTokenizer(line, ";");
                for (int i2 = 0; token.hasMoreTokens(); i2++) {
                    double dist = Double.parseDouble(token.nextToken());
                    
                    if (nodes.contains(i1) && nodes.contains(i2)) {
                        time.put(i1, i2, dist);
                    }
                }
            }
            
        } catch (IOException e) {
            throw new AssertionError("Reading matrix file error.\n" + e.toString());
        }
    }
    
    @Override
    public String info() {
        
        if (instance == null) {
            return "no instance";
        }
        String output = "file: " + file + "\n"
                + "instance: " + instance + "\n"
                + "number of nodes: " + nNodes + "\n"
                + "vehicles: " + nVehicles;
        return output;
    }
}
