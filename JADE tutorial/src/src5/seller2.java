package src5;

// RUN -name platform james:src5.buyer2;russ:src5.buyer2;s1:src5.seller2;s2:src5.seller2;s3:src5.seller2

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.*;
import java.util.Random;

public class seller2 extends Agent {
    Random rnd = newRandom();
    int price  = rnd.nextInt(100);

    // int stock/available_spots;
    // int income;
    // boolean transaction_done = false, transaction_failed = false;

    protected void setup() {
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = receive(MessageTemplate.MatchPerformative(ACLMessage.QUERY_REF));
                if (msg != null) { // handle received msg
                    addBehaviour(new transaction(myAgent, msg, getPrice()));
                }
                else block();
            }
        });

        addBehaviour(new TickerBehaviour(this, 2000) {
            protected void onTick() {
                updatePrice();
            }
        });
    }

    private void updatePrice() {
        int p = (int)(this.price * 0.9);
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
}