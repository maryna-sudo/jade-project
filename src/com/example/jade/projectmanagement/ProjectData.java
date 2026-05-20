package com.example.jade.projectmanagement;

import java.util.ArrayList;
import java.util.List;

public final class ProjectData {
    private final String projectId;
    private final String name;
    private final List<TaskItem> tasks = new ArrayList<>();
    private boolean completionAnnounced;

    public ProjectData(String projectId, String name) {
        this.projectId = projectId;
        this.name = name;
    }

    public String projectId() {
        return projectId;
    }

    public String name() {
        return name;
    }

    public List<TaskItem> tasks() {
        return tasks;
    }

    public void addTask(TaskItem task) {
        tasks.add(task);
    }

    public boolean isCompleted() {
        return !tasks.isEmpty() && tasks.stream().allMatch(task -> task.status() == TaskStatus.DONE);
    }

    public boolean completionAnnounced() {
        return completionAnnounced;
    }

    public void setCompletionAnnounced(boolean completionAnnounced) {
        this.completionAnnounced = completionAnnounced;
    }
}
