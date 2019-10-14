package src2;

import jade.core.Agent;

public class badPar extends Agent {
    protected void setup()
    {
        addBehaviour( new TwoStep() );
        addBehaviour( new Looper( this, 300 ) );
    }
}