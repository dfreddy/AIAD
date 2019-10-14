package src3;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;

public class sender extends Agent {
    protected void setup() {
        // First set-up answering behaviour

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null)
                    System.out.println(" - " +
                            myAgent.getLocalName() + " received: "
                            + msg.getContent() + " from "
                            + msg.getSender().getName());
                block();
            }
        });

        // Send messages to "a1" and "a2"

        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("Ping");
        int n_agents = 2;
        for (int i = n_agents; i > 0; --i)
            msg.addReceiver(new AID("a" + i, AID.ISLOCALNAME));

        send(msg);

    }
}