package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import behaviours.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

public class Airplane extends Agent {
    Random rnd = newRandom();
    int price  = rnd.nextInt(100),
            transactions = 0,
            results = 0;

    //Flight Specifications
    double totalFlightTime;
    double flightsTime;
    double connectionTime;

    //Might be LONG, MEDIUM, SHORT
    String flightType;

    //requirements
    int requiredExperienceForPilots;
    int requiredExperienceForAttendants;
    int requiredExperienceForCabinChief;

    //max payment of the airline per hour
    //if experience > x ==> 10€/realFlightHours && 5€/connectionHours else 8€
    double maxRealFlightHourPrice = 10;
    double maxConnectionHourPrice = 5;

    //Required crew (podera ser necessário)
    int nrOfRequiredPilots;
    int nrOfRequiredAttendants;
    int nrOfRequiredCabinChief;

    // int available_spots;
    // int available_money;

    // this func calculated an percentage of valueOfExperience to be used in the calculation of the maxOffer (calculateMaxOffer)
    // if experience lower than 40 (0-100) then all the payment per hour is equal to 0.7,
    //if higher then is proportional to experience/100,
    //1 is the maximum valueForExperience.
    public double calculateValueForExperience(int experience){
        double valueForExperience;

        if(experience < 40){
            valueForExperience = 0.7;
        }else{
            valueForExperience = (experience-40d)/100d + 0.7;

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
    public double calculateMaxOffer(int experience, String rank){
        double maxOffer;
        double valueForExperience = calculateValueForExperience(experience);

        if(rank == "PILOT"){
            maxOffer = (valueForExperience*maxRealFlightHourPrice*flightsTime) + (connectionTime * maxConnectionHourPrice);
            maxOffer = maxOffer + (maxOffer*0.5);
        }
        else if(rank == "CABIN_CHIEF"){
            maxOffer = (valueForExperience*maxRealFlightHourPrice*flightsTime) + (connectionTime * maxConnectionHourPrice);
            maxOffer = maxOffer + (maxOffer*0.2);
        }
        else
            maxOffer = (valueForExperience*maxRealFlightHourPrice*flightsTime) + (connectionTime * maxConnectionHourPrice);

        return maxOffer;
    }

    //This function generates all the flight specifications, namely the connection time, the real time spent in the air and the total flight time.
    public void generateFlightSpecification(){
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
    }

    // Attributes the FlightType, which is important to generate the necessary crew for each flight and its experience
    public void attributeFlightType(){
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
    }

    protected void setup() {
        generateFlightSpecification();
        attributeFlightType();



        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));
                if (msg != null) { // handle received msg
                    SequentialBehaviour t = new Transaction(myAgent, msg, getPrice()) {
                        public int onEnd() {
                            int r = getResult();
                            transactions++;
                            results += r;

                            // System.out.println("Added result " + r + " to " + myAgent.getLocalName());
                            // System.out.println(myAgent.getLocalName() + " has " + transactions + " transactions");
                            return super.onEnd();
                        }
                    };
                    addBehaviour(t);
                }
                else block();
            }
        });

        addBehaviour(new GCAgent(this, 5000));

        //  CHECKING FOR UPDATING PRICE EVERY x SECONDS
        //  UPDATES PRICE ONLY IF THE SELLER CANT SELL ON ALL TRANSACTIONS
        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                if (transactions == 0) return;

                // System.out.println(myAgent.getLocalName() + "\nnr transactions: " + transactions + "\nResults: " + results);
                if(results == transactions) updatePrice();

                results = 0;
                transactions = 0;
            }
        });
    }



    private void updatePrice() {
        int p = (int)(this.price * 0.7);
        this.price = p;
        System.out.println(getLocalName() + " <- updating prices");
    }

    private int getPrice() {
        return this.price;
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