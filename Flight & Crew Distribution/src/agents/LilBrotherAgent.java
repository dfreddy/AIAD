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
    private HashMap<Integer, CrewMemberValues> crew_members_values = new HashMap<Integer, CrewMemberValues>(); // Integer is the crew_member id
    private int existingAirplanes;

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
                    // TODO
                    //  export crew_member_values to csv
                }
            }
        });

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.INFORM));

                if(msg != null) {
                    // add/update crew_member_values with the content
                    String[] content = (msg.getContent().split(","));
                    int c_id = Integer.parseInt(content[0]);

                    CrewMemberValues tmp_cmv = new CrewMemberValues(c_id,
                            Float.parseFloat(content[1]), Float.parseFloat(content[2]),
                            Integer.parseInt(content[3]), content[4],
                            Integer.parseInt(content[5]), Float.parseFloat(content[6]));

                    crew_members_values.put(c_id, tmp_cmv);
                    System.out.println("\t\t\t\t\t\t\t\t" + getLocalName() +
                                        " <- received: " + tmp_cmv.toString());
                }
                else block();
            }
        });
    }
}

class CrewMemberValues {
    public int id;
    public float fl_tolerance, crew_patience;
    public int max_waiting_time;
    public String rank;
    public int exp;
    public float happiness;

    public CrewMemberValues(int id, float fl, float cp, int mwt, String r, int exp, float h) {
        this.id = id;
        this.fl_tolerance = fl;
        this.crew_patience = cp;
        this.max_waiting_time = mwt;
        this.rank = r;
        this.exp = exp;
        this.happiness = h;
    }

    public String toString() {
        return "id: " + id + ", fl_tolerance: " + fl_tolerance + ", crew patience: " + crew_patience +
                ", max_waiting_time: " + max_waiting_time + ", rank: " + rank + ", exp: " + exp + ", happiness: " + happiness;
    }
}