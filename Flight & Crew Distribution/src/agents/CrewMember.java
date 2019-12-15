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
import java.util.*;

import static java.lang.Integer.min;
import static java.lang.Integer.parseInt;

public class CrewMember extends Agent {

    private Random rnd = newRandom();
    private double bestSalaryOffer;
    private float bestOfferRating;
    private ACLMessage bestOffer;
    SequentialBehaviour seq;
    ParallelBehaviour par;

    private ArrayList<String> airplaneNameList = new ArrayList<>();

    /*
    private float experience = rnd.nextFloat(); // temporary for testing
    private int minOffer = 30, maxOffer; // minOffer/2 + (int) (minOffer * 5 * experience);
    */
    int proposal;
    int experience;
    String rank;
    double maxOffer, floatingOffer, minOffer;

    private float waiting_multiplier = rnd.nextFloat() * 1.5f + 0.5f; // formula: rnd(0..1) * range + min_value --- this case: 0.5 ~ 2
    private int waiting_time = 0, max_waiting_time = (int) (30000 * waiting_multiplier); // this case: max_waiting_time = 15s ~ 60s
    private float waiting_function = 0;
    float crew_patience = rnd.nextFloat()*3 + 1;
    // = (float) (1 / (1 + Math.exp(-max_waiting_time/waiting_time)) - 0.5);
    // from 0 ~ 0.5, decreases as waiting_time increases

    private int flight_length_preference = rnd.nextInt(15) + 1;
    private float flight_length_tolerance = rnd.nextFloat() * 4 + 2; // 2 ~ 6 tolerable flight length difference

    private double happiness = 0;
    private int id;
    /*
    Crew Member Values
    - id
        will work as the key to a hashset contained in the Lil Brother
        the hashset will be first updated when the crew member has its values
        the hashset will have the happiness field updated when the agent gets the job
    - flight length tolerance (fl tolerance)
    - crew patience / waiting time tolerance (wt tolerance)
    - max waiting time (not the actual time waiting, but the variable randomly defined above)
    - rank
    - experience
    - happiness (% diff between max offer and best final offer  &&  % diff between waiting time and max waiting time)
     */
    private HashMap<String, Double> crew_member_values = new HashMap<String, Double>();

    protected void setup() {
        bestSalaryOffer = 0;
        bestOffer = null;
        bestOfferRating = 0;

        defineCrewRank();
        calculateExperience();

        id = Integer.parseInt(getLocalName().substring(11));

        startBehaviours();
    }

