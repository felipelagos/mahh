
package multiarmedhyper.data;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class BaseReader extends ParameterReader {
    
    // info DB
    private final String user;
    private final String pass;
    private final String host;
    
    public BaseReader(String instance, String[] param) {
        super(instance);
        
        // db parameters
        if (param.length > 2) {
            this.host = param[0];
            this.user = param[1];
            this.pass = param[2];
        } else {
            this.host = "localhost/multiarmed";
            this.user = "postgres";
            this.pass = "pass";
        }
    }
    
    public BaseReader(String instance) {
        this(instance, new String[] {});
    }
    
    @Override
    boolean data() {
        
        try {
            
            // parameters connection
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            
            try (Connection conn = DriverManager.getConnection(
                    "jdbc:postgresql://" + host, props)) {
                
                // query instance
                PreparedStatement prep = conn.prepareStatement(
                        "SELECT instance, nodes, nvehicles, capacity "
                                + "FROM instance WHERE instance = ?");
                prep.setString(1, instance);
                
                try (ResultSet rs = prep.executeQuery()) {
                    
                    // if not info, return false
                    if (rs.next()) {
                        nVehicles = rs.getInt("nvehicles");
                        nNodes = rs.getInt("nodes");
                        capacity = rs.getDouble("capacity");
                    } else {
                        return false;
                    }
                }
                prep.close();
                
                // get info for each customer
                List<Double[]> ls = new ArrayList<>();
                prep = conn.prepareStatement("SELECT node, xcoor, "
                        + "ycoor, demand, early, late, service "
                        + "FROM node WHERE instance = ? ORDER BY node");
                prep.setString(1, instance);
                
                try (ResultSet rs = prep.executeQuery()) {
                    while (rs.next()) {
                        Double[] tm = new Double[7];
                        tm[0] = rs.getDouble("node");
                        tm[1] = Math.round(rs.getDouble("xcoor") * 1E2) / 1E2;
                        tm[2] = Math.round(rs.getDouble("ycoor") * 1E2) / 1E2;
                        tm[3] = Math.round(rs.getDouble("demand") * 1E2) / 1E2;
                        tm[4] = Math.round(rs.getDouble("early") * 1E2) / 1E2;
                        tm[5] = Math.round(rs.getDouble("late") * 1E2) / 1E2;
                        tm[6] = Math.round(rs.getDouble("service") * 1E2) / 1E2;
                        ls.add(tm);
                    }
                }
                prep.close();
                
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
                
                depot = 0;
            }
        } catch (SQLException e) {
            throw new AssertionError("problem reading info from database : " 
                    + e.toString());
        }
        
        return true;
    }
    
    @Override
    public String info() {
        
        if (instance == null) {
            return "no instance";
        }
        String output = "database instance: " + instance + "\n"
                + "number of nodes: " + nNodes + "\n"
                + "vehicles: " + nVehicles;
        return output;
    }
}
