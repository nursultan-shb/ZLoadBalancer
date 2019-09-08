package common;

public class TopicMetrics {
    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getPublisher() {
        return publisher;
    }

    public void setPublisher(String publisher) {
        this.publisher = publisher;
    }

    public String getMessagesCount() {
        return messagesCount;
    }

    public void setMessagesCount(String messagesCount) {
        this.messagesCount = messagesCount;
    }

    private String server;
    private String topic;
    private String publisher;
    private String messagesCount;
}
