package plan;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZThread;

public class PlanAnalyzerAgent implements ZThread.IAttachedRunnable {
    @Override
    public void run(Object[] args, ZContext ctx, ZMQ.Socket pipe) {
        PlanAnalyzer planAnalyzer = new PlanAnalyzer(ctx, pipe);
        ZMQ.Poller poller = ctx.createPoller(1);
        poller.register(planAnalyzer.pipe, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            int rc = poller.poll(1000);
            if (rc == -1)
                break; //  Context has been shut down

            if (poller.pollin(0))
                planAnalyzer.receiveMessage();
        }
    }
}
