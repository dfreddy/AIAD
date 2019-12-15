package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import behaviours.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class Airplane extends Agent {
    private Random rnd = newRandom();
    private int available_spots,
            initial_time_to_takeoff = 15000 + 5000 * (rnd.nextInt(5)), // initial time until takeoff varies between 15s and 40s
            time_to_takeoff = initial_time_to_takeoff,
            spent_budget = 0;

    //Flight Specifications
    double totalFlightTime, flightsTime, connectionTime;

    //Might be LONG, MEDIUM, SHORT
    String flightType;

    //requirements
    int requiredExperienceForPilots, requiredExperienceForAttendants, requiredExperienceForCabinChief;

    //max payment of the airline per hour
    double maxRealFlightHourPrice = calcMaxRealFlightHourPrice();
    double maxConnectionHourPrice = calcMaxConnectionHourPrice();

    private int nrOfRequiredPilots, nrOfRequiredAttendants, nrOfRequiredCabinChief,
            remainingPilotSpots, remainingAttendantsSpots, remainingCabinChiefSpots;

    private Map<ACLMessage, Map<Integer, String>> receivedTransactions = new HashMap<ACLMessage, Map<Integer, String>>(); // <reply, exp, rank>
    HashSet<AID> crew = new HashSet<AID>(); // updates with accepted crew members
    private boolean is_handling_business;  // impedes the airline from repeatedly starting

    protected void setup() {
        generateFlightSpecification();
        attributeFlightType();
        available_spots = remainingAttendantsSpots + remainingCabinChiefSpots + remainingPilotSpots;

        // System.out.println(getLocalName() + " <- time to takeoff = " + time_to_takeoff / 1000 + "s");

        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                time_to_takeoff = time_to_takeoff - 1000;

            }
        });

        is_handling_business = false;

        // cyclic behaviour that goes on receiving transactions
        // ignores ones from repeating senders
        CyclicBehaviour receive_transactions = new CyclicBehaviour() {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));
                // handle received msg only if it's not from a repeating sender

                if (msg != null && available_spots > 0 && !receivedTransactions.containsKey(msg)) {
                    String[] content = (msg.getContent().split(","));
                    int cm_exp = Integer.parseInt(content[0]);
                    String cm_rank = content[1];

                    SequentialBehaviour t = new Transaction(myAgent, msg, getSalaryForCrewMember(cm_exp, cm_rank), flightsTime, connectionTime) {
                        public int onEnd() {
                            ACLMessage barter_reply = getBarterReply();
                            int exp = getExperience();
                            String r = getRank();
                            Map<Integer, String> cm = new HashMap<Integer, String>();
                            cm.put(exp, r);

                            receivedTransactions.put(barter_reply, cm);

                            return super.onEnd();
                        }
                    };

                    addBehaviour(t);

                } else block();
            }
        };

        CyclicBehaviour biz_cycle = new CyclicBehaviour() {
            public void action() {

                // wont create another waker behaviour until the last one has ended
                if (is_handling_business == false) {
                    is_handling_business = true;
                    // wait to receive transactions
                    addBehaviour(new WakerBehaviour(myAgent, 5000) {
                        protected void onWake() {
                            if(available_spots > 0)
                                // System.out.println(getLocalName() + " received " + receivedTransactions.size() + " transactions");

                            handleBiz(receivedTransactions);
                            receivedTransactions.clear();
                        }
                    });
                }

            }
        };

        addBehaviour(receive_transactions);
        addBehaviour(biz_cycle);

        addBehaviour(new GCAgent(this, 5000));
    }

    // =============================== //
    // ========== FUNCTIONS ========== //
    // =============================== //

    // decides which proposals to accept
    // waits for 3s to receive all transactions, then handles them
    private void handleBiz(Map<ACLMessage, Map<Integer, String>> currentTransactions) {

        if (currentTransactions.size() == 0 || available_spots == 0) {
            finishCycle(); // do nothing if there's no finished transactions
            return;
        }

        /*
        String trans_list = "";
        if(currentTransactions.size() > 0) {
            Iterator itt = currentTransactions.entrySet().iterator();
            while (itt.hasNext()) {
                Map.Entry pair = (Map.Entry) itt.next();
                if (pair.getKey() == null) continue;
                ACLMessage tmp_msg = (ACLMessage) pair.getKey();
                AID receiver = (AID) (tmp_msg.getAllReceiver().next());
                trans_list += receiver.getLocalName().substring(11) + ",";
            }
        }
        System.out.println(getLocalName() + " <- transaction list: " + trans_list);
        */

        // decide what's the best offer to accept
        // considers every offer, starting wit the CrewMembers wit higher rank, in a loop where the available_budget/spots is updated accordingly
        ArrayList<ACLMessage> finishedTransactions;

        while(currentTransactions.size() > 0) {
            currentTransactions.remove(null);

            Iterator it = currentTransactions.entrySet().iterator();
            double best_rating = 0;
            ACLMessage best_proposal = null;
            String best_position = null;
            int best_prop = 0;

            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry) it.next();
                if (pair.getKey() == null) continue;

                Iterator it2 = ((Map<Integer, String>) (pair.getValue())).entrySet().iterator();

                ACLMessage tmp_msg = (ACLMessage) pair.getKey();

                Map.Entry pair2 = (Map.Entry) it2.next();
                int prop = Integer.parseInt(tmp_msg.getContent());
                double rating = getProposalRating((Integer) pair2.getKey(), (String) pair2.getValue(), prop);

                // AID receiver = (AID)(tmp_msg.getAllReceiver().next());
                // System.out.println(getLocalName() + "<- rating for " + receiver.getLocalName() + " (" + (String) pair2.getValue() + ") is: " + rating);

                if (rating >= best_rating) {
                    best_prop = prop;
                    best_rating = rating;
                    best_proposal = tmp_msg;
                    best_position = (String) pair2.getValue();
                }
            }

            if(best_proposal == null) {
                // System.out.println(getLocalName() + " <- found null");
            }

            // System.out.println(getLocalName() + " <- best rating = " + best_rating);

            if (best_rating >= 0.6 && best_position != null && best_proposal != null) {
                // if accepted
                switch (best_position) {
                    case "PILOT":
                        if(remainingPilotSpots > 0) {
                            remainingPilotSpots--;
                            best_proposal.setPerformative(ACLMessage.AGREE);
                            best_proposal.setContent(best_prop + "");
                            crew.add((AID) best_proposal.getAllReceiver().next());
                        }
                        break;
                    case "CABIN_CHIEF":
                        if(remainingCabinChiefSpots > 0) {
                            remainingCabinChiefSpots--;
                            best_proposal.setPerformative(ACLMessage.AGREE);
                            best_proposal.setContent(best_prop + "");
                            crew.add((AID) best_proposal.getAllReceiver().next());
                        }
                        break;
                    case "ATTENDANT":
                        if(remainingAttendantsSpots > 0) {
                            remainingAttendantsSpots--;
                            best_proposal.setPerformative(ACLMessage.AGREE);
                            best_proposal.setContent(best_prop + "");
                            crew.add((AID) best_proposal.getAllReceiver().next());
                        }
                        break;
                    default:
                        break;
                }
            }

            if(best_proposal != null) {
                send(best_proposal);
                currentTransactions.remove(best_proposal);
                spent_budget = spent_budget + Integer.parseInt(best_proposal.getContent());
            }
        }

        finishCycle();
    }

    private void finishCycle() {
        is_handling_business = false;
        // System.out.println(getLocalName() + " <- finished handling business");

        int diff = available_spots;
        available_spots = remainingAttendantsSpots + remainingCabinChiefSpots + remainingPilotSpots;
        diff = diff - available_spots;

        boolean last_cycle = (available_spots == 0 && diff == 0);
        // System.out.println(getLocalName() + " <- last cycle?= " + last_cycle);

        if (!last_cycle)
            System.out.println(getLocalName() + " <- employed " + diff);

        if (available_spots == 0 && time_to_takeoff <= 0) {
            // System.out.println(getLocalName() + " <- TOOK OFF");
            doDelete();
        }

        if (available_spots > 0 && time_to_takeoff <= 0) {
            System.out.println(getLocalName() + " <- couldn't fly. Missing " + remainingPilotSpots + " PILOTS, " +
                   remainingCabinChiefSpots + " CABIN CHIEFS and " + remainingAttendantsSpots + " ATTENDANTS.");
            doDelete();
        }

        if (time_to_takeoff > 0 && !last_cycle) {
            //System.out.println(getLocalName() + " <- NEEDS PILOTS: " + remainingPilotSpots + ", CABIN CHIEF: " + remainingCabinChiefSpots + ", ATTENDANTS: " + remainingAttendantsSpots);
            //System.out.println(getLocalName() + " <- time to takeoff is now " + time_to_takeoff / 1000 + "s");
        }
    }

    // if index is high
    // then higher urgency in employing
    private double getNecessityIndex(String rank) {
        int diff = initial_time_to_takeoff - time_to_takeoff;
        double percent_time_to_takeoff = (100*diff / initial_time_to_takeoff);
        percent_time_to_takeoff = percent_time_to_takeoff / 100;
        double percent_position = 0;

        switch(rank) {
            case "PILOT":
                if(remainingPilotSpots == 0) return 0;
                else percent_position = remainingPilotSpots / nrOfRequiredPilots;
                break;
            case "CABIN_CHIEF":
                if(remainingCabinChiefSpots == 0) return 0;
                else percent_position = remainingCabinChiefSpots / nrOfRequiredCabinChief;
                break;
            case "ATTENDANT":
                if(remainingAttendantsSpots == 0) return 0;
                else percent_position = remainingAttendantsSpots / nrOfRequiredAttendants;
                break;
            default:
                break;
        }

        return percent_time_to_takeoff * 0.9 + percent_position * 0.1;
    }

    // judges the proposal according to the experience
    // a high rating means it's a good proposal, for the airline
    private double getProposalRating(int exp, String rank, int proposal) {
        double maxOffer = calculateMaxOffer(exp, rank);

        double rating = maxOffer / proposal;

        return rating * getNecessityIndex(rank);
    }

    //Price varies between 12-15€ per hour flying
    public double calcMaxRealFlightHourPrice() {
        Random rnd = newRandom();
        return rnd.nextInt((13-10) + 1) + 10;
    }

    //Price varies between 6-8€ per hour in connection
    public double calcMaxConnectionHourPrice() {
        Random rnd = newRandom();
        return rnd.nextInt((9-5) + 1) + 5;
    }

    // this func calculated an percentage of valueOfExperience to be used in the calculation of the maxOffer (calculateMaxOffer)
    // if experience lower than 40 (0-100) then all the payment per hour is equal to 0.7,
    //if higher then is proportional to experience/100,
    //1 is the maximum valueForExperience.
    public double calculateValueForExperience(int experience) {
        double valueForExperience;

        if(experience < 40){
            valueForExperience = 0.5;
        }else{
            valueForExperience = (experience-40d)/100d + 0.5;

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
    public double calculateMaxOffer(int experience, String rank) {
        double maxOffer;
        double valueForExperience = calculateValueForExperience(experience);

        maxOffer = ((valueForExperience*maxRealFlightHourPrice*flightsTime) + (connectionTime * maxConnectionHourPrice)) / totalFlightTime;

        if(rank == "PILOT"){
            maxOffer = maxOffer + (maxOffer*0.5);
        }
        else if(rank == "CABIN_CHIEF"){
            maxOffer = maxOffer + (maxOffer*0.2);
        }
        else {}

        return maxOffer;
    }

    //This function generates all the flight specifications, namely the connection time, the real time spent in the air and the total flight time.
    public void generateFlightSpecification() {
        //used to limit the number or hours in a flight being generated (numberOfConnections * timesOfConnection + FlightTime)
        int minFlightTime = 1;
        int maxFlightTime = 16;
        int minConnectionTime = 1;
        int maxConnectionTime = 2;
        int maxNumberOfConnections = 2;

        //generates the connectionsTime
        Random rnd = newRandom();
        int nrOfConnections = rnd.nextInt(maxNumberOfConnections);
        connectionTime = 0;
        for (int i=0; i < nrOfConnections; i++){
            connectionTime = connectionTime + rnd.nextInt((maxConnectionTime - minConnectionTime) + 1) + minConnectionTime + rnd.nextDouble();
        }

        //generates the flightsTime and totalFlightTime
        flightsTime = rnd.nextInt((int) ((maxFlightTime - connectionTime) - minFlightTime) + 1) + minFlightTime + rnd.nextDouble();
        totalFlightTime = flightsTime + connectionTime;
        // System.out.println(getLocalName() + " <- total flight time = " + totalFlightTime);
    }

    // Attributes the FlightType, which is important to generate the necessary crew for each flight and its experience
    public void attributeFlightType() {
        //attributes flightType
        if(totalFlightTime <= 4){
            flightType = "SHORT";
            generateNecessaryCrew(2, 4, 1, 2, 2, 2);
        }
        else if( totalFlightTime > 4 && totalFlightTime <= 8){ //mid term flight
            flightType = "MEDIUM";
            generateNecessaryCrew(3, 6, 2, 3, 2, 2);
        }
        else{ // long term flight
            flightType = "LONG";
            generateNecessaryCrew(6, 10, 4, 6, 3, 4);
        }
    }

    // This func attributes the required experience for each agent in each flight (it is optional)
    private void generateRequiredExperience(int attendantExp, int chiefExp, int pilotExp){
        requiredExperienceForAttendants = attendantExp;
        requiredExperienceForCabinChief = chiefExp;
        requiredExperienceForPilots = pilotExp;
    }

    //IMPORTANT: min can NEVER be 0
    private void generateNecessaryCrew(int minAttendant, int maxAttendant, int minCabinChief, int maxCabinChief, int minPilot, int maxPilot){
        Random rnd = newRandom();

        nrOfRequiredAttendants = rnd.nextInt((maxAttendant - minAttendant) + 1) + minAttendant;
        nrOfRequiredCabinChief = rnd.nextInt((maxCabinChief - minCabinChief) + 1) + minCabinChief;
        nrOfRequiredPilots = rnd.nextInt((maxPilot - minPilot) + 1) + minPilot;

        remainingPilotSpots = nrOfRequiredPilots;
        remainingCabinChiefSpots = nrOfRequiredCabinChief;
        remainingAttendantsSpots = nrOfRequiredAttendants;
    }

    // depending on time_to_takeoff and CM rank
    private int getSalaryForCrewMember(int exp, String rank) {
        int sal;
        int diff = initial_time_to_takeoff - time_to_takeoff;
        double percent_time_to_takeoff = (100*diff / initial_time_to_takeoff);
        percent_time_to_takeoff = percent_time_to_takeoff / 100;

        double maxOffer = calculateMaxOffer(exp, rank);
        double minOffer = maxOffer / 2;

        // time left until takeoff has more influence on salary variation
        sal = (int) (minOffer + ((maxOffer-minOffer) * percent_time_to_takeoff * 1.2));

        // if(sal > 20) System.out.println(getLocalName() + " <- sal = " + sal + ", maxOffer = " + maxOffer + ", takeoff = " + percent_time_to_takeoff);
        return sal;
    }

    /*
    public void transactionFailed() {
        transaction_failed = true;
        transaction_done = true;
    }

    public void transactionSuccess() {
        transaction_failed = false;
        transaction_done = true;
    }
    */

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

            for (Object o : seen) myAgent.putBack((ACLMessage) o);

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