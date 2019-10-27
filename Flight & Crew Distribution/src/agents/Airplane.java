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

    // int available_spots;
    // int available_money;

    protected void setup() {
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