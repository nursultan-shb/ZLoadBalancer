package ms.shabykeev.loadbalancer.plan.generator;

import ms.shabykeev.loadbalancer.plan.metricsQueue.MetricsHolder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

public class GeneratorAgent implements ZThread.IAttachedRunnable {
    private static final Logger logger = LogManager.getLogger();

    // socket indices
    private final int PIPE_INDEX = 0;
    private final int PAIR_SOCKET_INDEX = 1;

    private Long lastPlanGenerationTime = 0L;
    private Integer planGenerationDelay = 10*1000; //millis

    private MetricsHolder metricsHolder;

    private static final String PAIR_SOCKET_ADDRESS = "inproc://plan2";


    @Override
    public void run(Object[] args, ZContext context, ZMQ.Socket pipe) {

        metricsHolder = new MetricsHolder(context, pipe, PAIR_SOCKET_ADDRESS);
        Generator generator = new Generator(context, PAIR_SOCKET_ADDRESS);
        generator.start();

        ZMQ.Poller poller = context.createPoller(2);
        poller.register(metricsHolder.pipe, ZMQ.Poller.POLLIN);
        poller.register(metricsHolder.pairSocket, ZMQ.Poller.POLLIN);

        while (!Thread.currentThread().isInterrupted()) {
            poller.poll(100);

            if (poller.pollin(PIPE_INDEX)) {
                metricsHolder.addMessage();
            }

            if (poller.pollin(PAIR_SOCKET_INDEX)) {
                metricsHolder.sendPlan();
            }

            if (System.currentTimeMillis() - lastPlanGenerationTime >= planGenerationDelay) {
                logger.info("time for sending metrics");
                metricsHolder.sendMetrics();
                lastPlanGenerationTime = System.currentTimeMillis();
            }
        }
    }
}
