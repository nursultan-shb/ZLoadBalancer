import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

public class LoadBalancerLogic implements IServerLogic{

    private static final Logger logger = LogManager.getLogger();
    private ZMQProcessManager processManager;

    private static final int FRONTEND_PORT = 7225;
    private static final int BACKEND_PORT = 5559;

    @Override
    public void initializeFields() {

        processManager = new ZMQProcessManager();
    }

    @Override
    public void startServer(){
        ZMQProcessStarter.runZMQProcess_LoadBalancer(processManager,
                "0.0.0.0", FRONTEND_PORT, BACKEND_PORT, "1");

        logger.info("Started a load balancer successfully on port: {}", FRONTEND_PORT);
    }

    @Override
    public void serverIsRunning() {
        AtomicBoolean keepRunning = new AtomicBoolean(true);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> keepRunning.set(false)));

        while (keepRunning.get()) {
            Utility.sleepNoLog(200000, 0);
        }
    }

    @Override
    public void cleanUp() {
        processManager.tearDown(2000);
        logger.info("Tear down completed");
    }
}
