package ms.shabykeev.loadbalancer.plan;

import ms.shabykeev.loadbalancer.common.ServerLoadMetrics;
import ms.shabykeev.loadbalancer.common.TopicMetrics;
import ms.shabykeev.loadbalancer.common.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.*;

public class MetricsQueue_old {
    private static final Logger logger = LogManager.getLogger();
    private ZContext ctx; //  Own context
    public ZMQ.Socket pipe;

    private Set<ServerLoadMetrics> loadMetrics = new HashSet<>();
    private ArrayList<TopicMetrics> topicMetrics = new ArrayList<>();

    /**
     * Map of a pair <LLA, GeoBroker>
     * */
    private HashMap<String, String> servers = new HashMap<>();

    protected MetricsQueue_old(ZContext ctx, ZMQ.Socket pipe)
    {
        this.ctx = ctx;
        this.pipe = pipe;
        Thread.currentThread().setName("plan-analyzer");
    }

    public void receiveMessage(){
        ZMsg msg = ZMsg.recvMsg(pipe);
        String sender = msg.popString();
        String msgType = msg.popString();

        if (msgType.equals(ZMsgType.ADD_SERVER.toString())){
            addServer(sender, msg);
        } else if (msgType.equals(ZMsgType.TOPIC_METRICS.toString())){
            addMetrics(msg);
        }
    }

    public void addMetrics(ZMsg msg){
        String server = msg.popString();
        String load = msg.popString();
        ServerLoadMetrics slm = new ServerLoadMetrics(server, Double.valueOf(load));
        //planCreator.updateServerLoadMetrics(slm);

        String topicMetricStr = msg.popString();
        if (topicMetricStr.length() > 2){
            ArrayList topicMetrics = getTopicMetrics(topicMetricStr, server);
            //planCreator.updateTopicMetrics(topicMetrics);
        }
    }

    public void addServer(String sender, ZMsg msg){
        ZFrame zframe = msg.getLast();
        servers.put(sender, zframe.toString());
    }

    public void updateServerLoadMetrics(ServerLoadMetrics metrics) {
        loadMetrics.add(metrics);
    }

    public void updateTopicMetrics(ArrayList<TopicMetrics> topicMetrics){
        topicMetrics.addAll(topicMetrics);
    }

    private ArrayList getTopicMetrics(String topicMetricStr, String server){
        String value = topicMetricStr.substring(1, topicMetricStr.length()-1);
        String[] keyValuePairs = value.split(",");
        ArrayList<TopicMetrics> topicMetrics = new ArrayList<>();

        for(String pair : keyValuePairs)
        {
            String[] entry = pair.split("=");
            //split the pairs to get key and value
            TopicMetrics tm = new TopicMetrics();
            tm.setServer(server);
            tm.setTopic(entry[0].trim());
            tm.setMessagesCount(Integer.valueOf(entry[1].trim()));
        }

        return topicMetrics;
    }

}
