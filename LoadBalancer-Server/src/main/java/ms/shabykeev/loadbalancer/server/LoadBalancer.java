package ms.shabykeev.loadbalancer.server;

import ms.shabykeev.loadbalancer.common.server.Configuration;
import ms.shabykeev.loadbalancer.common.server.IServerLogic;
import ms.shabykeev.loadbalancer.common.server.ServerLifeCycle;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class LoadBalancer {

    private static final Logger logger = LogManager.getLogger();

    public LoadBalancer(){

    }

    public static void main (String[] args){

        IServerLogic logic = new LoadBalancerLogic();
        Configuration config = Configuration.readConfiguration("configuration.toml");

        ServerLifeCycle lifecycle = new ServerLifeCycle(logic);
        lifecycle.run(config);
    }
}
