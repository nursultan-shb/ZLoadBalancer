import org.zeromq.ZThread;

public class Runner {

    public static void main(String[] args){
        ZThread.start(new ZClient());
    }
}
