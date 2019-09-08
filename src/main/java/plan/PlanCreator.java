package plan;

import common.Plan;
import common.ServerLoadMetrics;
import common.TopicMetrics;

import java.util.*;

public class PlanCreator {
    private Set<ServerLoadMetrics> loadMetrics = new HashSet<>();
    private ArrayList<TopicMetrics> topicMetrics = new ArrayList<>();

    private ArrayList<Plan> plans = new ArrayList<>();
    private static final Double SERVER_LOAD_THRESHOLD = 60D;
    private Long lastPlanUpdate = 0L;

    public void updateServerLoadMetrics(ServerLoadMetrics metrics) {
        loadMetrics.add(metrics);
    }

    public ArrayList<Plan> getPlan(){
        if (lastPlanUpdate == 0L){
            createInitialPlan();
        }
        else{
            constructPlan();
        }
        return plans;
    }


    public void createInitialPlan() {
        plans.add(new Plan("red", "tcp://0.0.0.0:5559"));
        plans.add(new Plan("blue", "tcp://0.0.0.0:5559"));
    }

    public void constructPlan() {
        ServerLoadMetrics leastLm = getLeastLoadedServer();

        for (ServerLoadMetrics slm : loadMetrics) {
            if (slm.getLoad() > SERVER_LOAD_THRESHOLD) {
                TopicMetrics tm = getMostLoadedTopic(slm.getServer());
                tm.setServer(leastLm.getServer());
            }

        }

        plans.clear();
        topicMetrics.forEach(s -> plans.add(new Plan(s.getTopic(), s.getServer())));
    }

    private ServerLoadMetrics getLeastLoadedServer() {
        ServerLoadMetrics slm = loadMetrics.stream()
                .min(Comparator.comparing(ServerLoadMetrics::getLoad))
                .orElse(new ServerLoadMetrics("", 2D));

        return slm;
    }

    private TopicMetrics getMostLoadedTopic(String server) {
        TopicMetrics tm = topicMetrics.stream()
                .filter(s -> s.getServer().equals(server))
                .max(Comparator.comparing(TopicMetrics::getMessagesCount))
                .orElse(new TopicMetrics());

        return tm;
    }

    public HashMap<String, String> getPlanByConsistedHashing() {

        //topic/server
        HashMap<String, String> plan = new HashMap<>();

        //fill in plan
        //plan.put("")

        return plan;

    }
}
