package src2;

import jade.core.Agent;
import jade.core.behaviours.*;

public class blockdt extends Agent
{
    protected void setup()
    {
        addBehaviour( new BlockTwice() );
    }
}


class BlockTwice extends SimpleBehaviour
{
    static long t0 = System.currentTimeMillis();

    public void action()
    {
        System.out.println( "Start: "
                + (System.currentTimeMillis()-t0) );
        block(250);
        System.out.println( "   after block(250): "
                + (System.currentTimeMillis()-t0) );
        block(1000);
        System.out.println( "   after block(1000): "
                + (System.currentTimeMillis()-t0) );
        System.out.println();
    }

    private int n = 0;
    public  boolean done() {  return ++n > 3;  }
}