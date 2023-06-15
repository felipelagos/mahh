
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
        Integer seed = null;
        
        // configuation
        double delta = 0.025;
        double eta = 2.0;
        double alpha = -1.0;
        double beta = -1.0;
        
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
                case "--seed=": 
                    try {
                        seed = Integer.parseInt(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Seed integer incorrect.\n" 
                                + e.toString());
                    }
                    break;
                case "--ejec=": 
                    try {
                        ejec = Integer.parseInt(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Execution parameter incorrect.\n" 
                                + e.toString());
                    }
                    break;
                case "--time=": 
                    try {
                        limtime = Double.parseDouble(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Runing time incorrect.\n" 
                                + e.toString());
                    }
                    limTimeNumberOfNodes = false;
                    break;
                case "--iterations=": 
                    try {
                        limIter = Integer.parseInt(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Iterations limit incorrect.\n" 
                                + e.toString());
                    }
                    limTimeNumberOfNodes = false;
                    break;
                case "--delta=": 
                    try {
                        delta = Double.parseDouble(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Delta parameter wrong.\n" 
                                + e.toString());
                    }
                    break;
                case "--eta=": 
                    try {
                        eta = Double.parseDouble(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Eta parameter wrong.\n" 
                                + e.toString());
                    }
                    break;
                case "--alpha=": 
                    try {
                        alpha = Double.parseDouble(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Alpha parameter wrong.\n" 
                                + e.toString());
                    }
                    break;
                case "--beta=": 
                    try {
                        beta = Double.parseDouble(parameter);
                    }
                    catch (NumberFormatException e) {
                        throw new AssertionError("Beta parameter wrong.\n" 
                                + e.toString());
                    }
                    break;
                case "--help":
                    System.out.println("\nMulti-Armed Bandit Hyper Heuristics "
                            + "for the Vehicle Routing Problem with Time Windows\n"
                            + "usage: java -jar runhyper [options]\n\n"
                            + "Options:\n"
                            + "--file=<arg>\t\t\t Input file instance. The file must follow the Solomon instances' structure.\n" 
                            + "--instance=<arg>\t\t Instance name when using DB postgresql.\n"
                            + "--host=<arg>\t\t\t Parameters for DB postgresql connection (host:port/database).\n"
                            + "--user=<arg>\t\t\t User for DB postgresql connection.\n"
                            + "--pass=<arg>\t\t\t Password for DB postgresql connection.\n"
                            + "--text\t\t\t\t The program runs printing the output in the terminal.\n"
                            + "--no-text\t\t\t The program runs printing nothing in the terminal.\n"
                            + "--one-digit\t\t\t Instance matrix distance is computed truncating the first decimal digit.\n"
                            + "--doble-pres\t\t\t Instance matrix distance is computed rounding to the second decimal digit.\n"
                            + "--auto-time\t\t\t Running time is defined according to the number of customers.\n"
                            + "--type=<arg>\t\t\t MAHH algorithm. ExpoHyper[0]; KheiriHyper[1]; KheiriHyperAll[2]; "
                            + "ThompsonHyper[3]; ExpoRecord[4]; ALNS[5]; ANLS+[6]; test algorithms[7-11].\n"
                            + "--seed=<arg>\t\t\t Long (integer) for random seed.\n"
                            + "--ejec=<arg>\t\t\t Integer for algorithm execution number for computational study. \n"
                            + "--time=<arg>\t\t\t Running time limit in seconds. \n"
                            + "--iterations=<arg>\t\t Integer for iterations limit.\n"
                            + "--delta=<arg>\t\t\t Numeric value for delta parameter. This parameter determines the acceptance solution tolerance.\n"
                            + "--eta=<arg>\t\t\t Numeric value for eta parameter for Expo Hyper algorithms (learning rate).\n"
                            + "--alpha=<arg>\t\t\t Numeric value for alpha parameter for Expo Hyper algorithms (minimum transition probability).\n"
                            + "--beta=<arg>\t\t\t Numeric value for beta parameter for Expo Hyper algorithms (minimum acceptance probability).\n"
                            + "--help\t\t\t\t Display help information.");
                    System.exit(0);
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
            if (alpha > 0 && alpha < 1) {
                ((ExpoHyper) algorithm).setAlpha(alpha);
            }
            if (beta > 0 && beta < 1) {
                ((ExpoHyper) algorithm).setBeta(beta);
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
        if (seed == null) {
            seed = type * 10000 + (int) Math.floor(delta * 1000) 
                    + (int) Math.floor(eta * 100) + ejec + instance.hashCode();
        }
        algorithm.setSeed(seed);
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
