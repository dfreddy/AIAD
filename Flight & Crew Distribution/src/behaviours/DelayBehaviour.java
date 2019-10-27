package behaviours;

import jade.core.behaviours.*;
import jade.lang.acl.*;
import jade.core.*;

public class DelayBehaviour extends SimpleBehaviour {
    private long timeout, wakeupTime;
    private boolean finished;

    public DelayBehaviour(Agent a, long timeout) {
        super(a);
        this.timeout = timeout;
        finished = false;
    }

    public void onStart() {
        wakeupTime = System.currentTimeMillis() + timeout;
    }

    public void action() {
        long dt = wakeupTime - System.currentTimeMillis();
        if (dt <= 0) {
            finished = true;
            handleElapsedTimeout();
        } else
            block(dt);

    } //end of action

    protected void handleElapsedTimeout() {}

    public void reset(long timeout) {
        wakeupTime = System.currentTimeMillis() + timeout ;
        finished = false;
    }

    public boolean done() {
        return finished;
    }
}