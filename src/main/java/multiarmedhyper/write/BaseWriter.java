
package multiarmedhyper.write;

import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import multiarmedhyper.algorithm.Algorithm;

public class BaseWriter {
    
    // info DB
    private final String user;
    private final String pass;
    private final String host;
    
    public BaseWriter(String[] param) {
        
        // db parameters
        if (param.length > 2) {
            this.host = param[0];
            this.user = param[1];
            this.pass = param[2];
        } else {
            this.host = "localhost/hyper";
            this.user = "felipe";
            this.pass = "admin";
        }
    }
    
    public boolean write(Algorithm algorithm, int ejec) {
        
        if (algorithm == null) {
            return false;
        }
        
        try {
            
            // parameters connection
            Properties props = new Properties();
            props.setProperty("user", user);
            props.setProperty("password", pass);
            
            try (Connection connPostgres 
                    = DriverManager.getConnection("jdbc:postgresql://" + host, props)) {
                
                // insert result
                String instance = algorithm.getParam().getInstance();
                String srtAlg = algorithm.algorithmName();
                PreparedStatement prep = connPostgres.prepareStatement(
                        "INSERT INTO result (instance, algorithm, ejec, objective, "
                                + "runtime, iterations, status) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?) "
                                + "ON CONFLICT ON CONSTRAINT result_pkey "
                                + "DO UPDATE SET objective = EXCLUDED.objective, "
                                + "runtime = EXCLUDED.runtime, "
                                + "iterations = EXCLUDED.iterations, "
                                + "status = EXCLUDED.status");
                prep.setString(1, instance);
                prep.setString(2, srtAlg);
                prep.setInt(3, ejec);
                prep.setDouble(4, algorithm.getTotalCost());
                prep.setDouble(5, algorithm.getRunTime());
                prep.setInt(6, algorithm.getIterations());
                if (algorithm.isFeasible()) {
                    prep.setString(7, "Feasible");
                } else {
                    prep.setString(7, "No Feasible");
                }
                
                prep.execute();
                
                // remove old solution
                PreparedStatement remove = connPostgres.prepareStatement(
                        "DELETE FROM solution WHERE instance = ? AND "
                                + "algorithm = ? AND ejec = ?");
                remove.setString(1, instance);
                remove.setString(2, srtAlg);
                remove.setInt(3, ejec);
                remove.execute();
                
                // insert routes
                PreparedStatement details = connPostgres.prepareStatement(
                        "INSERT INTO solution (instance, algorithm, ejec, route, position, "
                                + "location, time) VALUES (?, ?, ?, ?, ?, ?, ?)");
                List<List<Integer>> routes = algorithm.getBestRoutes();
                List<List<Double>> times = algorithm.getBestTimes();
                
                if (routes != null) {
                    for (int i = 0; i < routes.size(); i++) {
                        List<Integer> route = routes.get(i);
                        List<Double> time = times.get(i);
                        for (int j = 0; j < route.size(); j++) {
                            
                            // set prep
                            details.setString(1, instance);
                            details.setString(2, srtAlg);
                            details.setInt(3, ejec);
                            details.setInt(4, i);
                            details.setInt(5, j);
                            details.setInt(6, route.get(j));
                            details.setDouble(7, time.get(j));
                            details.addBatch();
                        }
                    }
                    details.executeBatch();
                }
                
                // insert transition
                remove = connPostgres.prepareStatement(
                        "DELETE FROM transition WHERE instance = ? AND "
                                + "algorithm = ? AND ejec = ?");
                remove.setString(1, instance);
                remove.setString(2, srtAlg);
                remove.setInt(3, ejec);
                remove.execute();
                
                details = connPostgres.prepareStatement(
                        "INSERT INTO transition (instance, algorithm, ejec, low_level1, "
                                + "low_level2, probability) VALUES (?, ?, ?, ?, ?, ?)");
                Table<Integer, Integer, Double> transition = algorithm.getTransitions();
                if (transition != null && !transition.isEmpty()) {
                    for (Cell<Integer, Integer, Double> cell : transition.cellSet()) {
                        
                        // set prep
                        details.setString(1, instance);
                        details.setString(2, srtAlg);
                        details.setInt(3, ejec);
                        details.setInt(4, cell.getRowKey());
                        details.setInt(5, cell.getColumnKey());
                        details.setDouble(6, cell.getValue());
                        details.addBatch();
                    }
                    details.executeBatch();
                }
                
                // insert apply
                remove = connPostgres.prepareStatement(
                        "DELETE FROM apply WHERE instance = ? AND "
                                + "algorithm = ? AND ejec = ?");
                remove.setString(1, instance);
                remove.setString(2, srtAlg);
                remove.setInt(3, ejec);
                remove.execute();
                
                details = connPostgres.prepareStatement(
                        "INSERT INTO apply (instance, algorithm, ejec, low_level, probability) "
                                + "VALUES (?, ?, ?, ?, ?)");
                Map<Integer, Double> map = algorithm.getApply();
                if (map != null && !map.isEmpty()) {
                    for (Integer key : map.keySet()) {
                        
                        // set prep
                        details.setString(1, instance);
                        details.setString(2, srtAlg);
                        details.setInt(3, ejec);
                        details.setInt(4, key);
                        details.setDouble(5, map.get(key));
                        details.addBatch();
                    }
                    details.executeBatch();
                }
                
                // insert output
                PreparedStatement output = connPostgres.prepareStatement(
                        "INSERT INTO output (instance, algorithm, ejec, output) "
                                + "VALUES (?, ?, ?, ?) "
                                + "ON CONFLICT ON CONSTRAINT output_pkey "
                                + "DO UPDATE SET output = EXCLUDED.output");
                output.setString(1, instance);
                output.setString(2, srtAlg);
                output.setInt(3, ejec);
                output.setString(4, algorithm.getOutput());
                output.execute();
                
                connPostgres.close();
            }
            
        } catch (SQLException e) {
            throw new AssertionError("problem writting info to database : " 
                    + e.toString());
        }
        
        return true;
    }
}
