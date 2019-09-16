package ms.shabykeev.loadbalancer.common.server;



public interface IServerLogic {
    void loadConfiguration(Configuration configuration);

    void initializeFields();

    void startServer();

    void serverIsRunning();

    void cleanUp();

}