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
    private int price  = rnd.nextInt(99) + 1;
    //    transactions = 0,
    //    results = 0;

    private int available_spots = 1,
        available_budget = 100;

    private HashMap<String, ACLMessage> currentSenders = new HashMap<String, ACLMessage>();
    HashSet<AID> crew = new HashSet<AID>(); // update with accepted crew members

    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));
                // handle received msg only if it's not from a repeating sender
                if (msg != null && available_spots > 0 && !currentSenders.containsKey(msg.getSender().getLocalName())) {
                    currentSenders.put(msg.getSender().getLocalName(), null);

                    SequentialBehaviour t = new Transaction(myAgent, msg, getPrice()) {
                        public int onEnd() {
                            // int r = getResult();
                            // transactions++;
                            // results += r;

                            // if the offer is agreeable, store it for later
                            // if (r == 0) {
                                ACLMessage barter_reply = getBarterReply();
                                currentSenders.replace(msg.getSender().getLocalName(), barter_reply);
                            // }

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

        // ticker behaviour closes out all existing transactions
        // decides which proposal to accept
        // updates price standards if none is acceptable
        addBehaviour(new TickerBehaviour(this, 5000) {
            protected void onTick() {
                if (currentSenders.size() == 0) return;

                // decide what's the best offer to accept
                ACLMessage best_offer_msg = null;
                int best_offer_value = price/2; // for now, assume the minimum accepted value is half the price

                Iterator it = currentSenders.entrySet().iterator();
                while(it.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry)it.next();
                    if(pair.getValue() == null) continue;

                    ACLMessage tmp_msg = (ACLMessage)pair.getValue();
                    if(Integer.parseInt(tmp_msg.getContent()) >= best_offer_value) {
                        best_offer_value = Integer.parseInt(tmp_msg.getContent());
                        best_offer_msg = tmp_msg;
                    }
                }

                // updates the performative for the best offer
                if(best_offer_msg != null) {
                    available_spots--;
                    best_offer_msg.setPerformative(ACLMessage.AGREE);
                }
                // if there's no best offer, update prices
                else updatePrice();

                // sends the replies to every crew member
                it = currentSenders.entrySet().iterator();
                while(it.hasNext()) {
                    HashMap.Entry pair = (HashMap.Entry)it.next();
                    if(pair.getValue() == null) continue;

                    ACLMessage barter_reply = (ACLMessage)pair.getValue();
                    send(barter_reply);
                }
                currentSenders.clear();

                // System.out.println(myAgent.getLocalName() + "\nnr transactions: " + transactions + "\nResults: " + results);
                // update price standards if all transactions failed
                // if(results == transactions) updatePrice();

                // reset all transaction related variables
                // results = 0;
                // transactions = 0;
            }
        });
    }

    private void updatePrice() {
        int p = (int)(this.price * 0.6);
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