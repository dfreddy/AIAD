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

                // create agents: 1 airplane "s1" and 1 crew member "crew_member"
                AgentController airplaneController = containerController.createNewAgent("s1", "agents.Airplane", null);
                airplaneController.start();
                AgentController crewmemberController = containerController.createNewAgent("crew_member", "agents.CrewMember", null);
                crewmemberController.start();

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
