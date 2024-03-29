package org.motechproject.event.queue;

import org.motechproject.event.MotechEvent;

/**
 * Sends <code>MotechEvent</code> to the ActiveMQ broker, the implementation is generated by Spring Integration.
 */
public interface OutboundEventGateway {

    /**
     *  Sends the motechEvent's message as a payload to the message channel
     *  defined in the Spring Integration configuration file.
     *
     * @param motechEvent the event to be sent
     */
    void sendEventMessage(MotechEvent motechEvent);
}
