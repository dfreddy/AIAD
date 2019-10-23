package src5;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.Random;
import static java.lang.Integer.parseInt;

public class parBuyer extends Agent {
    int bestPrice = 9999;
    ACLMessage bestOffer = null;

    protected void setup() {
        ACLMessage msg = newMsg( ACLMessage.QUERY_REF );

        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchPerformative( ACLMessage.INFORM ),
                MessageTemplate.MatchConversationId( msg.getConversationId() ));

        SequentialBehaviour seq = new SequentialBehaviour();
        addBehaviour(seq);

        ParallelBehaviour par = new ParallelBehaviour( ParallelBehaviour.WHEN_ALL );
        seq.addSubBehaviour(par);

        for (int i = 1; i<=3; i++) {
            msg.addReceiver(new AID( "s" + i,  AID.ISLOCALNAME ));

            par.addSubBehaviour( new receiverBehaviour( this, 1000, template) {
                public void handle(ACLMessage msg) {
                    if (msg != null) {
                        int offer = Integer.parseInt(msg.getContent());
                        if (offer < bestPrice) {
                            bestPrice = offer;
                            bestOffer = msg;
                        }
                    }
                }
            });
        }
        seq.addSubBehaviour( new OneShotBehaviour() {
            public void action() {
                if (bestOffer != null)
                    System.out.println("Best Price $" + bestPrice + " from " + bestOffer.getSender().getLocalName());
                else
                    System.out.println("Got nothing");
            }
        });
        send(msg);
    }

// ========== Utility methods =========================
//  --- generating Conversation IDs -------------------
    protected static int cidCnt = 0;
    String cidBase ;

    String genCID() {
        if (cidBase==null) {
            cidBase = getLocalName() + hashCode() +
                    System.currentTimeMillis()%10000 + "_";
        }
        return  cidBase + (cidCnt++);
    }

//  --- Methods to initialize ACLMessages -------------------
    ACLMessage newMsg( int perf, String content, AID dest) {
        ACLMessage msg = newMsg(perf);
        if (dest != null) msg.addReceiver( dest );
        msg.setContent( content );
        return msg;
    }

    ACLMessage newMsg( int perf) {
        ACLMessage msg = new ACLMessage(perf);
        msg.setConversationId( genCID() );
        return msg;
    }
}