package ms.shabykeev.loadbalancer.common.server;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMsg;

import java.util.Objects;
import java.util.Optional;

import de.hasenburg.geobroker.commons.model.KryoSerializer;
import de.hasenburg.geobroker.commons.model.message.ControlPacketType;
import de.hasenburg.geobroker.commons.model.message.payloads.AbstractPayload;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zeromq.ZMsg;

import java.util.Objects;
import java.util.Optional;

/**
 * Author: Jonathan Hasenburg
 * Source: https://github.com/MoeweX/geobroker
 * */

public class InternalServerMessage {

    private static final Logger logger = LogManager.getLogger();

    private String clientIdentifier;
    private ControlPacketType controlPacketType;
    private AbstractPayload payload;

    /**
     * TODO get rid of Optional
     * Optional is empty when
     * 	- ZMsg is not a InternalClientMessage or null
     * 	- Payload incompatible to control packet type
     * 	- Payload misses fields
     */
    public static Optional<InternalServerMessage> buildMessage(ZMsg msg, KryoSerializer kryo) {
        if (msg == null) {
            // happens when queue is empty
            return Optional.empty();
        }

        if (msg.size() != 3) {
            logger.error("Cannot parse message {} to InternalServerMessage, has wrong size.", msg.toString());
            return Optional.empty();
        }

        InternalServerMessage message = new InternalServerMessage();

        try {
            message.clientIdentifier = msg.popString();
            message.controlPacketType = ControlPacketType.valueOf(msg.popString());
            byte[] arr = msg.pop().getData();
            message.payload = kryo.read(arr, message.controlPacketType);
            if(message.payload == null){
                message = null;
            }
        } catch (Exception e) {
            logger.warn("Cannot parse message, due to exception, discarding it", e);
            message = null;
        }

        return Optional.ofNullable(message);
    }

    private InternalServerMessage() {

    }

    public InternalServerMessage(String clientIdentifier,
                                 ControlPacketType controlPacketType, AbstractPayload payload) {
        this.clientIdentifier = clientIdentifier;
        this.controlPacketType = controlPacketType;
        this.payload = payload;
    }

    public ZMsg getZMsg(KryoSerializer kryo) {
        byte[] arr = kryo.write(payload);
        return ZMsg.newStringMsg(clientIdentifier, controlPacketType.name()).addLast(arr);
    }

    /*****************************************************************
     * Generated Code
     ****************************************************************/

    public String getClientIdentifier() {
        return clientIdentifier;
    }

    public ControlPacketType getControlPacketType() {
        return controlPacketType;
    }

    public AbstractPayload getPayload() {
        return payload;
    }

    @Override
    public String toString() {
        return "InternalServerMessage{" +
                "clientIdentifier='" + clientIdentifier + '\'' +
                ", controlPacketType=" + controlPacketType +
                ", payload=" + payload +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InternalServerMessage)) {
            return false;
        }
        InternalServerMessage that = (InternalServerMessage) o;
        return Objects.equals(getClientIdentifier(), that.getClientIdentifier()) &&
                getControlPacketType() == that.getControlPacketType() &&
                Objects.equals(getPayload(), that.getPayload());
    }

    @Override
    public int hashCode() {

        return Objects.hash(getClientIdentifier(), getControlPacketType(), getPayload());
    }
}
