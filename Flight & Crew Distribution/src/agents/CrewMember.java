package agents;

import behaviours.DelayBehaviour;
import behaviours.ReceiverBehaviour;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.ParallelBehaviour;
import jade.core.behaviours.SequentialBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class CrewMember extends Agent {
    Random rnd = newRandom();
    int bestPrice = 9999;
    ACLMessage msg, bestOffer;
    //Experience measures is from 0 to 100 and is calculated regarding other parameters
    int experience;
    String rank;
    double maxOffer;
    double minOffer;

    void defineCrewRank(){
        Random rnd = newRandom();
        int rndRank = rnd.nextInt(100);
        if(rndRank < 15)
            rank = "PILOT";
        else if( rndRank >= 15 && rndRank <= 30)
            rank = "CABIN_CHIEF";
        else
            rank = "ATTENDANT";
    }

    void calculateExperience() {
        double exp = 0;
        Random rnd = newRandom();
        int flightTime;
        int monthWorkingInAirline;

        flightTime = rnd.nextInt(1000);
        monthWorkingInAirline = rnd.nextInt(480);
        int flightTimeScore = (int) Math.ceil((flightTime*100)/1000);
        int monthWorkingInAirlineScore = (int) Math.ceil((monthWorkingInAirline*100)/480);

        if(rank == "PILOT")
            exp = 0.8*flightTimeScore + 0.2*monthWorkingInAirlineScore;
        else if(rank == "CABIN_CHIEF")
            exp = 0.6*flightTimeScore + 0.4*monthWorkingInAirlineScore;
        else
            exp = 0.4*flightTimeScore + 0.6*monthWorkingInAirlineScore;

        experience = (int) Math.ceil(exp);
    }

    // this func calculated an percentage of valueOfExperience to be used in the calculation of the maxOffer (calculateMaxOffer)
    // if experience lower than 40 (0-100) then all the payment per hour is equal to 0.6,
    //if higher then is proportional to experience/100,
    //1 is the maximum valueForExperience.
    public double calculateValueForExperience(int experience){
        double valueForExperience;

        if(experience < 40){
            valueForExperience = 0.6;
        }else{
            valueForExperience = (experience-40d)/100d + 0.6;

            if(valueForExperience > 1)
                valueForExperience = 1;
        }

        return valueForExperience;
    }

    // This function calculates the maximum value the agent is wiling to pay for
    // maxExperience = 100
    // Pilots receive 50% more from the calculated MaxOffer,
    // Cabin Chief receive 20%,
    // Attendants, dont.
    public void calculateMaxMinOffer(double realFlightTime, double connectionTime){
        double valueForExperience = calculateValueForExperience(experience);
        Random rnd = newRandom();
        int maxRealFlightHourPrice = rnd.nextInt((20-10)+1) + 10;
        int minRealFlightHourPrice = rnd.nextInt((10-5)+1) + 5;

        int maxConnectionFlightHourPrice = rnd.nextInt((10-6)+1) + 6;
        int minConnectionFlightHourPrice = rnd.nextInt((6-2)+1) + 2;


        double maxOffer = (valueForExperience*maxRealFlightHourPrice*realFlightTime) + (connectionTime * maxConnectionFlightHourPrice);
        double minOffer = (valueForExperience * minRealFlightHourPrice * realFlightTime) + (connectionTime * minConnectionFlightHourPrice);


        if(rank == "PILOT"){
            maxOffer = maxOffer + (maxOffer*0.5);
            minOffer = maxOffer + (maxOffer*0.5);
        }
        else if(rank == "CABIN_CHIEF"){
            maxOffer = maxOffer + (maxOffer*0.2);
            minOffer = maxOffer + (maxOffer*0.2);
        }
        else{};

        this.maxOffer = Math.max(maxOffer, minOffer);
        this.minOffer = Math.min(minOffer, maxOffer);
    }

    protected void setup() {
        bestPrice = 9999;
        bestOffer = null;

        //defines the crew rank (ex. pilot, cabin chief, attendant)
        defineCrewRank();
        //calculates the experience inside the airline
        calculateExperience();

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

            par.addSubBehaviour( new ReceiverBehaviour( this, 2000, template) {
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

        seq.addSubBehaviour(new DelayBehaviour(this, rnd.nextInt( 1000 )) {
            public void handleElapsedTimeout() {
                if (bestOffer != null) {
                    ACLMessage reply = bestOffer.createReply();
                    System.out.println("\n=="+ getLocalName() + " <- Best Price $" + bestPrice + " from " + bestOffer.getSender().getLocalName());
                    if ( bestPrice <= 10 ) {

                        // RANDOM BASED BARTERING
                        int offer = rnd.nextInt(bestPrice);
                        reply.setPerformative( ACLMessage.REQUEST );
                        reply.setContent( "" + offer);
                        send(reply);
                        System.out.println("=="+ getLocalName() + " <- Offering " + offer + " to " + bestOffer.getSender().getLocalName());
                    }
                }
            }
        });

        // template match <...CID &  AGREE or REFUSE ...>
        MessageTemplate receiverTemplate = MessageTemplate.and(
                MessageTemplate.MatchConversationId(msg.getConversationId()), MessageTemplate.or(
                        MessageTemplate.MatchPerformative(ACLMessage.AGREE), MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
                ));

        seq.addSubBehaviour(new ReceiverBehaviour(this, 2000, receiverTemplate){
            public void handle(ACLMessage msg) {
                if (msg != null ) {
                    if( msg.getPerformative() == ACLMessage.AGREE)
                        System.out.println("==" + getLocalName() + " <- BOUGHT\n");
                    else {
                        System.out.println("==" + getLocalName() + " <- GOT REJECTED\n");
                        setup();
                    }
                }
                else {
                    System.out.println("==" + getLocalName()
                            +" timed out... setting up again\n");
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