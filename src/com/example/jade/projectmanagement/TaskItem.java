package com.example.jade.projectmanagement;

public final class TaskItem {
    private final String taskId;
    private final String projectId;
    private final String title;
    private final String description;
    private TaskStatus status = TaskStatus.TODO;
    private String assignedWorker;

    public TaskItem(String taskId, String projectId, String title, String description) {
        this.taskId = taskId;
        this.projectId = projectId;
        this.title = title;
        this.description = description;
    }

    public String taskId() {
        return taskId;
    }

    public String projectId() {
        return projectId;
    }

    public String title() {
        return title;
    }

    public String description() {
        return description;
    }

    public TaskStatus status() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String assignedWorker() {
        return assignedWorker;
    }

    public void setAssignedWorker(String assignedWorker) {
        this.assignedWorker = assignedWorker;
    }
}
