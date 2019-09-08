import com.google.gson.Gson;
import common.Plan;
import common.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ZMQProcess_LoadBalancer extends ZMQProcess {

    private static final Logger logger = LogManager.getLogger();

    // Address and port of server frontend
    private String ip;
    private int frontend_port;
    private int backend_port;

    // socket indices
    private final int FRONTEND_INDEX = 0;
    private final int BACKEND_INDEX = 1;

    private ArrayList<Plan> plan = new ArrayList<>();

    ZMQProcess_LoadBalancer(String ip, int frontendPort, int backendPort){
        super("LoadBalancer");
        this.ip = ip;
        this.frontend_port = frontendPort;
        this.backend_port = backendPort;
    }


    @Override
    protected List<ZMQ.Socket> bindAndConnectSockets(ZContext context) {
        ZMQ.Socket[] socketArray = new ZMQ.Socket[2];
        String frontendAddress = "tcp://" + ip + ":" + frontend_port;
        String backendAddress = "tcp://" + ip + ":" + backend_port;
        logger.info("The frontend address of the LoadBalancer: " + frontendAddress);
        logger.info("The backend address of the LoadBalancer: " + backendAddress);

        ZMQ.Socket frontend = context.createSocket(SocketType.ROUTER);
        frontend.setHWM(10000);
        frontend.connect(frontendAddress);
        frontend.setIdentity(identity.getBytes());
        frontend.setSendTimeOut(1);
        socketArray[FRONTEND_INDEX] = frontend;

        ZMQ.Socket backend = context.createSocket(SocketType.ROUTER);
        backend.setHWM(10000);
        backend.setIdentity(backendAddress.getBytes(ZMQ.CHARSET));
        backend.bind(backendAddress);
        backend.setSendTimeOut(1);
        socketArray[BACKEND_INDEX] = backend;

        return Arrays.asList(socketArray);
    }

    @Override
    protected void processZMsg(int socketIndex, ZMsg msg) {
        switch (socketIndex) {
            case BACKEND_INDEX:
                ZFrame frame = msg.pollLast();

                if (frame.toString().equals(ZMsgType.ZPING.toString())){
                    msg.addLast(ZMsgType.ZPONG.toString());
                    msg.send(sockets.get(BACKEND_INDEX));
                    break;
                }
                if (frame.toString().equals(ZMsgType.PLAN.toString())){
                    updatePlan(msg);
                }

                if (!msg.send(sockets.get(FRONTEND_INDEX))) {
                    logger.warn("Dropping response to client as HWM reached.");
                }
                break;
            case FRONTEND_INDEX:
                sendToBackend(msg);
                break;
            default:
                logger.error("Cannot process message for socket at index {}, as this index is not known.", socketIndex);
        }
    }

    private void sendToBackend(ZMsg msg){

        //TODO send to specific gb servers
        if (!msg.send(sockets.get(BACKEND_INDEX))) {
            logger.warn("Dropping client request as HWM reached.");
        }
    }

    private void updatePlan(ZMsg msg){
        plan.clear();
        ZFrame frame = msg.pollLast();
        Gson gson = new Gson();
        plan = gson.fromJson(frame.toString(), ArrayList.class);
    }

    //region shutdownCompleted
    @Override
    protected void shutdownCompleted() {
        logger.info("Shut down ZMQProcess_Server {}", "");
    }
    //endregion
}
