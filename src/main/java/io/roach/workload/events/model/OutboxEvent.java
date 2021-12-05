package io.roach.workload.events.model;

import java.time.LocalDateTime;
import java.util.UUID;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;

import io.roach.workload.common.jpa.AbstractEntity;

@Entity
public class OutboxEvent extends AbstractEntity<UUID> {
    @Id
    @Column(name = "id", updatable = false)
    private UUID id;

    @Column(nullable = false, updatable = false, name = "create_time")
    @Basic(fetch = FetchType.LAZY)
    private LocalDateTime createTime;

    @Column(name = "aggregate_type")
    private String aggregateType;

    @Column(name = "aggregate_id")
    private String aggregateId;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "payload")
    @Basic(fetch = FetchType.LAZY)
    private String payload;

    @Override
    public UUID getId() {
        return id;
    }

    public OutboxEvent setId(UUID id) {
        this.id = id;
        return this;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public OutboxEvent setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
        return this;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public OutboxEvent setAggregateType(String aggregateType) {
        this.aggregateType = aggregateType;
        return this;
    }

    public String getAggregateId() {
        return aggregateId;
    }

    public OutboxEvent setAggregateId(String aggregateIde) {
        this.aggregateId = aggregateIde;
        return this;
    }

    public String getEventType() {
        return eventType;
    }

    public OutboxEvent setEventType(String eventType) {
        this.eventType = eventType;
        return this;
    }

    public String getPayload() {
        return payload;
    }

    public OutboxEvent setPayload(String payload) {
        this.payload = payload;
        return this;
    }
}
