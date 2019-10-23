package src5;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.Random;

class transaction extends SequentialBehaviour {
    Random rnd = newRandom();
    ACLMessage msg, reply;
    String ConvID;
    int price;

    public transaction(Agent a, ACLMessage msg, int price) {
        super(a);
        this.msg = msg;
        ConvID = msg.getConversationId();
        this.price = price;
    }

    public void onStart() {
        addSubBehaviour( new delayBehaviour( myAgent, rnd.nextInt( 1000 )) {
            public void handleElapsedTimeout() {
                System.out.println(myAgent.getLocalName() + " <- QUERY from " +
                        msg.getSender().getLocalName() +
                        ". Will answer with $" + price);

                reply = msg.createReply();
                reply.setPerformative( ACLMessage.INFORM );
                reply.setContent("" + price);
                myAgent.send(reply);
            }
        });

        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchPerformative( ACLMessage.REQUEST ),
                MessageTemplate.MatchConversationId( ConvID ));

        addSubBehaviour(new receiverBehaviour(myAgent, 2000, template) {
            public void handle( ACLMessage msg1) {
                if (msg1 != null ) {
                    int offer = Integer.parseInt(msg1.getContent());

                    // RANDOM BASED BARTERING
                    reply = msg1.createReply();
                    if (offer >= rnd.nextInt(price))
                        reply.setPerformative(ACLMessage.AGREE);
                    else {
                        reply.setPerformative(ACLMessage.REFUSE);
                    }
                    myAgent.send(reply);
                }
                else {
                    System.out.println("Timeout!! $" + price +
                            " from " + myAgent.getLocalName() +
                            " is no longer valid");
                }
            }
        });
    }

    Random newRandom() {
        return new Random( hashCode() + System.currentTimeMillis());
    }
}