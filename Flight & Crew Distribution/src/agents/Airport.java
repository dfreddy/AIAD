//
// re-adapted jade.Boot
//

package agents;

import jade.core.ProfileException;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.util.ExtendedProperties;
import jade.util.Logger;
import jade.util.leap.Properties;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

public class Airport {
    public static final String DEFAULT_FILENAME = "leap.properties";
    private static Logger logger = Logger.getMyLogger("jade.Boot");

    public static void main(String[] args) {
        try {
            ProfileImpl p = null;
            if (args.length > 0) {
                if (args[0].startsWith("-")) {
                    Properties pp = parseCmdLineArgs(args);
                    if (pp == null) {
                        return;
                    }

                    p = new ProfileImpl(pp);
                } else {
                    p = new ProfileImpl(args[0]);
                }
            } else {
                p = new ProfileImpl("leap.properties");
            }

            Runtime.instance().setCloseVM(true);
            if (p.getBooleanProperty("main", true)) {
                ContainerController containerController = Runtime.instance().createMainContainer(p);


                AgentController bigBrotherController = containerController.createNewAgent("big_brother", "agents.BigBrotherAgent", null);
                bigBrotherController.start();

                // create agents: 1 airplane "s1" and 1 crew member "crew_member"
                AgentController airplaneController = containerController.createNewAgent("s1", "agents.Airplane", null);
                airplaneController.start();
                airplaneController = containerController.createNewAgent("s2", "agents.Airplane", null);
                airplaneController.start();
                airplaneController = containerController.createNewAgent("s3", "agents.Airplane", null);
                airplaneController.start();
                airplaneController = containerController.createNewAgent("s4", "agents.Airplane", null);
                airplaneController.start();

                for(int i=0; i<100; i++) {
                    String name = "crew_member" + i;
                    AgentController crewmemberController = containerController.createNewAgent(name, "agents.CrewMember", null);
                    crewmemberController.start();
                }

            } else {
                Runtime.instance().createAgentContainer(p);
            }
        } catch (ProfileException var3) {
            System.err.println("Error creating the Profile [" + var3.getMessage() + "]");
            var3.printStackTrace();
            printUsage();
            System.exit(-1);
        } catch (IllegalArgumentException var4) {
            System.err.println("Command line arguments format error. " + var4.getMessage());
            var4.printStackTrace();
            printUsage();
            System.exit(-1);
        } catch (StaleProxyException e) {
            System.err.println("Stale Proxy Exception");
            e.printStackTrace();
        }
    }

    public Airport() {
    }

    public static void testCrewPersonality(){
        int s = 0, m=0, l=0, d=0;

        for (int i = 0; i < 10000; i++){
            Airplane b1 = new Airplane();
            b1.generateFlightSpecification();
            b1.attributeFlightType();

            CrewMember a1 = new CrewMember();
            a1.defineCrewRank();
            a1.calculateExperience();
            a1.calculateMaxMinOffer(b1.flightsTime, b1.connectionTime);


            System.out.println("Ite: " + i);
            System.out.println("FlightTime: " + b1.flightsTime);
            System.out.println("ConnectionTime: " + b1.connectionTime);
            System.out.println("TotalFlightTime: " + b1.totalFlightTime);
            System.out.println("Rank: " + a1.rank);
            System.out.println("Experience: " + a1.experience);
            double airMaxOffer = b1.calculateMaxOffer(a1.experience, a1.rank);
            System.out.println("AirlineMaxOffer: " + airMaxOffer);
            System.out.println("CrewMaxOffer: " + a1.maxOffer);
            System.out.println("CrewMinOffer: " + a1.minOffer);

            if(a1.rank == "PILOT"){ //short term flight
                s++;
            }
            else if(a1.rank == "CABIN_CHIEF"){ //mid term flight
                m++;
            }
            else{ // long term flight
                l++;
            }

            if(airMaxOffer < a1.minOffer)
                d++;

            System.out.println();
        }

        System.out.println();
        System.out.println("Nr of Pilots: " + s);
        System.out.println("Nr of Cabin Chiefs: " + m);
        System.out.println("Nr of Attendants: " + l);
        System.out.println("Nr of Airline Offer < CrewMinOffer: " + d);


    }

    public static void testAirlinePersonality(){
        int s = 0, m=0, l=0;

        for (int i = 0; i < 10000; i++){
            Airplane a1 = new Airplane();
            a1.generateFlightSpecification();
            a1.attributeFlightType();


            System.out.println("Ite: " + i);
            System.out.println("FlightTime: " + a1.flightsTime);
            System.out.println("ConnectionTime: " + a1.connectionTime);
            System.out.println("TotalFlightTime: " + a1.totalFlightTime);
            System.out.println("FlightType: " + a1.flightType);
            System.out.println("MaxOffer Pilot 30: " + a1.calculateMaxOffer(30, "PILOT"));
            System.out.println("MaxOffer Cabin-Chief: " + a1.calculateMaxOffer(30, "CABIN_CHIEF"));
            System.out.println("MaxOffer Attendant: " + a1.calculateMaxOffer(30, "ATTENDANT"));
            System.out.println("MaxOffer Pilot 80: " + a1.calculateMaxOffer(80, "PILOT"));
            System.out.println("MaxOffer Cabin-Chief: " + a1.calculateMaxOffer(80, "CABIN_CHIEF"));
            System.out.println("MaxOffer Attendant: " + a1.calculateMaxOffer(80, "ATTENDANT"));



            if(a1.flightType == "SHORT"){ //short term flight
                s++;
            }
            else if(a1.flightType == "MEDIUM"){ //mid term flight
                m++;
            }
            else{ // long term flight
                l++;
            }


            System.out.println();
        }

        System.out.println();
        System.out.println("Nr of ShortFlights: " + s);
        System.out.println("Nr of MediumFlights: " + m);
        System.out.println("Nr of LongFlights: " + l);

    }

