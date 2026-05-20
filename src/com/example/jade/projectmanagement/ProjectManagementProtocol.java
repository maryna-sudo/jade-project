package com.example.jade.projectmanagement;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ProjectManagementProtocol {
    public static final String SERVICE_TYPE = "task-execution";
    public static final String TASK_ASSIGNMENT = "pm-task-assignment";
    public static final String TASK_PROGRESS = "pm-task-progress";
    public static final String PROJECT_EVENTS = "pm-project-events";
    public static final String FIELD_SEPARATOR = "\\|";

    private ProjectManagementProtocol() {
    }

    public static String encodeTask(TaskItem task) {
        return String.join("|",
                task.projectId(),
                task.taskId(),
                escape(task.title()),
                escape(task.description()));
    }

    public static Map<String, String> decodeTask(String content) {
        String[] parts = content.split(FIELD_SEPARATOR, 4);
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid task payload: " + content);
        }

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("projectId", parts[0]);
        fields.put("taskId", parts[1]);
        fields.put("title", unescape(parts[2]));
        fields.put("description", unescape(parts[3]));
        return fields;
    }

    public static String encodeProgress(String projectId, String taskId, TaskStatus status) {
        return String.join("|", projectId, taskId, status.name());
    }

    public static Map<String, String> decodeProgress(String content) {
        String[] parts = content.split(FIELD_SEPARATOR, 3);
        if (parts.length != 3) {
            throw new IllegalArgumentException("Invalid progress payload: " + content);
        }

        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("projectId", parts[0]);
        fields.put("taskId", parts[1]);
        fields.put("status", parts[2]);
        return fields;
    }

    public static String formatEvent(String eventType, String details) {
        return eventType + "|" + escape(details);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("|", "\\|");
    }

    private static String unescape(String value) {
        StringBuilder result = new StringBuilder();
        boolean escaped = false;
        for (char ch : value.toCharArray()) {
            if (escaped) {
                result.append(ch);
                escaped = false;
            } else if (ch == '\\') {
                escaped = true;
            } else {
                result.append(ch);
            }
        }
        return result.toString();
    }
}
