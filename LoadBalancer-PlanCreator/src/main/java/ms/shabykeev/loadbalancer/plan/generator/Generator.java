package ms.shabykeev.loadbalancer.plan.generator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class Generator extends Thread {

    private static final Logger logger = LogManager.getLogger();

    private ZContext ctx;
    public ZMQ.Socket pairSocket;

    private Integer planNumber = 0;

    private final int PAIR_SOCKET_INDEX = 0;

    public Generator(ZContext context, String socketAddress){
        this.ctx = context;
        Thread.currentThread().setName("plan-generator");

        pairSocket = ctx.createSocket(SocketType.PAIR);
        pairSocket.bind("inproc://plan2");
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String msg = pairSocket.recvStr();
            createPlan(msg);
        }
    }

    public void createPlan(String metrics){
        logger.info("started plan creation" + metrics.toString());
        //simulate work
        try{
            Thread.sleep(4000);
        }
        catch (Exception e){
            logger.error(e.getMessage());
        }

        logger.info("finished plan creation");
        sendPlan();
    }

    public void sendPlan(){
        ZMsg msg = new ZMsg();
        msg.add("send plan " + planNumber);
        msg.send(pairSocket);
        planNumber ++;
    }

}
