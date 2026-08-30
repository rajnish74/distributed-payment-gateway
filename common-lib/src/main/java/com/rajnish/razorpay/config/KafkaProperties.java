package com.rajnish.razorpay.config;


import com.rajnish.razorpay.enums.EventAggregateType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
@ConfigurationProperties(prefix = "app.kafka")
@Getter
@Setter
public class KafkaProperties {

    private Map<String, String> topics = new HashMap<>();

    public String topicFor(EventAggregateType aggregateType) {
        String topic = topics.get(aggregateType.name().toLowerCase());
        if (topic == null) {
            throw new IllegalStateException("Topic not found for aggregate type: " + aggregateType.name());
        }
        return topic;
    }

}
