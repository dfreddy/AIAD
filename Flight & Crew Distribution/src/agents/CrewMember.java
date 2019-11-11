package agents;

import behaviours.*;
import behaviours.ReceiverBehaviour;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

import static java.lang.Integer.parseInt;

public class CrewMember extends Agent {

    private Random rnd = newRandom();
    private int bestSalaryOffer = 0;
    private ACLMessage bestOffer;
    private String [] airportNameList = null;

    private int min_acceptable_offer = 30, max_acceptable_offer = 70;

    protected void setup() {
        bestSalaryOffer = 0;
        bestOffer = null;

        ACLMessage msg = newMsg(ACLMessage.QUERY_REF);

        MessageTemplate template = MessageTemplate.and(
                MessageTemplate.MatchPerformative( ACLMessage.INFORM ),
                MessageTemplate.MatchConversationId( msg.getConversationId() ));

        SequentialBehaviour seq = new SequentialBehaviour();
        addBehaviour(seq);

        ParallelBehaviour par = new ParallelBehaviour( ParallelBehaviour.WHEN_ALL );

        // CrewMembers should ask BigBrother for the airportNameList beforehand
        // ACLMessage big_brother_msg = newMsg(ACLMessage.QUERY_REF)
        // send big_brother_msg and wait for a reply
        // while( airportNameList == null ) { do nothing; }
        // for (int i = 0; i < airportNameList.length; i++) {

        for (int i = 0; i < 3; i++) {   // temporary fix
            // TODO send it to every existing Airplane Agent, instead of a static list of Airplane Agents
            msg.addReceiver( new AID("s"+i, AID.ISLOCALNAME ));

            par.addSubBehaviour( new ReceiverBehaviour( this, 1000, template) {
                public void handle(ACLMessage msg) {
                    if (msg != null) {
                        int offer = Integer.parseInt( msg.getContent());
                        if (offer > bestSalaryOffer) {
                            bestSalaryOffer = offer;
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
                    System.out.println("=="+ getLocalName() + " <- Best Salary Offer is $" + bestSalaryOffer + " from " + bestOffer.getSender().getLocalName());

                    // RANDOM BASED BARTERING
                    int proposal;
                    if (bestSalaryOffer < min_acceptable_offer) bestSalaryOffer = min_acceptable_offer;
                    if (bestSalaryOffer < max_acceptable_offer)
                        proposal =  bestSalaryOffer + (int)(bestSalaryOffer * rnd.nextFloat()*0.5f); // crew member will accept anything above 70. will try to barter if it's below 70
                    else
                        proposal = bestSalaryOffer;

                    // TODO: add also the worker's resume (rank) to the reply's content
                    reply.setPerformative( ACLMessage.REQUEST );
                    reply.setContent( "" + proposal );
                    send(reply);
                    System.out.println("=="+ getLocalName() + " <- Asking for a salary of $" + proposal + " from " + bestOffer.getSender().getLocalName());
                }
            }
        });

        // template match <...CID &  AGREE or REFUSE ...>
        MessageTemplate receiverTemplate = MessageTemplate.and(
                MessageTemplate.MatchConversationId(msg.getConversationId()), MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.AGREE)
                        , MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                ));

        seq.addSubBehaviour(new ReceiverBehaviour(this, 5000, receiverTemplate){
            public void handle(ACLMessage msg) {
                if (msg != null ) {
                    if( msg.getPerformative() == ACLMessage.INFORM){
                        airportNameList = msg.getContent().split(";");
                        System.out.println(myAgent.getLocalName() + " got airport list: " + msg.getContent());
                        return;
                    }
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

        /*  Cyclic Behaviour for catching
        MessageTemplate receiverBigBrotherTemplate = MessageTemplate.and(
                MessageTemplate.MatchConversationId(big_brother_msg.getConversationId()), MessageTemplate.MatchPerformative(ACLMessage.INFORM)
                );

        seq.addSubBehaviour(new CyclicBehaviour(){
                ACLMessage msg = receive(receiverBigBrotherTemplate);
                if (msg != null ) {
                    airportNameList = msg.getContent().split(";");
                    System.out.println(myAgent.getLocalName() + " got airport list: " + msg.getContent());
                    return;
                }
        });
        */

        send(msg);
    }

    // ========== Utility methods =========================
    //  --- generating Conversation IDs -------------------
    private static int cidCnt = 0;
    private String cidBase ;

    private String genCID() {
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

    private ACLMessage newMsg(int perf) {
        ACLMessage msg = new ACLMessage(perf);
        msg.setConversationId( genCID() );
        return msg;
    }

    private Random newRandom() {
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

    private void dumpMessage(ACLMessage msg) {
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