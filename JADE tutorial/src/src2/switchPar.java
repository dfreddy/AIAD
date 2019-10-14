package src2;

import jade.core.Agent;
import jade.core.behaviours.*;

public class switchPar extends Agent
{

    protected void setup()
    {
        addBehaviour( new TwoSteps() );
        addBehaviour( new Looper( this, 300 ) );
    }

    protected void takeDown()
    {
        System.exit(0);
    }


    class TwoSteps extends SimpleBehaviour
    {
        int state = 1;

        public void action()
        {
            switch( state) {
                case 1:
                    block( 200 );
                    break;

                case 2:
                    System.out.println( "--- Message 1 --- " );
                    block( 800 );
                    break;

                case 3:
                    System.out.println( "  -- message 2 --" );
                    finished = true;
                    doDelete();   // applies to the Agent
            }
            state++;
        }

        private boolean finished = false;
        public  boolean done() {  return finished;  }
    }

}