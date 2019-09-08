package plan;

import common.Plan;
import common.ServerLoadMetrics;
import common.ZMsgType;
import de.hasenburg.geobroker.commons.communication.ZMQProcess;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlanServer extends ZMQProcess {

    private static final Logger logger = LogManager.getLogger();

    private final String frontendAddress = "tcp://127.0.0.1:6061";
    private final String backendAddress = "tcp://127.0.0.1:6062";
    private ArrayList<String> loadBalancers;
    private ArrayList<Plan> plans = new ArrayList<>();

    private ZContext context;
    private ZMQ.Socket frontend;
    private ZMQ.Socket backend;
    private ZMQ.Socket pipe;

    // socket indices
    private final int FRONTEND_INDEX = 0;
    private final int BACKEND_INDEX = 1;
    private final int PIPE_INDEX = 2;

    // Address and port of server
    private String ip;
    private int port;

    PlanServer(String ip, int port, String brokerId) {
        super(getServerIdentity(brokerId));
        this.ip = ip;
        this.port = port;
    }

    @Override
    protected void processZMsg(int socketIndex, ZMsg msg) {
        switch (socketIndex) {
            case BACKEND_INDEX:
                handleBackendMessages();
                break;
            case FRONTEND_INDEX:

                break;
            case PIPE_INDEX:
                sendPlan();
                break;
            default:
                logger.error("Cannot process message for socket at index {}, as this index is not known.", socketIndex);
        }
    }


    @Override
    protected List<ZMQ.Socket> bindAndConnectSockets(ZContext context) {
        ZMQ.Socket[] socketArray = new ZMQ.Socket[3];

        frontend = context.createSocket(SocketType.ROUTER);
        frontend.setHWM(10000);
        frontend.setIdentity(frontendAddress.getBytes(ZMQ.CHARSET));
        frontend.connect(frontendAddress);
        frontend.setSendTimeOut(1);
        socketArray[FRONTEND_INDEX] = frontend;

        backend = context.createSocket(SocketType.ROUTER);
        backend.setHWM(10000);
        backend.setIdentity(backendAddress.getBytes(ZMQ.CHARSET));
        backend.bind(backendAddress);
        backend.setSendTimeOut(1);
        socketArray[BACKEND_INDEX] = backend;

        PlanAnalyzerAgent agent = new PlanAnalyzerAgent();
        Object[] args = new Object[1];
        pipe = ZThread.fork(context, agent, args);
        socketArray[PIPE_INDEX] = pipe;

        return Arrays.asList(socketArray);
    }

    private void handleBackendMessages(){
        updateMetrics();
    }

    private void updateMetrics(){
        //TODO fill in loadMetrics list with server metrics
    }


    private void sendPlan(){
        for(String lbId: loadBalancers){
            ZMsg msgPlan = new ZMsg();
            msgPlan.add(new ZFrame(lbId));
            msgPlan.add(ZMsgType.PLAN.toString());
            msgPlan.add(plans.toString());
            msgPlan.send(frontend);
        }
    }

    @Override
    protected void utilizationCalculated(double utilization) {

    }

    @Override
    protected void shutdownCompleted() {
        logger.info("Shut down ZMQProcess_Server {}", getServerIdentity(identity));
    }

    public static String getServerIdentity(String brokerId) {
        return brokerId + "-server";
    }


}
