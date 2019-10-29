/*
package agents;

import jade.core.*;
import jade.core.Runtime;
import jade.lang.acl.*;
import behaviours.*;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class Airport {
    public static void main() throws StaleProxyException {
        Runtime runtime = Runtime.instance();

        Profile profile = new ProfileImpl();
        profile.setParameter(Profile.MAIN_HOST, "localhost");
        profile.setParameter(Profile.GUI, "true");

        ContainerController containerController = runtime.createMainContainer(profile);

        AgentController airplaneController = containerController.createNewAgent("airplane1", "agents.Airplane", null);
        airplaneController.start();
    }
}
*/