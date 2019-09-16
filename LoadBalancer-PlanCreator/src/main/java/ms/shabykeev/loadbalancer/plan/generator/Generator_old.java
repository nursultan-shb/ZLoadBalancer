package ms.shabykeev.loadbalancer.plan.generator;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

public class Generator_old extends Thread {
    private static final Logger logger = LogManager.getLogger();

    private ZContext ctx;
    public ZMQ.Socket pairSocket;
    public ZMQ.Socket pipe;

    private Long lastPlanGenerationTime = 0L;
    private Integer planGenerationDelay = 0; //millis
    private Integer planNumber = 0;

    public Generator_old(ZContext context, Integer planGenerationDelay, ZMQ.Socket pipe){
        this.ctx = context;
        this.pipe = pipe;
        this.planGenerationDelay = 1000*planGenerationDelay;
        Thread.currentThread().setName("plan-generator");

        pairSocket = ctx.createSocket(SocketType.PAIR);
        pairSocket.connect("inproc://plan2");
    }

    @Override
    public void run()
    {
        while (!Thread.currentThread().isInterrupted()) {
            logger.info("delta :" + (System.currentTimeMillis() - lastPlanGenerationTime) );
            if (System.currentTimeMillis() - lastPlanGenerationTime >= planGenerationDelay){
                ZMsg msg = new ZMsg();
                msg.add(planNumber.toString());
                msg.send(pairSocket);
                logger.info("asking for metrics " + planNumber);

                lastPlanGenerationTime = System.currentTimeMillis();

                try{
                    Thread.sleep(planGenerationDelay);
                }
                catch (InterruptedException e){
                    logger.error(e.getMessage());
                }

            }
        }
    }

    public void createPlan(){
        String metrics = pairSocket.recvStr();
        logger.info("received queue" + metrics.toString());
        sendPlan();
    }

    public void sendPlan(){
        ZMsg msg = new ZMsg();
        msg.add("send plan " + planNumber);
        msg.send(pipe);
        planNumber ++;
    }

}
