package io.roach.workload.common.cli;

import com.zaxxer.hikari.HikariPoolMXBean;

public class ConnectionPoolStats {
    public static ConnectionPoolStats from(HikariPoolMXBean mxBean) {
        ConnectionPoolStats ps = new ConnectionPoolStats();
        ps.name = mxBean.toString();
        ps.activeConnections = mxBean.getActiveConnections();
        ps.idleConnections = mxBean.getIdleConnections();
        ps.threadsAwaitingConnection = mxBean.getThreadsAwaitingConnection();
        ps.totalConnections = mxBean.getTotalConnections();
        return ps;
    }

    public String name;

    public int activeConnections;

    public int idleConnections;

    public int threadsAwaitingConnection;

    public int totalConnections;
}
