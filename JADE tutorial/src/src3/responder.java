package src3;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;

public class responder extends Agent {
    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive();
                if (msg != null) {

                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    reply.setContent(" Gossip.....");
                    send(reply);

                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(" Really sexy stuff... cheap! ");
                    send(reply);
                }
                block();
            }
        });
    }
}