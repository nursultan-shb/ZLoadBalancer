package ms.shabykeev.loadbalancer.common;

import org.zeromq.ZFrame;

public class ServerLoadMetrics {

    public ServerLoadMetrics(String server, Double load){
        this.server = server;
        this.load = load;
    }


    public String getServer() {
        return server;
    }


    public void setServer(String server) {
        this.server = server;
    }

    public Double getLoad() {
        return load;
    }

    public void setLoad(Double load) {
        this.load = load;
    }

    /**
     * A pub/sub server, i.e., GeoBroker
     * */
    private String server;
    private Double load;
}
