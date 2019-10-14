package src3;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.*;

public class mainTemplate extends Agent {
    MessageTemplate mt1 =
            MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchSender(new AID("a1",
                            AID.ISLOCALNAME)));

    protected void setup() {
        // Send messages to "a1" and "a2"
        ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
        msg.setContent("Ping");
        for (int i = 1; i <= 2; i++)
            msg.addReceiver(new AID("a" + i, AID.ISLOCALNAME));

        send(msg);

        // Set-up Behaviour 1
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                System.out.print("Behaviour ONE: ");
                ACLMessage msg = receive(mt1);
                if (msg != null)
                    System.out.println("gets "
                            + msg.getPerformative() + " from "
                            + msg.getSender().getLocalName() + " ="
                            + msg.getContent());
                else {
                    System.out.println("gets NULL");
                    block();
                }
            }
        });

        // Set-up Behaviour 2
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                System.out.print("Behaviour TWO: ");
                ACLMessage msg = receive();
                if (msg != null)
                    System.out.println("gets "
                            + msg.getPerformative() + " from "
                            + msg.getSender().getLocalName() + " ="
                            + msg.getContent());
                else {
                    System.out.println("gets NULL");
                    block();
                }
            }
        });
    }
}
