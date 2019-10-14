package src3;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.AMSService;
import jade.domain.FIPAAgentManagement.*;
import jade.lang.acl.*;

public class comm1 extends Agent {
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

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("Ping");

        for (int i = 0; i < agents.length; i++)
            msg.addReceiver(agents[i].getName());

        send(msg);

        System.out.println(this.getLocalName() + " sent " + msg.getContent());

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null)
                    System.out.println(myAgent.getLocalName() + " got Answer" + " <- "
                            + msg.getContent() + " from "
                            + msg.getSender().getName());
                block();
            }
        });


    }
}