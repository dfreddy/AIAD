package agents;

import behaviours.DelayBehaviour;
import jade.core.Agent;
import jade.core.AID;

import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/*
    Keeps track of existing agents in the system
    Relays this information to the agents that need it
 */
public class BigBrotherAgent extends Agent {
    private ArrayList existingCrewMembers = new ArrayList();
    private int existingPilots = 0, existingCabinChiefs = 0, existingAttendants = 0;

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
                // System.out.println( getLocalName() + " <- airplane list: " + airportListString );
                sendMessage(agents, airportListString.toString());

            }
        });

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));

                if(msg != null && !existingCrewMembers.contains(msg.getSender().getLocalName())) {
                    existingCrewMembers.add(msg.getSender().getLocalName());
                    switch(msg.getContent()) {
                        case "PILOT":
                            existingPilots++;
                            break;
                        case "CABIN_CHIEF":
                            existingCabinChiefs++;
                            break;
                        case "ATTENDANT":
                            existingAttendants++;
                            break;
                        default:
                            break;
                    }

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

        seq.addSubBehaviour(new TickerBehaviour(this, 5000) {
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