    public static Properties parseCmdLineArgs(String[] args) throws IllegalArgumentException {
        Properties props = new ExtendedProperties();
        int i = 0;

        while(true) {
            if (i < args.length) {
                String name;
                if (args[i].startsWith("-")) {
                    if (args[i].equalsIgnoreCase("-version")) {
                        logger.log(Logger.INFO, "----------------------------------\n" + Runtime.getCopyrightNotice() + "----------------------------------------");
                        return null;
                    }

                    if (args[i].equalsIgnoreCase("-help")) {
                        printUsage();
                        return null;
                    }

                    if (args[i].equalsIgnoreCase("-container")) {
                        props.setProperty("main", "false");
                    } else if (args[i].equalsIgnoreCase("-backupmain")) {
                        props.setProperty("backupmain", "true");
                    } else if (args[i].equalsIgnoreCase("-gui")) {
                        props.setProperty("gui", "true");
                    } else if (args[i].equalsIgnoreCase("-nomtp")) {
                        props.setProperty("nomtp", "true");
                    } else if (args[i].equalsIgnoreCase("-name")) {
                        ++i;
                        if (i >= args.length) {
                            throw new IllegalArgumentException("No platform name specified after \"-name\" option");
                        }

                        props.setProperty("platform-id", args[i]);
                    } else if (args[i].equalsIgnoreCase("-mtp")) {
                        ++i;
                        if (i >= args.length) {
                            throw new IllegalArgumentException("No mtps specified after \"-mtp\" option");
                        }

                        props.setProperty("mtps", args[i]);
                    } else if (args[i].equalsIgnoreCase("-conf")) {
                        ++i;
                        if (i >= args.length) {
                            throw new IllegalArgumentException("No configuration file name specified after \"-conf\" option");
                        }

                        try {
                            props.load(args[i]);
                        } catch (Exception var4) {
                            if (logger.isLoggable(Logger.SEVERE)) {
                                logger.log(Logger.SEVERE, "WARNING: error loading properties from file " + args[i] + ". " + var4);
                            }
                        }
                    } else {
                        name = args[i].substring(1);
                        ++i;
                        if (i >= args.length) {
                            throw new IllegalArgumentException("No value specified for property \"" + name + "\"");
                        }

                        props.setProperty(name, args[i]);
                    }

                    ++i;
                    continue;
                }

                if (props.getProperty("agents") != null && logger.isLoggable(Logger.WARNING)) {
                    logger.log(Logger.WARNING, "WARNING: overriding agents specification set with the \"-agents\" option");
                }

                name = args[i];
                props.setProperty("agents", args[i]);
                ++i;
                if (i < args.length) {
                    if (logger.isLoggable(Logger.WARNING)) {
                        logger.log(Logger.WARNING, "WARNING: ignoring command line argument " + args[i] + " occurring after agents specification");
                    }

                    if (name != null && name.indexOf(40) != -1 && !name.endsWith(")") && logger.isLoggable(Logger.WARNING)) {
                        logger.log(Logger.WARNING, "Note that agent arguments specifications must not contain spaces");
                    }

                    if (args[i].indexOf(58) != -1 && logger.isLoggable(Logger.WARNING)) {
                        logger.log(Logger.WARNING, "Note that agent specifications must be separated by a semicolon character \";\" without spaces");
                    }
                }
            }

            if ("true".equals(props.getProperty("nomtp")) && props.getProperty("mtps") != null) {
                if (logger.isLoggable(Logger.WARNING)) {
                    logger.log(Logger.WARNING, "WARNING: both \"-mtps\" and \"-nomtp\" options specified. The latter will be ignored");
                }

                props.remove("nomtp");
            }

            return props;
        }
    }

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("java -cp <classpath> jade.Boot [options] [agents]");
        System.out.println("Main options:");
        System.out.println("    -container");
        System.out.println("    -gui");
        System.out.println("    -name <platform name>");
        System.out.println("    -host <main container host>");
        System.out.println("    -port <main container port>");
        System.out.println("    -local-host <host where to bind the local server socket on>");
        System.out.println("    -local-port <port where to bind the local server socket on>");
        System.out.println("    -conf <property file to load configuration properties from>");
        System.out.println("    -services <semicolon separated list of service classes>");
        System.out.println("    -mtps <semicolon separated list of mtp-specifiers>");
        System.out.println("     where mtp-specifier = [in-address:]<mtp-class>[(comma-separated args)]");
        System.out.println("    -<property-name> <property-value>");
        System.out.println("Agents: [-agents] <semicolon separated list of agent-specifiers>");
        System.out.println("     where agent-specifier = <agent-name>:<agent-class>[(comma separated args)]");
        System.out.println();
        System.out.println("Look at the JADE Administrator's Guide for more details");
    }
}
