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
    private int available_spots = 2,
            available_budget = 100*available_spots,
            salary  = rnd.nextInt(available_budget / available_spots - 30) + 31;

    private HashMap<String, ACLMessage> currentSenders = new HashMap<String, ACLMessage>();
    HashSet<AID> crew = new HashSet<AID>(); // update with accepted crew members
    private int n_offers_last_cycle = 0;

    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));

                // handle received msg only if it's not from a repeating sender
                if (msg != null && available_spots > 0 && !currentSenders.containsKey(msg.getSender().getLocalName())) {
                    currentSenders.put(msg.getSender().getLocalName(), null);

                    // TODO
                    // in the future, getSalary() will not provide a random number
                    // instead, it will depend on the CrewMember's experience, time until flight takeoff, and number of offers it had in the previous cycle
                    // as such, updateSalary() will not be necessary, as getSalary() will automatically update
                    SequentialBehaviour t = new Transaction(myAgent, msg, getSalary()) {
                        public int onEnd() {
                            ACLMessage barter_reply = getBarterReply();
                            currentSenders.replace(msg.getSender().getLocalName(), barter_reply);
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
        // updates salary standards if none is acceptable
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                // do nothing if there's no finished transactions
                if (currentSenders.size() == 0) return;

                // decide what's the best offer to accept
                // TODO: consider every offer, starting wit the best ones, in a loop where the available_budget/spots is updated accordingly
                ACLMessage best_offer_msg = null;
                int best_offer_value = getMaxSalaryBudget();
                Iterator it = currentSenders.entrySet().iterator();

                while (it.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry) it.next();
                    if (pair.getValue() == null) continue;

                    ACLMessage tmp_msg = (ACLMessage) pair.getValue();
                    if (Integer.parseInt(tmp_msg.getContent()) < best_offer_value) { // TODO: tmp_msg.getContent() will return the worker's resume as well. Parse the msg accordingly.
                        best_offer_value = Integer.parseInt(tmp_msg.getContent());
                        best_offer_msg = tmp_msg;
                    }
                }

                // randomly check if best salary offer is not too high
                boolean bool_accepted_offer = false;
                int measure = rnd.nextInt(getMaxSalaryBudget() - getSalary()) + getSalary(); // will accept anything below the proposed salary
                System.out.println(getLocalName() + " <- will accept anything under $" + measure);
                if (best_offer_value <= available_budget &&
                    best_offer_value <= measure)
                    bool_accepted_offer = true;

                // updates the performative for the best offer
                if(best_offer_msg != null && bool_accepted_offer) {
                    available_budget = available_budget - best_offer_value;
                    available_spots--;
                    best_offer_msg.setPerformative(ACLMessage.AGREE);
                }
                // if there's no best offer, update salary standards
                // TODO: updateSalaryBudget() becomes reduntant when each individual offer starts depending on preferences, instead of a random on existing budget.
                else if (!bool_accepted_offer)
                    updateSalaryBudget();

                // sends the replies to every crew member
                it = currentSenders.entrySet().iterator();
                while(it.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry)it.next();
                    if(pair.getValue() == null) continue;

                    ACLMessage barter_reply = (ACLMessage)pair.getValue();
                    send(barter_reply);
                }

                n_offers_last_cycle = currentSenders.size();
                currentSenders.clear();

                if(available_spots == 0)
                    System.out.println("\n" + getLocalName() + " <- is ready to fly with $" + available_budget + " remaining");

            }
        });
    }

    private void updateSalaryBudget() {
        float multiplier = rnd.nextFloat()*0.3f;
        int p;
        if (available_spots > 1)
            p = this.salary + (int)(this.salary * multiplier);
        else
            p = this.salary - (int)(this.salary * multiplier);

        if (p > (getMaxSalaryBudget())) {
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

    // the max budget for a single crew member cannot exceed the avg salary for a crew member by 50%
    private int getMaxSalaryBudget() {
        int b = (int)(this.salary * 1.5);
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