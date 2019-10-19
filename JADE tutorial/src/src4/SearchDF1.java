package src4;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.core.AID;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.*;
import jade.domain.FIPAException;

public class SearchDF1 extends Agent {
    protected void setup()
    {
        ServiceDescription sd  = new ServiceDescription();
        sd.setType( "buyer" );
        sd.setName( getLocalName() );
        register( sd );

        try {
            DFAgentDescription dfd = new DFAgentDescription();
            DFAgentDescription[] result = DFService.search(this, dfd);

            System.out.println("Search returns: " + result.length + " elements" );
            if (result.length>0)
                System.out.println(" " + result[0].getName() );


            sd  = new ServiceDescription();
            sd.setType( "buyer" );
            dfd.addServices(sd);
            result = DFService.search(this, dfd);
            System.out.println("Search for BUYER: " + result.length + " elements" );
            if (result.length>0)
                System.out.println(" " + result[0].getName() );

            sd.setType( "seller" );
            result = DFService.search(this, dfd);
            if (result==null) System.out.println("Search1 returns null");
            else {
                System.out.println("Search for SELLER: " + result.length + " elements" );
                if (result.length>0)
                    System.out.println(" " + result[0].getName() );
            }
        }
        catch (FIPAException fe) { fe.printStackTrace(); }

        System.exit(0);
    }

    void register( ServiceDescription sd) {
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        dfd.addServices(sd);

        try {
            DFService.register(this, dfd );
        }
        catch (FIPAException fe) { fe.printStackTrace(); }
    }

    AID getService( String service ) {
        DFAgentDescription dfd = new DFAgentDescription();
        ServiceDescription sd = new ServiceDescription();
        sd.setType( service );
        dfd.addServices(sd);
        try
        {
            DFAgentDescription[] result = DFService.search(this, dfd);
            if (result.length>0)
                return result[0].getName() ;
        }
        catch (Exception fe) {}
        return null;
    }

}