package src2;

import jade.core.Agent;
import jade.core.behaviours.*;

public class parallel extends Agent
{
    protected void setup()
    {
        addBehaviour( new Looper( this, 300 ) );
        addBehaviour( new Looper( this, 500 ) );
    }
}

class Looper extends SimpleBehaviour
{
    static String offset = "";
    static long   t0     = System.currentTimeMillis();

    String tab = "" ;
    int    n   = 1;
    long   dt;

    public Looper( Agent a, long dt) {
        super(a);
        this.dt = dt;
        offset += "    " ;
        tab = new String(offset) ;
    }

    public void action()
    {
        System.out.println( tab +
                (System.currentTimeMillis()-t0)/10*10 + ": " +
                myAgent.getLocalName() );
        block( dt );
        n++;
    }

    public  boolean done() {  return n>6;  }

}



class TwoStep extends SimpleBehaviour
{
    public void action()
    {
        block(250);
        System.out.println( "--- Message 1 --- " );
        block(500);
        System.out.println( "    - message 2 " );
        finished = true;
    }

    private boolean finished = false;
    public  boolean done() {  return finished;  }
}