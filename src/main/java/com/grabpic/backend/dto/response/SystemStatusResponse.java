package com.grabpic.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SystemStatusResponse {

    private String application;
    private String status;
    private String version;
    private String javaVersion;
    private String springBootVersion;
    private String uptime;
    private DatabaseInfo database;
    private SystemInfo system;
    private DiskInfo disk;
    private String time;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DatabaseInfo {
        private String status;
        private String name;
        private String version;
        private String host;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemInfo {
        private int cpuCores;
        private String freeMemory;
        private String totalMemory;
        private String maxMemory;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiskInfo {
        private String totalSpace;
        private String freeSpace;
    }
}
