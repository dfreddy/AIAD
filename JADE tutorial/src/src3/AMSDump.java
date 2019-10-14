package src3;

import jade.core.Agent;
import jade.core.AID;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.*;

public class AMSDump extends Agent {
    protected void setup() {
        AMSAgentDescription[] agents = null;
        try {
            SearchConstraints c = new SearchConstraints();
            c.setMaxResults((long) -1);
            agents = AMSService.search(this, new AMSAgentDescription(), c);
        } catch (Exception e) {
            System.out.println("Problem searching AMS: " + e);
            e.printStackTrace();
        }

        AID myID = getAID();
        for (int i = 0; i < agents.length; i++) {
            AID agentID = agents[i].getName();
            System.out.println(
                    (agentID.equals(myID) ? "*** " : "    ")
                            + i + ": " + agentID.getName()
            );
        }
        doDelete();
        System.exit(0);
    }

}//end class AMSDump