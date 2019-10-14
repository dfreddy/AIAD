package src2;

import jade.core.Agent;
import jade.core.behaviours.*;

public class goodPar extends Agent
{

    protected void setup()
    {
        addBehaviour( new Step1() );
        addBehaviour( new Looper( this, 300 ) );
    }


    class Step1 extends SimpleBehaviour
    {
        int state = 0;

        public void action()
        {
            if (state==0)
                block( 200 );
            else {
                System.out.println( "--- Message 1 --- " );
                addBehaviour( new Step2() );
            }
            state++;
        }

        public  boolean done() {  return state>1;  }
    }

    class Step2 extends SimpleBehaviour
    {
        int state = 0;

        public void action()
        {
            if (state==0)
                block( 600 );
            else {
                System.out.println( "    - message 2 " );
                doDelete();   // applies to the Agent
            }
            state++;
        }

        public  boolean done() {  return state>1;  }
    }

}