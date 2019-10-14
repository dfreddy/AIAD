package src3;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;

public class receiver extends Agent {

    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null)
                    System.out.println(" - " +
                            myAgent.getLocalName() + " received: " +
                            msg.getContent());
                block();
            }
        });
    }
}
