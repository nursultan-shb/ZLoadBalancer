
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ZMQProcessStarter {
    private static final Logger logger = LogManager.getLogger();

    public static ZMQProcess_LoadBalancer runZMQProcess_LoadBalancer(ZMQProcessManager processManager,
                                                                     String ip, int frontendPort, int backendPort,
                                                         String brokerId) {
        ZMQProcess_LoadBalancer zmqProcess = new ZMQProcess_LoadBalancer(ip, frontendPort, backendPort);
        processManager.submitZMQProcess("1", zmqProcess);
        return zmqProcess;
    }
}
