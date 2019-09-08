
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ZMQProcessManager {

    private static final Logger logger = LogManager.getLogger();

    private final ExecutorService pool = Executors.newCachedThreadPool();
    private final ConcurrentHashMap<String, Future<?>> zmqProcesses = new ConcurrentHashMap<>();

    private final ZContext context;

    public ZMQProcessManager() {
        context = new ZContext(1);
        logger.info("Started ZMQProcessManager");
    }
    /**
     * Tries to tear down ZMQProcessManager. If futures do not complete in the given time, returns false and does not
     * kill the context.
     *
     * @param timeout - timeout in microseconds
     * @return true if teared down in timeout time
     */
    public boolean tearDown(int timeout) {

        if (!context.getSockets().isEmpty()) {
            logger.warn("There are still open sockets in ZContext: {}. " +
                            "They are being closed now, but is it intended that they are still open?",
                    context.getSockets());
        }

        context.destroy();
        logger.info("Teared down ZMQProcessManager");
        return true;
    }


    public ZContext getContext() {
        return context;
    }

    public void submitZMQProcess(String identity, ZMQProcess process) {
        if (getIncompleteZMQProcesses().contains(identity)) {
            logger.error("Cannot start ZMQProcess with identity {}, as one with this identity already exists",
                    identity);
            return;
        }
        process.init(context);
        Future<?> future = pool.submit(process);
        zmqProcesses.put(identity, future);
        logger.info("Started {} with identity {}", process.getClass().getSimpleName(), identity);

    }

    public List<String> getIncompleteZMQProcesses() {
        List<String> list = new ArrayList<>();
        for (Map.Entry<String, Future<?>> entry : zmqProcesses.entrySet()) {
            if (!entry.getValue().isDone()) {
                list.add(entry.getKey());
            } else {
                // if not active anymore, remove
                logger.debug("ZMQProcess {} has completed", entry.getKey());
                zmqProcesses.remove(entry.getKey());
            }
        }
        logger.trace("These processes are incomplete: " + list);
        return list;
    }
}
