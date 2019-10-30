package agents;

import behaviours.*;
import behaviours.ReceiverBehaviour;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import static java.lang.Integer.parseInt;

public class CrewMember extends Agent {
    Random rnd = newRandom();
    int bestPrice = 9999;
    ACLMessage msg, bestOffer;

    protected void setup() {
        bestPrice = 9999;
        bestOffer = null;

        msg = newMsg( ACLMessage.QUERY_REF );

        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchPerformative( ACLMessage.INFORM ),
                MessageTemplate.MatchConversationId( msg.getConversationId() ));

        SequentialBehaviour seq = new SequentialBehaviour();
        addBehaviour(seq);

        ParallelBehaviour par = new ParallelBehaviour( ParallelBehaviour.WHEN_ALL );

        for (int i = 1; i<=3; i++) {
            // TODO send it to every existing Airplane Agent, instead of a static list of Airplane Agents
            msg.addReceiver( new AID( "s" + i,  AID.ISLOCALNAME ));

            par.addSubBehaviour( new ReceiverBehaviour( this, 1000, template) {
                public void handle(ACLMessage msg) {
                    if (msg != null) {
                        int offer = Integer.parseInt( msg.getContent());
                        if (offer < bestPrice) {
                            bestPrice = offer;
                            bestOffer = msg;
                        }
                    }
                }
            });
        }
        seq.addSubBehaviour(par);

        seq.addSubBehaviour(new DelayBehaviour(this, rnd.nextInt(1000)) {
            public void handleElapsedTimeout() {
                if (bestOffer != null) {
                    ACLMessage reply = bestOffer.createReply();
                    System.out.println("=="+ getLocalName() + " <- Best Price is $" + bestPrice + " from " + bestOffer.getSender().getLocalName());
                    // if ( bestPrice <= 30 ) {

                        // RANDOM BASED BARTERING
                        if (bestPrice > 30) bestPrice = 30;
                        int offer = rnd.nextInt(bestPrice)+1;
                        reply.setPerformative( ACLMessage.REQUEST );
                        reply.setContent( "" + offer );
                        send(reply);
                        System.out.println("=="+ getLocalName() + " <- Offering $" + offer + " to " + bestOffer.getSender().getLocalName());
                    // }
                    // else setup();
                }
            }
        });

        // template match <...CID &  AGREE or REFUSE ...>
        MessageTemplate receiverTemplate = MessageTemplate.and(
                MessageTemplate.MatchConversationId(msg.getConversationId()), MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.AGREE), MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                ));

        seq.addSubBehaviour(new ReceiverBehaviour(this, 5000, receiverTemplate){
            public void handle(ACLMessage msg) {
                if (msg != null ) {
                    if( msg.getPerformative() == ACLMessage.AGREE)
                        System.out.println("\n==" + getLocalName() + " <- GOT ACCEPTED by " + msg.getSender().getLocalName());
                    else {
                        System.out.println("\n==" + getLocalName() + " <- GOT REJECTED by " + msg.getSender().getLocalName());
                        setup();
                    }
                }
                else {
                    System.out.println("==" + getLocalName()
                            +" timed out... setting up again");
                    setup();
                }
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

    ACLMessage newMsg(int perf) {
        ACLMessage msg = new ACLMessage(perf);
        msg.setConversationId( genCID() );
        return msg;
    }

    Random newRandom() {
        return new Random( hashCode() + System.currentTimeMillis());
    }

    // Garbage Disposal
    class GCAgent extends TickerBehaviour {
        Set seen = new HashSet(),
                old  = new HashSet();

        GCAgent(Agent a, long dt) { super(a,dt); }

        protected void onTick() {
            ACLMessage msg = myAgent.receive();
            while (msg != null) {
                if (! old.contains(msg))
                    seen.add( msg);
                else {
                    System.out.println("==" + getLocalName() + " <- Flushing message:");
                    dumpMessage( msg );
                }
                msg = myAgent.receive();
            }

            for(Iterator it = seen.iterator(); it.hasNext(); )
                myAgent.putBack( (ACLMessage) it.next() );

            old.clear();
            Set tmp = old;
            old = seen;
            seen = tmp;
        }
    }
    static long t0 = System.currentTimeMillis();

    void dumpMessage( ACLMessage msg ) {
        System.out.print( "t=" + (System.currentTimeMillis()-t0)/1000F + " in "
                + getLocalName() + ": "
                + ACLMessage.getPerformative(msg.getPerformative() ));

        System.out.print( "  from: " +
                (msg.getSender()==null ? "null" : msg.getSender().getLocalName())
                +  " --> to: ");

        for (Iterator it = msg.getAllReceiver(); it.hasNext();)
            System.out.print( ((AID) it.next()).getLocalName() + ", ");
        System.out.println( "  cid: " + msg.getConversationId());
        System.out.println( "  content: " +  msg.getContent());
    }
}