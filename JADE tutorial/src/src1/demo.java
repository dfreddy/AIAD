package src1;

import jade.core.Agent;

public class demo extends Agent {
	protected void setup() {
	  	System.out.println("Hello World! My name is "+getLocalName());
	  	
	  	// Make this agent terminate
	  	while(true) {}
	  } 

}
