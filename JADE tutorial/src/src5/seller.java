package src5;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.Random;

public class seller extends Agent {
    Random rnd = newRandom();
    MessageTemplate template =
            MessageTemplate.MatchPerformative( ACLMessage.QUERY_REF );

    ACLMessage reply;

    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive( template );
                if (msg!=null) {
                    // we create the reply
                    reply = msg.createReply();
                    reply.setPerformative( ACLMessage.INFORM );
                    int dollar = rnd.nextInt(100);
                    reply.setContent("" + dollar);

                    int delay = rnd.nextInt( 1000 );
                    System.out.println(myAgent.getLocalName() + " <- QUERY from " +
                            msg.getSender().getLocalName() +
                            ". Will answer in " + delay + "ms with $" + dollar);

                    // but only send it after a random delay
                    addBehaviour(new delayBehaviour( myAgent, delay) {
                        public void handleElapsedTimeout() {
                            send(reply);
                        }
                    });
                }
                else block(); // only block() if no msg is received, otherwise some messages may be ignored
            }
        });
    }

// ==========================================
// ========== Utility methods ===============
// ==========================================


//  --- generating distinct Random generator -------------------

    Random newRandom()
    {	return  new Random( hashCode() + System.currentTimeMillis()); }

}

