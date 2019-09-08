import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.List;

public abstract class ZMQProcess implements Runnable {

    private static final Logger logger = LogManager.getLogger();
    private static final int TIMEOUT_SECONDS = 3; // logs when not received in time, but repeats

    private ZContext context = null;

    protected final String identity;
    protected ZMQ.Poller poller;
    protected List<ZMQ.Socket> sockets;

    void init(ZContext context) {
        this.context = context;
    }

    public ZMQProcess(String identity) {
        this.identity = identity;
    }

    @Override
    public void run(){
        try{
            // check whether init was called
            if (context == null) {
                logger.fatal("ZMQProcess with identity {} started before init was called, shutting down", identity);
                System.exit(1);
            }

            // set thread name
            Thread.currentThread().setName(identity);
            // get other sockets (UDF)
            sockets = bindAndConnectSockets(context);
            // register them at poller
            poller = context.createPoller(sockets.size());
            sockets.forEach(s -> poller.register(s, ZMQ.Poller.POLLIN));

            // poll all sockets
            while (!Thread.currentThread().isInterrupted()) {
                logger.trace("Waiting {}s for a message", TIMEOUT_SECONDS);
                poller.poll(TIMEOUT_SECONDS * 1000);

                // poll each socket
                for (int socketIndex = 0; socketIndex < sockets.size(); socketIndex++) {
                    if (poller.pollin(socketIndex)) {
                        ZMsg msg = ZMsg.recvMsg(sockets.get(socketIndex));

                        // process the ZMsg (UDF)
                        processZMsg(socketIndex, msg);

                        // do not poll other sockets when we got a message, restart while loop
                        break;
                    }
                }
            }

            // destroy sockets as we are shutting down
            sockets.forEach(s -> context.destroySocket(s));

            // UDF for shutdown completed
            shutdownCompleted();

        }
        catch (Exception ex){
            logger.error(ex.getMessage());
        }
    }

    protected abstract List<ZMQ.Socket> bindAndConnectSockets(ZContext context);

    protected abstract void processZMsg(int socketIndex, ZMsg msg);

    protected abstract void shutdownCompleted();
}
