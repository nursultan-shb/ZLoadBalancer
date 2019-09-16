package serverProcess;

import ms.shabykeev.loadbalancer.common.ZHelper;
import ms.shabykeev.loadbalancer.common.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.*;

public class ZClient implements ZThread.IDetachedRunnable {

    private static final Logger logger = LogManager.getLogger();

    private final String clientIdentity = "client";
    private final String loadBalancerAddress = "tcp://" + "0.0.0.0" + ":" + "5559";
    private final byte[] loadBalancerIdentity = loadBalancerAddress.getBytes(ZMQ.CHARSET);
    private boolean isLoadBalancerAvailable = false;

    //region run
    @Override
    public void run(Object[] args)
    {
        //  Prepare our context and sockets
        try {
            ZContext context = new ZContext();
            ZMQ.Socket client = context.createSocket(SocketType.ROUTER);
            ZHelper.setId(clientIdentity, client); //  Set a printable identity
            client.connect(loadBalancerAddress);

            ZMQ.Poller poller = context.createPoller(1);
            poller.register(client, ZMQ.Poller.POLLIN);
            int attempt = 0;
            while (!Thread.currentThread().isInterrupted()) {
                ZMsg pingMsg = getPingMessage();
                pingMsg.send(client);
                attempt++;
                poller.poll(10*1000);
                 if (poller.pollin(0)){
                     ZMsg reply = ZMsg.recvMsg(client);
                     System.out.println(reply.toString());
                     System.out.println("Attempt=" + attempt);
                     if (reply.getLast().toString().equals(ZMsgType.ZPONG.toString())){
                         this.isLoadBalancerAvailable = true;
                         break;
                     }
                }

            }


            ZMsg msg = new ZMsg();
            msg.push("CONN");
            msg.push(loadBalancerIdentity);
            msg.send(client);
        }
        catch (Exception e){
            logger.error(e.getMessage());
        }
    }
    //endregion

    //region getPingMessage
    private ZMsg getPingMessage(){
        ZMsg msg = new ZMsg();
        msg.push(ZMsgType.ZPING.toString());
        msg.push(loadBalancerIdentity);

        return msg;
    }
    //endregion

    public static void main(String[] args){
        ZThread.start(new ZClient());

        while (true){

        }
    }
}
