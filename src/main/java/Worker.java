import org.zeromq.*;

public class Worker implements ZThread.IDetachedRunnable {

    private final int id;

    public Worker (Integer id){
        this.id = id;
    }

    @Override
    public void run(Object[] args)
    {
        //  Prepare our context and sockets
        try (ZContext context = new ZContext()) {
            ZMQ.Socket worker = context.createSocket(SocketType.DEALER);
            worker.setIdentity(("Worker-" + this.id).getBytes());

            worker.connect("ipc://backend.ipc");

            //  Tell backend we're ready for work
            ZFrame frame = new ZFrame("Ready");
            frame.send(worker, 0);


            ZMQProcessManager processManager = new ZMQProcessManager();


            while (true) {
                ZMsg msg = ZMsg.recvMsg(worker);
                if (msg == null)
                    break;


            }
        }
    }
}
