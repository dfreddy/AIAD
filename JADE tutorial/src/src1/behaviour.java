package src1;

import jade.core.Agent;
import jade.core.behaviours.SimpleBehaviour;

public class behaviour extends Agent {

	protected void setup() {
        addBehaviour( new B1( this ) );
    }
} 

class B1 extends SimpleBehaviour 
{   
    public B1(Agent a) { 
         super(a);  
    }
              
    public void action() {
       System.out.println( "Hello World! My name is " + 
              myAgent.getLocalName() );
    }
    
    private boolean finished = false;
    
    public  boolean done() {
    	return finished;
    	}
}
