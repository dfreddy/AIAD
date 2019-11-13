package agents;

import jade.core.Agent;
import jade.core.AID;

import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;

import java.util.HashMap;
import java.util.Iterator;

public class BigBrotherAgent extends Agent
{
    protected void setup()
    {
        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                AMSAgentDescription [] agents = null;
                try {
                    SearchConstraints c = new SearchConstraints();
                    c.setMaxResults ((long) -1);
                    agents = AMSService.search( BigBrotherAgent.this, new AMSAgentDescription (), c );
                }
                catch (Exception e) {
                    System.out.println( "Problem searching AMS: " + e );
                    e.printStackTrace();
                }


                StringBuilder airportListString = new StringBuilder();
                for(AMSAgentDescription mAgent : agents){
                    if(mAgent.getName().getName().startsWith("s")){
                        airportListString.append(mAgent.getName().getLocalName()).append(";");
                    }
                }
                sendMessage(agents, airportListString.toString());

            }
        });
    }

    private void sendMessage(AMSAgentDescription [] agents, String airportListString){
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent(airportListString);

        for (AMSAgentDescription agent : agents) {
            if (agent.getName().getLocalName().startsWith("crew_member")) {

                msg.addReceiver(agent.getName());
            }
        }

        //System.out.println( "Big Brother: sending airplane list"   );

        send(msg);
    }
}