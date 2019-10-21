package src5;

import jade.core.Agent;
import jade.core.behaviours.*;

public class waker extends Agent {
    long t0 ;
    long time() { return System.currentTimeMillis()-t0; }

    protected void setup() {
        addBehaviour( new WakerBehaviour( this, 250 ) {
            protected void onWake() {
                System.out.println(time() + ": " + "... Message1");
                addBehaviour(new WakerBehaviour(myAgent, 500) {
                    protected void onWake() {
                        System.out.println(time() + ": " + "  ...and then Message 2");
                    }
                });
            }
        });

        t0 = System.currentTimeMillis();
    }
}
