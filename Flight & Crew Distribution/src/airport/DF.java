package airport;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

public class DF extends Agent {
    protected void setup() {
        /*
        ServiceDescription sd  = new ServiceDescription();
        sd.setType( "buyer" );
        sd.setName( getLocalName() );
        register( sd );
        */
    }

    void register( ServiceDescription sd) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd );
        } catch (FIPAException fe) { fe.printStackTrace(); }
    }

    protected void takeDown() {
        try { DFService.deregister(this); }
        catch (Exception e) {}
    }
}
