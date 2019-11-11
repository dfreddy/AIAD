package behaviours;

import behaviours.*;
import behaviours.ReceiverBehaviour;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.Random;

public class Transaction extends SequentialBehaviour {
    Random rnd = newRandom();
    ACLMessage msg, reply, barter_reply = null;
    String ConvID;
    int price;
    int result;

    public Transaction(Agent a, ACLMessage msg, int price) {
        super(a);
        this.msg = msg;
        ConvID = msg.getConversationId();
        this.price = price;
        this.result = 0;
    }

    public void onStart() {
        addSubBehaviour(new DelayBehaviour( myAgent, rnd.nextInt(1000)) {
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

        addSubBehaviour(new ReceiverBehaviour(myAgent, 2000, template) {
            public void handle( ACLMessage msg1) {
                if (msg1 != null ) {
                    int offer = Integer.parseInt(msg1.getContent());

                    // sets the default reply to REFUSE
                    // in the Airplane classe it will be decided if it's accepted
                    barter_reply = msg1.createReply();
                    barter_reply.setPerformative(ACLMessage.REFUSE);
                    barter_reply.setContent("" + offer);
                    return;
                    /*
                    if (offer >= rnd.nextInt(price/2)) {
                        barter_reply.setPerformative(ACLMessage.AGREE);
                        barter_reply.setContent("" + offer);
                    }
                    else {
                        barter_reply.setPerformative(ACLMessage.REFUSE);
                        barter_reply.setContent("" + offer);
                        result = 1;
                        myAgent.send(barter_reply);
                    }
                    */
                }
                else {
                    System.out.println("Timeout!! $" + price +
                            " from " + myAgent.getLocalName() +
                            " is no longer valid. " + msg.getSender().getLocalName() + " took long to respond");
                }
            }
        });
    }

    public int getResult() {
        return result;
    }

    public ACLMessage getBarterReply() { return barter_reply; }

    Random newRandom() {
        return new Random( hashCode() + System.currentTimeMillis());
    }
}