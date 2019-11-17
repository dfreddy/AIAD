package behaviours;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.Random;

public class Transaction extends SequentialBehaviour {
    Random rnd = newRandom();
    ACLMessage msg, reply, barter_reply = null;
    String ConvID;
    int price, result, final_offer;
    int experience;
    double flight_time, connection_time;
    String rank;

    public Transaction(Agent a, ACLMessage msg, int price, double ft, double ct) {
        super(a);
        this.msg = msg;
        ConvID = msg.getConversationId();
        this.price = price;
        this.flight_time = ft;
        this.connection_time = ct;
        this.result = 0;
    }

    public void onStart() {
        addSubBehaviour(new DelayBehaviour( myAgent, rnd.nextInt(500)) {
            public void handleElapsedTimeout() {
                // System.out.println(myAgent.getLocalName() + " <- QUERY from " + msg.getSender().getLocalName() + ". Will answer with $" + price);

                reply = msg.createReply();
                reply.setPerformative( ACLMessage.INFORM );
                reply.setContent(price + "," + flight_time + "," + connection_time);
                myAgent.send(reply);
            }
        });

        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchPerformative( ACLMessage.REQUEST ),
                MessageTemplate.MatchConversationId( ConvID ));

        addSubBehaviour(new ReceiverBehaviour(myAgent, 4000, template) {
            public void handle( ACLMessage msg1) {
                if (msg1 != null ) {
                    String[] content = (msg1.getContent().split(","));
                    final_offer = Integer.parseInt(content[0]);
                    experience = Integer.parseInt(content[1]);
                    rank = content[2];

                    // sets the default reply to REFUSE
                    // in the Airplane classe it will be decided if it's accepted
                    barter_reply = msg1.createReply();
                    barter_reply.setPerformative(ACLMessage.REFUSE);
                    barter_reply.setContent("" + final_offer);

                    return;
                }
                else {
                    // System.out.println("Timeout!! $" + price + " from " + myAgent.getLocalName() + " is no longer valid. " + msg.getSender().getLocalName() + " took long to respond");
                }
            }
        });
    }

    public int getFinalOffer() { return final_offer; }
    public int getExperience() { return experience; }
    public String getRank() { return rank; }

    public ACLMessage getBarterReply() { return barter_reply; }

    Random newRandom() {
        return new Random( hashCode() + System.currentTimeMillis());
    }
}