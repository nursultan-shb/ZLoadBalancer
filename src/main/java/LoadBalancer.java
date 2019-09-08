
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoadBalancer {

    private static final Logger logger = LogManager.getLogger();

    public LoadBalancer(){

    }

    public static void main (String[] args){
        IServerLogic logic = new LoadBalancerLogic();

        ServerLifeCycle lifecycle = new ServerLifeCycle(logic);
        lifecycle.run();
    }


}
