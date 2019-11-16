package agents;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import behaviours.*;

import java.lang.reflect.Array;
import java.util.*;

public class Airplane extends Agent {
    private Random rnd = newRandom();
    private int min_salary = 30, max_salary = 100, // TODO: make min_salary variable --- make max_salary vary wit the min_salary
            available_spots = 2,
            available_budget = max_salary * available_spots,
            salary = rnd.nextInt(max_salary - 30) + 31, // TODO: remove
            initial_salary = salary, // TODO: remove
            initial_time_to_takeoff = 15000 + 5000 * (rnd.nextInt(5)), // initial time until takeoff varies between 15s and 40s
            time_to_takeoff = initial_time_to_takeoff,
            flight_length = rnd.nextInt(12) + 1; // TODO: replace wit one from branch

    private Map<ACLMessage, Float> currentTransactions = new HashMap<ACLMessage, Float>(); // <reply, rank>
    HashSet<AID> crew = new HashSet<AID>(); // updates with accepted crew members
    private int n_offers_last_cycle = 0;

    protected void setup() {
        System.out.println(getLocalName() + " <- time to takeoff = " + time_to_takeoff / 1000 + "s");

        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));

                // handle received msg only if it's not from a repeating sender
                if (msg != null && available_spots > 0 && !currentTransactions.containsKey(msg.getSender().getLocalName())) {
                    float cm_exp = Float.parseFloat(msg.getContent());

                    SequentialBehaviour t = new Transaction(myAgent, msg, getSalaryForCrewMember(cm_exp), flight_length) {
                        public int onEnd() {
                            ACLMessage barter_reply = getBarterReply();
                            float exp = getExperience();
                            currentTransactions.put(barter_reply, exp);

                            return super.onEnd();
                        }
                    };

                    addBehaviour(t);
                }
                else block();
            }
        });

        addBehaviour(new GCAgent(this, 5000));

        // ticker behaviour closes out all existing transactions
        // decides which proposal to accept
        addBehaviour(new TickerBehaviour(this, 4000) {
            protected void onTick() {
                // do nothing if there's no finished transactions
                if (currentTransactions.size() == 0) return;

                time_to_takeoff = time_to_takeoff - 4000;

                // decide what's the best offer to accept
                // considers every offer, starting wit the CrewMembers wit higher rank, in a loop where the available_budget/spots is updated accordingly
                ACLMessage best_offer_msg = null;
                int best_offer_value = getMaxSalaryBudget();
                Iterator it = currentTransactions.entrySet().iterator();

                // TODO:
                //  rate proposal! n decide if it's worth it
                //  take into consideration available_budget and spots
                //  update performative for the reply if necessary
                while (it.hasNext()) {
                    Map.Entry pair = (Map.Entry) it.next();
                    if (pair.getKey() == null) continue;
                    
                    ACLMessage tmp_msg = (ACLMessage) pair.getKey();
                    if (Integer.parseInt(tmp_msg.getContent()) < best_offer_value) {
                        best_offer_value = Integer.parseInt(tmp_msg.getContent());
                        best_offer_msg = tmp_msg;
                    }
                }

                // randomly check if best salary offer is not too high
                boolean bool_accepted_offer = false;
                int measure = rnd.nextInt(getMaxSalaryBudget() - getSalary() + 1) + getSalary(); // will accept anything below the proposed salary
                System.out.println(getLocalName() + " <- will accept anything under $" + measure);
                if (best_offer_value <= available_budget &&
                        best_offer_value <= measure) {
                    bool_accepted_offer = true;
                    System.out.println(getLocalName() + " <- accepted offer for " + best_offer_value);
                }
                // updates the performative for the best offer
                if (best_offer_msg != null && bool_accepted_offer) {
                    available_budget = available_budget - best_offer_value;
                    available_spots--;
                    best_offer_msg.setPerformative(ACLMessage.AGREE);
                    crew.add((AID)best_offer_msg.getAllReceiver().next());
                }


                // sends the replies to every crew member
                it = currentTransactions.entrySet().iterator();
                while (it.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry) it.next();
                    if (pair.getKey() == null) continue;

                    ACLMessage barter_reply = (ACLMessage) pair.getKey();
                    send(barter_reply);
                }

                n_offers_last_cycle = currentTransactions.size();
                currentTransactions.clear();



                // update time to takeoff
                // time_to_takeoff = time_to_takeoff - 4000; -- done above --
                System.out.println(getLocalName() + " <- time to takeoff is now " + time_to_takeoff / 1000 + "s\n");

                if (available_spots == 0) {
                    System.out.println("\n" + getLocalName() + " <- is ready to fly with $" + available_budget + " remaining");
                    doDelete();
                }

            }
        });
    }

    // depending on time_to_takeoff and CM rank
    private int getSalaryForCrewMember(float exp) {
        int sal;
        int diff = initial_time_to_takeoff - time_to_takeoff;
        double percent_time_to_takeoff = (100*diff / initial_time_to_takeoff);
        percent_time_to_takeoff = percent_time_to_takeoff / 100;

        // time left until takeoff has more influence on salary variation
        // relatively arbitrary numbers
        sal = (int) (min_salary/2 + ((max_salary-min_salary)*0.6 * exp) + ((max_salary-min_salary)*0.6 * percent_time_to_takeoff));

        return sal;
    }


    private void updateSalaryBudget() {
        float percent_time_to_takeoff = time_to_takeoff/initial_time_to_takeoff;
        float multiplier = rnd.nextFloat() * percent_time_to_takeoff / 4; // 4 is arbitrary here
        int p;

        p = this.initial_salary + (int)(this.initial_salary * multiplier);

        if (p > (getMaxSalaryBudget()) && available_spots > 1) { // is willing to go to debt to fill in every spot
            this.salary = getMaxSalaryBudget();
            System.out.println(getLocalName() + " <- is at max offer: $" + this.salary);
        }
        else {
            this.salary = p;
            System.out.println(getLocalName() + " <- updating offer: $" + this.salary + " --- available budget: $" + this.available_budget);
        }
    }

    private int getSalary() {
        return this.salary;
    }

    // the max budget for a single crew member cannot exceed the predicted salary for a crew member by 50%
    private int getMaxSalaryBudget() {
        int b = (int)(this.max_salary * 1.5);
        if (b > available_budget)
            b = available_budget;

        return b;
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
                    System.out.println("==" + getLocalName() + " <- Flushing message:");
                    dumpMessage( msg );
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