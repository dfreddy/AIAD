package agents;

import behaviours.DelayBehaviour;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;

/*
    Keeps track of the personality values of the agents
    Saves them into a csv when there are no more
 */
public class LilBrotherAgent extends Agent {
    private HashMap<Integer, HashMap<String, Integer>> crew_members_values = new HashMap<Integer, HashMap<String, Integer>>(); // Integer is the crew_member id
    private int existingAirplanes;
    private ArrayList existingCrewMembers = new ArrayList();
    private int existingPilots = 0, existingCabinChiefs = 0, existingAttendants = 0;

    protected void setup()
    {
        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                existingAirplanes = 0;

                AMSAgentDescription [] agents = null;
                try {
                    SearchConstraints c = new SearchConstraints();
                    c.setMaxResults ((long) -1);
                    agents = AMSService.search( LilBrotherAgent.this, new AMSAgentDescription (), c );
                }
                catch (Exception e) {
                    System.out.println( "Problem searching AMS: " + e );
                    e.printStackTrace();
                }

                for(AMSAgentDescription mAgent : agents){
                    if(mAgent.getName().getName().startsWith("s")){
                        existingAirplanes++;
                    }
                }

                if(existingAirplanes == 0) {
                    // do export to csv
                }
            }
        });

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));

                if(msg != null) {
                    // add/update crew_member_values with the content
                }
                else block();
            }
        });

        // inform the user of the existing crew members in the market
        SequentialBehaviour seq = new SequentialBehaviour();

        seq.addSubBehaviour(new DelayBehaviour(this, 1000) {
            public void handleElapsedTimeout() {

                System.out.println("");
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" + getLocalName() + " <- PILOTS: " + existingPilots +
                        ", CABIN CHIEFS: " + existingCabinChiefs +
                        ", ATTENDANTS: " + existingAttendants + "\n");

                existingCrewMembers.clear();
                existingAttendants = 0;
                existingCabinChiefs = 0;
                existingPilots = 0;
            }
        });

        seq.addSubBehaviour(new DelayBehaviour(this, 1000) {});

        seq.addSubBehaviour(new TickerBehaviour(this, 4000) {
            protected void onTick() {

                System.out.println("");
                System.out.println("\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t\t" + getLocalName() + " <- PILOTS: " + existingPilots +
                        ", CABIN CHIEFS: " + existingCabinChiefs +
                        ", ATTENDANTS: " + existingAttendants + "\n");

                existingCrewMembers.clear();
                existingAttendants = 0;
                existingCabinChiefs = 0;
                existingPilots = 0;
            }
        });

        addBehaviour(seq);
    }

    private void sendMessage(AMSAgentDescription [] agents, String airportListString){
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent(airportListString);

        for (AMSAgentDescription agent : agents) {
            if (agent.getName().getLocalName().startsWith("crew_member")) {
                msg.addReceiver(agent.getName());
            }
        }


        send(msg);
    }
}