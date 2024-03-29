package org.motechproject.tasks.contract;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TriggerEventRequest {

    private String displayName;
    private String subject;
    private String triggerListenerSubject;
    private List<EventParameterRequest> eventParameters;
    private String description;

    private TriggerEventRequest() {
        eventParameters = new ArrayList<>();
    }

    public TriggerEventRequest(String displayName, String subject, String description, List<EventParameterRequest> eventParameters) {
        this.displayName = displayName;
        this.subject = subject;
        this.description = description;
        this.eventParameters = eventParameters;
        this.triggerListenerSubject = subject;
    }

    public TriggerEventRequest(String displayName, String subject, String description, List<EventParameterRequest> eventParameters, String triggerListenerSubject) {
        this.displayName = displayName;
        this.description = description;
        this.eventParameters = eventParameters;
        this.triggerListenerSubject = triggerListenerSubject;
        this.subject = subject;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getSubject() {
        return subject;
    }

    public List<EventParameterRequest> getEventParameters() {
        return eventParameters;
    }

    public String getDescription() {
        return description;
    }

    public String getTriggerListenerSubject() {
        return StringUtils.isEmpty(triggerListenerSubject) ? subject : triggerListenerSubject;
    }

    @Override
    public int hashCode() {
        return Objects.hash(eventParameters);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }


        final TriggerEventRequest other = (TriggerEventRequest) obj;

        return Objects.equals(this.eventParameters, other.eventParameters);
    }

    @Override
    public String toString() {
        return String.format("TriggerEvent{eventParameters=%s}", eventParameters);
    }
}
