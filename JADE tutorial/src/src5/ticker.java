package src5;

import jade.core.Agent;
import jade.core.behaviours.*;

public class ticker extends Agent {
    long t0 = System.currentTimeMillis();
    Behaviour loop;

    protected void setup() {
        loop = new TickerBehaviour( this, 300 ) {
            protected void onTick() {
                System.out.println( System.currentTimeMillis()-t0 +
                        ": " + myAgent.getLocalName());
            }
        };

        addBehaviour( loop );
    }
}
