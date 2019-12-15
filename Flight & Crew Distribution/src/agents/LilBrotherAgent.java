package agents;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.AMSAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/*
    Keeps track of the personality values of the agents
    Saves them into a csv when there are no more
 */
public class LilBrotherAgent extends Agent {
    private HashMap<Integer, CrewMemberValues> crew_members_values = new HashMap<Integer, CrewMemberValues>(); // Integer is the crew_member id
    private int existingAirplanes, existingCrewMembers;
    private int airplane_counter = 1, crewmember_counter = 1;
    private int max_airplanes = 4, max_crewmembers = 90;
    private PrintWriter writer;
    private ContainerController cc;

    protected void setup()
    {
        cc = this.getContainerController();

        // Creates agents if there's a need for new ones
        addBehaviour(new CyclicBehaviour() {
            public void action() {
                // create agents when there's missing
                existingAirplanes = 0;
                existingCrewMembers = 0;

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

                for(AMSAgentDescription mAgent : agents){
                    if(mAgent.getName().getName().startsWith("crew_member")){
                        existingCrewMembers++;
                    }
                }

                // create airplanes
                while(existingAirplanes < max_airplanes) {
                    String name = "s" + airplane_counter;
                    try {
                        System.out.println(getLocalName() + " <- creating airplane " + name);
                        cc.createNewAgent(name, "agents.Airplane", null).start();
                    } catch (StaleProxyException e) {
                        e.printStackTrace();
                    }
                    airplane_counter++;
                    existingAirplanes++;
                }

                // create crew members
                while(existingCrewMembers < max_crewmembers) {
                    String name = "crew_member" + crewmember_counter;
                    try {
                        // System.out.println(getLocalName() + " <- creating crewmember " + name);
                        cc.createNewAgent(name, "agents.CrewMember", null).start();
                    } catch (StaleProxyException e) {
                        e.printStackTrace();
                    }
                    crewmember_counter++;
                    existingCrewMembers++;
                }

            }
        });

        // Exports crew members to csv
        addBehaviour(new TickerBehaviour(this, 6000) {
            protected void onTick() {
                try {
                    FileWriter f = new FileWriter("crew_members.csv", true);
                    BufferedWriter b = new BufferedWriter(f);
                    writer = new PrintWriter(b);
                }
                catch (IOException e) {e.printStackTrace();}

                System.out.println(getLocalName() + " <- saving crew member values");
                StringBuilder sb = new StringBuilder();
                for (Map.Entry<Integer, CrewMemberValues> entry : crew_members_values.entrySet()) {
                    CrewMemberValues tmp = entry.getValue();
                    sb.append("\n");
                    sb.append(tmp.id);
                    sb.append(",");
                    sb.append(tmp.fl_tolerance);
                    sb.append(",");
                    sb.append(tmp.crew_patience);
                    sb.append(",");
                    sb.append(tmp.max_waiting_time);
                    sb.append(",");
                    sb.append(tmp.rank);
                    sb.append(",");
                    sb.append(tmp.exp);
                    sb.append(",");
                    sb.append(tmp.happiness);
                }
                writer.write(sb.toString());
                writer.close();
                crew_members_values.clear();
            }
        });

        // Checks existing agents
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
                            Integer.parseInt(content[5]), Double.parseDouble(content[6]));

                    if(tmp_cmv.happiness > 0)
                        crew_members_values.put(c_id, tmp_cmv);

                    // System.out.println("\t\t\t\t\t\t\t\t" + getLocalName() + " <- received: " + tmp_cmv.toString());
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
    public double happiness;

    public CrewMemberValues(int id, float fl, float cp, int mwt, String r, int exp, double h) {
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