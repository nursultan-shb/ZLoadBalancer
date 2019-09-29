package ms.shabykeev.loadbalancer.plan.generator;

import ms.shabykeev.loadbalancer.common.Plan;
import ms.shabykeev.loadbalancer.common.ServerLoadMetrics;
import ms.shabykeev.loadbalancer.common.TopicMetrics;
import ms.shabykeev.loadbalancer.common.ZMsgType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import javax.xml.crypto.dsig.keyinfo.KeyValue;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.averagingDouble;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.reducing;

public class Generator extends Thread {

    private static final Logger logger = LogManager.getLogger();

    private ZContext ctx;
    public ZMQ.Socket pairSocket;

    private Integer planNumber = 0;
    private Long lastGenerationTime = 0L;
    private static final Double SERVER_LOAD_THRESHOLD = 5D;

    private ArrayList<TopicMetrics> topicMetrics = new ArrayList<>();
    private ArrayList<ServerLoadMetrics> serverLoadMetrics = new ArrayList<>();
    private HashMap<String, String> planMap = new HashMap<>();

    public Generator(ZContext context, String socketAddress) {
        this.ctx = context;
        Thread.currentThread().setName("plan-generator");

        pairSocket = ctx.createSocket(SocketType.PAIR);
        pairSocket.bind(socketAddress);
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            String msg = pairSocket.recvStr();
            logger.info("Generator starts the job");
            managePlan(msg);
        }
    }

    public void managePlan(String metrics) {

        if (metrics.length() > 3) {

            parseData(metrics);
            ArrayList<Plan> newPlans = createPlan();
            boolean isDifferent = mergePlan(newPlans);

            if (isDifferent) {
                logger.info("sending plan " + planNumber);
                sendPlan();
                planNumber++;
            }
        }
    }

    public void sendPlan() {
        ZMsg msg = new ZMsg();
        msg.add(ZMsgType.PLAN.toString());
        msg.add(convertPlanToString());
        msg.send(pairSocket);
    }

    private void parseData(String metrics) {
        if (metrics.length() <= 3) return;

        clearData();

        ArrayList<ServerLoadMetrics> lmList = new ArrayList<>();
        ArrayList<TopicMetrics> tmList = new ArrayList<>();

        metrics = deleteEdgeSymbols(metrics.trim());
        String[] strValues = metrics.split("],");

        for (String strValue : strValues) {
            String value = replaceSpecialCharacters(strValue);
            String[] elements = value.split(",");

            if (value.contains(ZMsgType.TOPIC_METRICS.toString())) {
                String server = elements[2].trim();
                ServerLoadMetrics slm = new ServerLoadMetrics(server, elements[0], Double.valueOf(elements[3]));
                lmList.add(slm);

                if (elements.length > 4) { //a message contains topic metrics
                    ArrayList tm = parseTopicMetrics(elements[4].trim(), server);
                    tmList.addAll(tm);
                }
            }
        }

        Map<String, Optional<ServerLoadMetrics>> lmMap = lmList.stream()
                .collect(groupingBy(ServerLoadMetrics::getServer,
                        Collectors.maxBy(Comparator.comparing(ServerLoadMetrics::getLoad))));

        for (Map.Entry element : lmMap.entrySet()) {
            Object obj = element.getValue();
            if (obj != null) {
                serverLoadMetrics.add(((Optional<ServerLoadMetrics>) obj).get());
            }
        }

        Map<String, Map<String, Optional<TopicMetrics>>> tmMap = tmList.stream()
                .collect(groupingBy(TopicMetrics::getServer, groupingBy(TopicMetrics::getTopic,
                        Collectors.maxBy(Comparator.comparing(TopicMetrics::getMessagesCount)))));

        for (Map.Entry element : tmMap.entrySet()) {
            Object obj = element.getValue();
            if (obj != null) {
                Map<String, Optional<TopicMetrics>> nestedMap = ((Map) (obj));
                for (Map.Entry nestedElement : nestedMap.entrySet()) {
                    Object nestedObject = nestedElement.getValue();
                    if (nestedObject != null) {
                        topicMetrics.add(((Optional<TopicMetrics>) nestedObject).get());
                    }
                }
            }
        }
    }

    private boolean mergePlan(ArrayList<Plan> plans){
        boolean isDifferent = false;

        for(Plan plan: plans){
            if (planMap.containsKey(plan.getTopic())){
                String server = planMap.get(plan.getTopic());
                if (!server.equals(plan.getServer())){
                    isDifferent = true;
                }
            }
            else {
                isDifferent = true;
            }

            planMap.put(plan.getTopic(), plan.getServer());
        }

        return isDifferent;
    }

    private ArrayList<Plan> createPlan() {
        ServerLoadMetrics leastLm = getLeastLoadedServer();

        for (ServerLoadMetrics slm : serverLoadMetrics) {
            if (slm.getLoad() >= SERVER_LOAD_THRESHOLD && topicMetrics.size() > 0) {
                TopicMetrics tm = getMostLoadedTopic(slm.getServer());
                if (tm != null) {
                    tm.setServer(leastLm.getServer());
                }
            }
        }

        ArrayList<Plan> newPlans = new ArrayList<>();
        topicMetrics.forEach(s -> newPlans.add(new Plan(s.getTopic(), s.getServer())));
        return newPlans;
    }

    /*
    private boolean isDifferent(ArrayList<Plan> newPlans) {
        if (newPlans.size() != plans.size()) {
            return true;
        }

        //search for a topic/server mapping in the old plan
        for (Plan newPlan : newPlans) {
            Optional<Plan> element = plans.stream().filter(
                    s -> s.getServer().equals(newPlan.getServer()) && s.getTopic().equals(newPlan.getTopic())).findAny();
            if (!element.isPresent()) {
                return true;
            }
        }

        return false;
    }
    */

    private ServerLoadMetrics getLeastLoadedServer() {
        ServerLoadMetrics slm = serverLoadMetrics.stream()
                .min(Comparator.comparing(ServerLoadMetrics::getLoad)).get();

        return slm;
    }

    private TopicMetrics getMostLoadedTopic(String server) {
        TopicMetrics tm = topicMetrics.stream()
                .filter(s -> s.getServer().equals(server))
                .max(Comparator.comparing(TopicMetrics::getMessagesCount)).orElse(null);

        return tm;
    }

    private ArrayList<TopicMetrics> parseTopicMetrics(String value, String server) {
        ArrayList<TopicMetrics> tmList = new ArrayList<>();

        if (value.trim().length() < 3) {
            return tmList;
        }

        String[] keyValuePairs = value.split("\\|");

        for (String pair : keyValuePairs) {
            String[] entry = pair.split("=");
            TopicMetrics tm = new TopicMetrics();
            tm.setServer(server);
            tm.setTopic(entry[0].trim());
            tm.setMessagesCount(Integer.valueOf(entry[1].trim()));
            tmList.add(tm);
        }

        return tmList;
    }

    private String deleteEdgeSymbols(String str) {
        return str.substring(1, str.length() - 1).trim();
    }

    private String replaceSpecialCharacters(String str) {
        return str.replace("[", "").replace("]", "").trim();
    }

    private void clearData(){
        this.topicMetrics.clear();
        this.serverLoadMetrics.clear();
    }

    private String convertPlanToString(){
        /*
        String listString = plans.stream().map(Plan::toString)
                .collect(Collectors.joining(", "));
        */

        StringBuilder sb = new StringBuilder();
        planMap.forEach((k, v) -> sb.append(String.format(k + "=" + v + "|")));
        String result = sb.length() > 0 ? sb.substring(0, sb.length()-1).trim() : "";

        return result;
    }

}
