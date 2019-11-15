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
    private float bestOfferRating = 0;
    private ACLMessage bestOffer;
    private String [] airportNameList = null;

    private float rank = rnd.nextFloat(); // temporary for testing
    private int min_acceptable_offer = 30, max_acceptable_offer; // min_acceptable_offer/2 + (int) (min_acceptable_offer * 5 * rank);

    private float waiting_multiplier = rnd.nextFloat() * 1.5f + 0.5f; // formula: rnd(0..1) * range + min_value --- this case: 0.5 ~ 2
    private int waiting_time = 0, max_waiting_time = (int) (30000 * waiting_multiplier); // this case: max_waiting_time = 15s ~ 60s
    private float waiting_function = 0;
    // = (float) (1 / (1 + Math.exp(-max_waiting_time/waiting_time)) - 0.5);
    // from 0 ~ 0.5, decreases as waiting_time increases

    private int flight_length_preference = rnd.nextInt(12) + 1;
    private float flight_length_tolerance = rnd.nextFloat() * 3 + 3; // 3 ~ 6 tolerable flight length difference

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

        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                waiting_time = waiting_time + 1000;
                waiting_function = (float) (1 / (1 + Math.exp(-max_waiting_time/waiting_time)) - 0.5); // from 0 ~ 0.5, decreases as waiting_time increases
            }
        });

        // CrewMembers should ask BigBrother for the airportNameList beforehand
        // ACLMessage big_brother_msg = newMsg(ACLMessage.QUERY_REF)
        // send big_brother_msg and wait for a reply
        // while( airportNameList == null ) { do nothing; }
        // for (int i = 0; i < airportNameList.length; i++) {
        for (int i = 0; i < 3; i++) { // temporary fix
            // TODO send it to every existing Airplane Agent, instead of a static list of Airplane Agents
            msg.addReceiver( new AID("s"+i, AID.ISLOCALNAME ));
            msg.setContent("" + rank);

            par.addSubBehaviour( new ReceiverBehaviour( this, 1000, template) {
                public void handle(ACLMessage msg) {
                    if (msg != null) {
                        String[] content = (msg.getContent().split(","));
                        int offer = Integer.parseInt(content[0]);
                        int flight_length = Integer.parseInt(content[1]);

                        // use the flight_length to rate the salary offer
                        float fl_multiplier = getFlightLengthPreferenceMultiplier(flight_length);
                        float offer_rating = offer * fl_multiplier;

                        if (offer_rating > bestOfferRating) {
                            bestOfferRating = offer_rating;
                            bestSalaryOffer = offer;
                            bestOffer = msg;
                        }
                    }
                }
            });
        }
        seq.addSubBehaviour(par);

        seq.addSubBehaviour(new DelayBehaviour(this, rnd.nextInt(500)) {
            public void handleElapsedTimeout() {
                if (bestOffer != null) {
                    ACLMessage reply = bestOffer.createReply();
                    System.out.println(getLocalName() + " <- Best Salary Offer is $" + bestSalaryOffer + " from " + bestOffer.getSender().getLocalName());

                    // RANDOM BASED BARTERING
                    int proposal;
                    if (bestSalaryOffer < min_acceptable_offer) bestSalaryOffer = min_acceptable_offer;

                    // crew member will accept anything above x, depending on its rank
                    // will propose a higher offer, depending on how comfortable it is to wait more
                    max_acceptable_offer = (int) (min_acceptable_offer + min_acceptable_offer * (2*rank) * (5*waiting_function));
                    System.out.println(getLocalName() + " <- max acceptable offer = " + max_acceptable_offer);

                    if (bestSalaryOffer >= max_acceptable_offer)
                        proposal = bestSalaryOffer;
                    else
                        proposal = (int) (bestSalaryOffer + bestSalaryOffer * waiting_function);



                    reply.setPerformative( ACLMessage.REQUEST );
                    reply.setContent( proposal + "," + rank );
                    send(reply);
                    System.out.println(getLocalName() + " (" + (int) (rank*100) + ") <- Asking for $" + proposal + " from " + bestOffer.getSender().getLocalName());
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
                        System.out.println(getLocalName() + " <- GOT ACCEPTED by " + msg.getSender().getLocalName());
                    else {
                        System.out.println(getLocalName() + " <- GOT REJECTED by " + msg.getSender().getLocalName());
                        setup();
                    }
                }
                else {
                    System.out.println(getLocalName()
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
    private float getFlightLengthPreferenceMultiplier(int flight_length) {
        int diff = Math.abs(flight_length_preference - flight_length);

        if (diff == 0) return 2f;

        return (float) (1 / Math.pow(diff/flight_length_tolerance, 1/3)); // multiplier ranges from 1 ~ 1.8 with these flight tolerance numbers
    }

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