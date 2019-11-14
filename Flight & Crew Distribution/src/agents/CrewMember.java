package agents;

import behaviours.*;
import behaviours.ReceiverBehaviour;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.tools.sniffer.Message;

import java.util.*;

import static java.lang.Integer.parseInt;

public class CrewMember extends Agent {
  private Random rnd = newRandom();
  private int bestPrice = 9999;
  private ACLMessage bestOffer;

  private ArrayList<String> airplaneNameList = new ArrayList<>();

  private ArrayList<ReceiverBehaviour> airplaneReceiverBehaviourArrayList = new ArrayList<>();
  private ACLMessage msg;
  ParallelBehaviour par;
  MessageTemplate template;

  SequentialBehaviour seq;
  DelayBehaviour delay;

  protected void setup() {
    bestPrice = 9999;
    bestOffer = null;

    seq = new SequentialBehaviour();
    addBehaviour(seq);

    msg = newMsg(ACLMessage.QUERY_REF);

    template = MessageTemplate.and(
            MessageTemplate.MatchPerformative( ACLMessage.INFORM ),
            MessageTemplate.MatchConversationId( msg.getConversationId() ));

    addBigBrotherListener();
  }

  private void addBigBrotherListener(){
    MessageTemplate templateBigBrother = MessageTemplate.and(
            MessageTemplate.MatchPerformative( ACLMessage.INFORM ),
            MessageTemplate.MatchSender( new AID( "big_brother",  AID.ISLOCALNAME )));

    seq.addSubBehaviour(new ReceiverBehaviour(this, 4000, templateBigBrother){
      @Override
      public void handle(ACLMessage msg) {
        if (msg != null ) {
          String [] newAirplaneName = msg.getContent().split(";");
            //System.out.println("new Airplane: ");

          ArrayList<String> newAirplaneList = new ArrayList<>();
          ArrayList<String> removedAirplaneList = new ArrayList<>();

          for (String s : newAirplaneName) {
            if (!airplaneNameList.contains(s)) {
              airplaneNameList.add(s);
              newAirplaneList.add(s);
            }
          }

          addSubBehaviours(newAirplaneList);
          addBigBrotherListener();
        }else{
          addBigBrotherListener();
        }
      }
    });
  }
  private void addSubBehaviours(ArrayList<String> newAirplaneList ){
    boolean newAirplane = false;
    if(newAirplaneList.size() == 0){return;}


    par = new ParallelBehaviour(ParallelBehaviour.WHEN_ALL);


    for (String s : newAirplaneList) {
      newAirplane = true;
      //System.out.println("new Airplane: " + s);

      // TODO send it to every existing Airplane Agent, instead of a static list of Airplane Agents
      msg.addReceiver( new AID( s,  AID.ISLOCALNAME ));
      ReceiverBehaviour airplaneReceiverBehaviour = new ReceiverBehaviour(CrewMember.this, 1000, template) {
        public void handle(ACLMessage msg) {
          if (msg != null) {
            int offer = Integer.parseInt(msg.getContent());
            //System.out.println(getLocalName() + " received new offer: " + msg.getSender().getLocalName() + " | " + offer);
            if (offer < bestPrice) {
              bestPrice = offer;
              bestOffer = msg;
            }
          }
        }
      };

      airplaneReceiverBehaviourArrayList.add(airplaneReceiverBehaviour);
      par.addSubBehaviour(airplaneReceiverBehaviour);

    }

    if(newAirplane){
      seq.addSubBehaviour(par);
      addResponseBehaviour();
      send(msg);
    }
  }
  private void removeSubBehaviours(String [] newAirplaneNameList){

  }
  private void addResponseBehaviour(){
    seq.removeSubBehaviour(delay);
    delay = new DelayBehaviour(this, rnd.nextInt(1000)) {
      public void handleElapsedTimeout() {
        if (bestOffer != null) {
          ACLMessage reply = bestOffer.createReply();
          System.out.println("=="+ getLocalName() + " <- Best Price is $" + bestPrice + " from " + bestOffer.getSender().getLocalName());
          // if ( bestPrice <= 30 ) {

          // RANDOM BASED BARTERING
          if (bestPrice > 30) bestPrice = 30;
          int offer = rnd.nextInt(bestPrice)+1;
          reply.setPerformative( ACLMessage.REQUEST );
          reply.setContent( "" + offer );
          send(reply);
          System.out.println("=="+ getLocalName() + " <- Offering $" + offer + " to " + bestOffer.getSender().getLocalName());

          addAgreeRefuseBehaviours();
          // }
          // else setup();
        }
      }
    };
    seq.addSubBehaviour(delay);
  }

