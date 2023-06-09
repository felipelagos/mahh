
package multiarmedhyper;

import multiarmedhyper.algorithm.AdaptiveImproved;
import multiarmedhyper.algorithm.AdaptiveLargeNeighborhoodSearch;
import multiarmedhyper.algorithm.Algorithm;
import multiarmedhyper.algorithm.ExpoHyper;
import multiarmedhyper.algorithm.ExpoHyperRecord;
import multiarmedhyper.algorithm.IterExpo;
import multiarmedhyper.algorithm.KheiriHyper;
import multiarmedhyper.algorithm.KheiriHyperAll;
import multiarmedhyper.algorithm.TestExpoHyper;
import multiarmedhyper.algorithm.TestKheiriHyper;
import multiarmedhyper.algorithm.TestThompsonHyper;
import multiarmedhyper.algorithm.ThompsonHyper;
import multiarmedhyper.data.BaseReader;
import multiarmedhyper.data.InstanceReader;
import multiarmedhyper.data.ParameterReader;
import multiarmedhyper.write.BaseWriter;

public class MultiArmedHyper {

    public static void main(String[] args) {
        
        // instance parameters
        String[] dataConn = new String[3];
        String instance = "R110_6";
        String file = "instances/gehring/R110_6.TXT";
        
        // algorithm parameters
        int type = 0;
        double limtime = 300.0;
        int limIter = -1;
        boolean text = true;
        boolean typeDouble = false;
        boolean limTimeNumberOfNodes = false;
        int ejec = 1;
        
        // configuation
        double delta = 0.025;
        double eta = 2.0;
        double alpha = -1.0;
        
        // process arguments
        for (String arg : args) {
            String set;
            String parameter;
            if (arg.contains("=")) {
                set = arg.substring(0, arg.indexOf("=") + 1);
                parameter = arg.substring(arg.indexOf("=") + 1);
            } else {
                set = arg;
                parameter = "";
            }        
            switch (set) {
                case "--file=": 
                    file = parameter;
                    text = true;
                    break;
                case "--instance=": 
                    instance = parameter;
                    break;
                case "--host=": 
                    dataConn[0] = parameter;
                    break;
                case "--user=": 
                    dataConn[1] = parameter;
                    break;
                case "--pass=": 
                    dataConn[2] = parameter;
                    break;
                case "--text": 
                    text = true;
                    break;
                case "--no-text": 
                    text = false;
                    break;
                case "--one-digit": 
                    typeDouble = false;
                    break;
                case "--doble-pres": 
                    typeDouble = true;
                    break;
                case "--auto-time": 
                    limTimeNumberOfNodes = true;
                    break;
                case "--type=": 
                    try {
                        type = Integer.parseInt(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Type parameter wrong.\n" 
                                + e.toString());
                    }
                    break;
                case "--ejec=": 
                    try {
                        ejec = Integer.parseInt(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Type parameter wrong.\n" 
                                + e.toString());
                    }
                    break;
                case "--time=": 
                    try {
                        limtime = Double.parseDouble(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Runing time parameters wrong.\n" 
                                + e.toString());
                    }
                    limTimeNumberOfNodes = false;
                    break;
                case "--iterations=": 
                    try {
                        limIter = Integer.parseInt(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Runing time parameters wrong.\n" 
                                + e.toString());
                    }
                    limTimeNumberOfNodes = false;
                    break;
                case "--delta=": 
                    try {
                        delta = Double.parseDouble(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Type parameter wrong.\n" 
                                + e.toString());
                    }
                    break;
                case "--eta=": 
                    try {
                        eta = Double.parseDouble(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Type parameter wrong.\n" 
                                + e.toString());
                    }
                    break;
                case "--alpha=": 
                    try {
                        alpha = Double.parseDouble(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Type parameter wrong.\n" 
                                + e.toString());
                    }
                    break;
                default:
                    throw new AssertionError("Incorrect option.");
            }
        }
        
        // read instance
        ParameterReader param;
        if (dataConn[0] != null) {
            param = new BaseReader(instance, dataConn);
        } else {
            param = new InstanceReader(file);
        } 
        param.setDistanceType(typeDouble);
        param.read();
        
        // set algorithm 
        Algorithm algorithm;
        switch (type) {
            case 0 -> {
                algorithm = new ExpoHyper(param);
            }
            case 1 -> {
                algorithm = new KheiriHyper(param);
            }
            case 2 -> {
                algorithm = new KheiriHyperAll(param);
            }
            case 3 -> {
                algorithm = new ThompsonHyper(param);
            }
            case 4 -> {
                algorithm = new ExpoHyperRecord(param);
            }
            case 5 -> {
                algorithm = new AdaptiveLargeNeighborhoodSearch(param);
            }
            case 6 -> {
                algorithm = new AdaptiveImproved(param);
            }
            case 7 -> {
                algorithm = new TestExpoHyper(param);
            }
            case 8 -> {
                algorithm = new TestKheiriHyper(param);
            }
            case 9 -> {
                algorithm = new TestThompsonHyper(param);
            }
            case 10 -> {
                algorithm = new IterExpo(param);
                limIter = 25000;
            }
            case 11 -> {
                algorithm = new IterExpo(param);
                ((IterExpo) algorithm).setName("FastExpo");
                limIter = 5000;
            }
            default -> algorithm = new ExpoHyper(param);
        }
        
        // configuration
        algorithm.setDelta(delta);
        if (algorithm instanceof ExpoHyper || algorithm instanceof ExpoHyperRecord) {
            ((ExpoHyper) algorithm).setEta(eta);
            if (alpha > 0) {
                ((ExpoHyper) algorithm).setAlpha(alpha);
            }
        }
        
        // max time running
        algorithm.setTimeLimit(10 * 60 * 60);
        
        // auto configuration
        if (limTimeNumberOfNodes) {
            
            int nodes = param.getNodes().size();
            if (nodes <= 120) {
                algorithm.setTimeLimit(10 * 60);
            } else if (nodes <= 220) {
                algorithm.setTimeLimit(15 * 60);
            } else if (nodes <= 320) {
                algorithm.setTimeLimit(20 * 60);
            } else if (nodes <= 420) {
                algorithm.setTimeLimit(25 * 60);
            } else if (nodes <= 520) {
                algorithm.setTimeLimit(30 * 60);
            } else if (nodes <= 620) {
                algorithm.setTimeLimit(35 * 60);
            } else if (nodes <= 720) {
                algorithm.setTimeLimit(40 * 60);
            } else if (nodes <= 820) {
                algorithm.setTimeLimit(45 * 60);
            } else if (nodes <= 920) {
                algorithm.setTimeLimit(50 * 60);
            } else {
                algorithm.setTimeLimit(55 * 60);
            }
            
            // irrestricted iterations
            algorithm.setIterLimit(Integer.MAX_VALUE);
        } else {
            algorithm.setTimeLimit(limtime);
        }
        
        // limit iterations
        if (limIter > 0) {
            algorithm.setIterLimit(limIter);
            algorithm.setTimeLimit(10 * 60 * 60);
        } else {
            algorithm.setIterLimit(Integer.MAX_VALUE);
        }
        
        // set algorithm and solve
        algorithm.setSeed(type * 10000 + (int) Math.floor(delta * 1000) 
                + (int) Math.floor(eta * 100) + ejec + instance.hashCode());
        algorithm.setOutput(text);
        algorithm.solve();
        
        // write output in DB
        if (dataConn[0] != null) {
            BaseWriter writer = new BaseWriter(dataConn);
            writer.write(algorithm, ejec);
        }
        
        // clear parameters
        param.clear();
    }
}
