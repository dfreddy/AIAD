package agents;

import jade.core.Agent;
import jade.core.AID;

import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.*;

public class BigBrotherAgent extends Agent
{
    protected void setup()
    {
        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                AMSAgentDescription [] agents = null;
                try {
                    SearchConstraints c = new SearchConstraints();
                    c.setMaxResults (new Long(-1));
                    agents = AMSService.search( BigBrotherAgent.this, new AMSAgentDescription (), c );
                }
                catch (Exception e) {
                    System.out.println( "Problem searching AMS: " + e );
                    e.printStackTrace();
                }

                AID myID = getAID();
                for (int i=0; i<agents.length;i++)
                {
                    AID agentID = agents[i].getName();
                    System.out.println(
                            ( agentID.equals( myID ) ? "*** " : "    ")
                                    + i + ": " + agentID.getName()
                    );
                }
            }
        });
    }
}