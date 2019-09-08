public class ServerLifeCycle {

    private final IServerLogic serverLogic;

    public ServerLifeCycle(IServerLogic serverLogic) {
        this.serverLogic = serverLogic;
    }

    public void run() {

        serverLogic.initializeFields();

        serverLogic.startServer();

        serverLogic.serverIsRunning();

        serverLogic.cleanUp();
    }

}