  private void addAgreeRefuseBehaviours(){
    MessageTemplate receiverTemplate = MessageTemplate.and(
            MessageTemplate.MatchConversationId(msg.getConversationId()), MessageTemplate.or(
                    MessageTemplate.MatchPerformative(ACLMessage.AGREE), MessageTemplate.MatchPerformative(ACLMessage.REFUSE)
            ));

    seq.addSubBehaviour(new ReceiverBehaviour(this, 5000, receiverTemplate){
      public void handle(ACLMessage msg) {
        if (msg != null ) {
          if( msg.getPerformative() == ACLMessage.AGREE)
            System.out.println("\n==" + getLocalName() + " <- GOT ACCEPTED by " + msg.getSender().getLocalName());
          else {
            System.out.println("\n==" + getLocalName() + " <- GOT REJECTED by " + msg.getSender().getLocalName());
            setup();
          }
        }
        else {
          System.out.println("==" + getLocalName()
                  +" timed out... setting up again");
          //setup();
        }
      }
    });

  }
  // ========== Utility methods =========================
  //  --- generating Conversation IDs -------------------
  private static int cidCnt = 0;
  private String cidBase ;

  private String genCID() {
    if (cidBase==null) {
      cidBase = getLocalName() + hashCode() +
              System.currentTimeMillis()%10000 + "_";
    }
    return  cidBase + (cidCnt++);
  }

  //  --- Methods to initialize ACLMessages -------------------
  ACLMessage newMsg( int perf, String content, AID dest) {
    ACLMessage msg = newMsg(perf);
    if (dest != null) msg.addReceiver( dest );
    msg.setContent( content );
    return msg;
  }

  private ACLMessage newMsg(int perf) {
    ACLMessage msg = new ACLMessage(perf);
    msg.setConversationId( genCID() );
    return msg;
  }

  private Random newRandom() {
    return new Random( hashCode() + System.currentTimeMillis());
  }

  // Garbage Disposal
  class GCAgent extends TickerBehaviour {
    Set seen = new HashSet(),
            old  = new HashSet();

    GCAgent(Agent a, long dt) { super(a,dt); }

    protected void onTick() {
      ACLMessage msg = myAgent.receive();
      while (msg != null) {
        if (! old.contains(msg))
          seen.add( msg);
        else {
          System.out.println("==" + getLocalName() + " <- Flushing message:");
          dumpMessage( msg );
        }
        msg = myAgent.receive();
      }

      for(Iterator it = seen.iterator(); it.hasNext(); )
        myAgent.putBack( (ACLMessage) it.next() );

      old.clear();
      Set tmp = old;
      old = seen;
      seen = tmp;
    }
  }
  static long t0 = System.currentTimeMillis();

  private void dumpMessage(ACLMessage msg) {
    System.out.print( "t=" + (System.currentTimeMillis()-t0)/1000F + " in "
            + getLocalName() + ": "
            + ACLMessage.getPerformative(msg.getPerformative() ));

    System.out.print( "  from: " +
            (msg.getSender()==null ? "null" : msg.getSender().getLocalName())
            +  " --> to: ");

    for (Iterator it = msg.getAllReceiver(); it.hasNext();)
      System.out.print( ((AID) it.next()).getLocalName() + ", ");
    System.out.println( "  cid: " + msg.getConversationId());
    System.out.println( "  content: " +  msg.getContent());
  }
}