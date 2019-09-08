package plan;

import common.Plan;
import common.ServerLoadMetrics;
import common.TopicMetrics;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;

public class PlanAnalyzer {
    private static final Logger logger = LogManager.getLogger();
    private ZContext ctx; //  Own context
    public ZMQ.Socket pipe;
    PlanCreator planCreator;
    private HashSet<String> servers = new HashSet<>();
    private HashSet<String> updatingServers = new HashSet<>();

    protected PlanAnalyzer(ZContext ctx, ZMQ.Socket pipe)
    {
        planCreator = new PlanCreator();
        this.ctx = ctx;
        this.pipe = pipe;
        Thread.currentThread().setName("plan-analyzer");
    }

    public void receiveMessage(){

    }

    public void addMetrics(){
        ZMsg msg = ZMsg.recvMsg(pipe);
        ZFrame zframe = msg.getLast();
        ServerLoadMetrics slm = new ServerLoadMetrics("", 2D);
        planCreator.updateServerLoadMetrics(slm);

        updatingServers.add("");
        if (updatingServers.size() >= servers.size()){
            sendPlan(planCreator.getPlan());
        }

    }

    public void addServer(){
        ZMsg msg = ZMsg.recvMsg(pipe);
        ZFrame zframe = msg.getLast();
        servers.add(zframe.toString());
    }

    public void sendPlan(ArrayList<Plan> plans){
        ZMsg msgRequest = new ZMsg();
        msgRequest.push(plans.toString());
        msgRequest.send(pipe);
    }

}
