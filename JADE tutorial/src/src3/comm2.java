package src3;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.lang.acl.*;

public class comm2 extends Agent {

    String name = "alice";
    AID alice = new AID(name, AID.ISLOCALNAME);

    protected void setup() {
        AgentContainer c = getContainerController();
        try {
            AgentController a = c.createNewAgent(name, "src3.pong", null);
            a.start();
            System.out.println("+++ Created: " + alice);
        } catch (Exception e) {
        }

        addBehaviour(new SimpleBehaviour(this) {
            int n = 0;

            public void action() {
                ACLMessage msg = new ACLMessage(ACLMessage.INFORM);
                msg.setContent("Message #" + n);
                msg.addReceiver(alice);
                System.out.println("+++ Sending: " + n);
                send(msg);
                block(1000);
            }

            public boolean done() {
                return ++n > 3;
            }

        });
    }
}