    protected void startBehaviours() {
        ACLMessage informBigBrother = new ACLMessage();
        informBigBrother.setContent(rank);
        informBigBrother.setPerformative(ACLMessage.INFORM);
        informBigBrother.addReceiver(new AID( "big_brother",  AID.ISLOCALNAME ));
        send(informBigBrother);

        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                waiting_time = waiting_time + 1000;
                waiting_function = (float) Math.abs(1 / (1 + Math.exp(-max_waiting_time / waiting_time)) - 1); // from 0 ~ 0.5, increases wit waiting_time
            }
        });

        addBigBrotherListener();
    }

    private void addBigBrotherListener(){
        MessageTemplate templateBigBrother = MessageTemplate.and(
                MessageTemplate.MatchPerformative( ACLMessage.INFORM ),
                MessageTemplate.MatchSender( new AID( "big_brother",  AID.ISLOCALNAME )));

        addBehaviour(new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive(templateBigBrother);

                if (msg != null ) {
                    airplaneNameList.clear();
                    String [] newAirplaneName = msg.getContent().split(";");
                    // System.out.println(getLocalName() + " <- airplane list: " + msg.getContent());

                    for(String s : newAirplaneName) {
                        // System.out.println(getLocalName() + " <- Airplane: " + s);
                        airplaneNameList.add(s);
                    }

                    if(airplaneNameList.size() > 0) {
                        // System.out.println(getLocalName() + " <- airplane list: " + airplaneNameList);
                        startNegotiating();
                    }

                }
                else block();
            }
        });

        /*
        addBehaviour(new ReceiverBehaviour(this, 1000, templateBigBrother){
            @Override
            public void handle(ACLMessage msg) {
                if (msg != null ) {
                    String [] newAirplaneName = msg.getContent().split(";");
                    System.out.println(getLocalName() + " <- airplane list: " + msg.getContent());

                    for(String s : newAirplaneName) {
                        // System.out.println(getLocalName() + " <- Airplane: " + s);
                        airplaneNameList.add(s);
                    }

                    if(airplaneNameList.size() > 0) {
                        // System.out.println(getLocalName() + " <- airplane list: " + airplaneNameList);
                        startNegotiating();
                    }
                    else
                        addBigBrotherListener();
                }

                else addBigBrotherListener();
            }
        });
        */
    }

    private void startNegotiating() {
        // if(airplaneNameList.size() == 0) startBehaviours();
        seq = new SequentialBehaviour();
        par = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);
        addBehaviour(seq);

        ACLMessage msg = newMsg(ACLMessage.QUERY_REF);
        // send query to airplanes and wait for replies
        for (String s : airplaneNameList) {
            msg.addReceiver( new AID(s, AID.ISLOCALNAME ));
            msg.setContent("" + experience + "," + rank);

            MessageTemplate template = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId(msg.getConversationId()));

            par.addSubBehaviour( new ReceiverBehaviour( this, 3000, template) {
                public void handle(ACLMessage msg) {
                    if (msg != null) {
                        String[] content = (msg.getContent().split(","));
                        int offer = Integer.parseInt(content[0]);
                        double flight_time = Double.parseDouble(content[1]);
                        double connection_time = Double.parseDouble(content[2]);

                        calculateMaxMinOffer(flight_time, connection_time);

                        // use the flight_length to rate the salary offer
                        float fl_multiplier = getFlightLengthPreferenceMultiplier(flight_time + connection_time);
                        float offer_rating = (float) offer * fl_multiplier;
                        // System.out.println(getLocalName() + " (" + experience + ") " + flight_length_preference + "hrs" + " <- rating for $" + offer + " from " + msg.getSender().getLocalName() + " is: " + offer_rating);

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
                    // System.out.println(getLocalName() + " <- Best Salary Offer is $" + bestSalaryOffer + " from " + bestOffer.getSender().getLocalName());

                    // RANDOM BASED BARTERING
                    if (bestSalaryOffer < minOffer) bestSalaryOffer = minOffer;

                    // crew member will accept anything above x, depending on its experience
                    // will propose a higher offer, depending on how comfortable it is to wait more

                    floatingOffer = maxOffer - (maxOffer - minOffer) * crew_patience * waiting_function;
                    // minOffer + minOffer * (5*waiting_function);

                    // System.out.println(getLocalName() + " <- max acceptable offer = " + maxOffer);

                    if (bestSalaryOffer >= maxOffer)
                        proposal = (int) bestSalaryOffer;
                    else
                        proposal = (int) (bestSalaryOffer + bestSalaryOffer * waiting_function);

                    reply.setPerformative( ACLMessage.REQUEST );
                    reply.setContent( proposal + "," + experience + "," + rank );
                    send(reply);

                    // System.out.println(getLocalName() + " (" + experience + ") <- Asking for $" + proposal + " from " + bestOffer.getSender().getLocalName());
                }
            }
        });

        // template match <...CID &  AGREE or REFUSE ...>
        MessageTemplate receiverTemplate = MessageTemplate.or(
                MessageTemplate.MatchPerformative(ACLMessage.AGREE)
                , MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
        );

        seq.addSubBehaviour(new ReceiverBehaviour(this, 6000, receiverTemplate){
            public void handle(ACLMessage msg) {
                if (msg != null ) {
                    if (msg.getPerformative() == ACLMessage.AGREE) {
                        // System.out.println("\t\t" + getLocalName() + " <- (" + rank + ") GOT ACCEPTED by " + msg.getSender().getLocalName() + " for $" + proposal);

                        // resend values to lil_brother but now with updated happiness
                        updateHappiness();
                        ACLMessage informLilBrother = new ACLMessage();
                        informLilBrother.setPerformative(ACLMessage.INFORM);
                        informLilBrother.addReceiver(new AID( "lil_brother",  AID.ISLOCALNAME ));
                        informLilBrother.setContent(id + "," + flight_length_tolerance + "," + crew_patience + "," + max_waiting_time +
                                "," + rank + "," + experience + "," + happiness);
                        send(informLilBrother);

                        doDelete();
                    }
                    else {
                        // System.out.println("\t\t\t\t" + getLocalName() + " <- GOT REJECTED with $" + proposal);
                        startBehaviours();
                    }
                }
                else {
                    // System.out.println(getLocalName() +" timed out... setting up again");
                    startBehaviours();
                }
            }
        });

        // System.out.println("Sent initial msg");
        send(msg);
    }

    // ========================= Utility methods ========================= //
    void updateHappiness() {
        // proposal / minOffer -> how good the proposal accepted was
        // proposal / maxOffer ^2 -> how good the proposal was compared to the max
        // waiting_function -> how tired they were of waiting (+0.1 to prevent 0div)
        happiness = 10 * ((proposal / minOffer) * Math.pow(proposal / maxOffer, 2)) / (waiting_function + 0.1);
    }

    void defineCrewRank(){
        Random rnd = newRandom();
        int rndRank = rnd.nextInt(100);
        if(rndRank < 25)
            rank = "PILOT";
        else if( rndRank >= 25 && rndRank <= 50)
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
    public void calculateMaxMinOffer(double realFlightTime, double connectionTime) {
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
            minOffer = minOffer + (minOffer*0.5);
        }
        else if(rank == "CABIN_CHIEF"){
            maxOffer = maxOffer + (maxOffer*0.2);
            minOffer = minOffer + (minOffer*0.2);
        }
        else{};

        this.maxOffer = Math.max(maxOffer, minOffer);
        this.minOffer = Math.min(minOffer, maxOffer);
    }

    private float getFlightLengthPreferenceMultiplier(double flight_length) {
        double diff = Math.abs(flight_length_preference - flight_length);

        if (diff == 0) return 1.6f;

        double pwr = Math.pow(diff/flight_length_tolerance, 1/5d);

        return (float) (1 / pwr); // multiplier ranges from 0 ~ 1.6 with these flight tolerance numbers
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
                    // System.out.println("==" + getLocalName() + " <- Flushing message:");
                    // dumpMessage( msg );
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
