package com.example.jade.projectmanagement;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.SearchConstraints;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ProjectCoordinatorAgent extends Agent {
    private final Map<String, ProjectData> projects = new LinkedHashMap<>();
    private int workerCursor;

    @Override
    protected void setup() {
        seedProjects();
        System.out.printf("[%s] Coordinator started with %d projects and %d tasks%n",
                getLocalName(), projects.size(), taskCount());

        addBehaviour(new TaskUpdatesServer());
        addBehaviour(new TickerBehaviour(this, 2000) {
            @Override
            protected void onTick() {
                dispatchPendingTasks();
                announceCompletedProjects();
                if (allProjectsCompleted()) {
                    notifyMonitor("ALL_PROJECTS_DONE", "All demo projects have been completed.");
                    System.out.printf("[%s] All projects completed%n", getLocalName());
                    stop();
                }
            }
        });
    }

    private void seedProjects() {
        ProjectData website = new ProjectData("PRJ-1", "Website Redesign");
        website.addTask(new TaskItem("TASK-1", website.projectId(), "Plan backlog", "Prepare project backlog and priorities."));
        website.addTask(new TaskItem("TASK-2", website.projectId(), "Create wireframes", "Draft the first page wireframes."));
        website.addTask(new TaskItem("TASK-3", website.projectId(), "Review landing page", "Validate the landing page task flow."));

        ProjectData mobile = new ProjectData("PRJ-2", "Mobile Release");
        mobile.addTask(new TaskItem("TASK-4", mobile.projectId(), "Define sprint scope", "Break release work into executable tasks."));
        mobile.addTask(new TaskItem("TASK-5", mobile.projectId(), "Implement notifications", "Prepare the notification delivery task."));
        mobile.addTask(new TaskItem("TASK-6", mobile.projectId(), "Run acceptance review", "Close the project with acceptance checks."));

        projects.put(website.projectId(), website);
        projects.put(mobile.projectId(), mobile);

        for (ProjectData project : projects.values()) {
            notifyMonitor("PROJECT_CREATED", project.projectId() + " " + project.name());
            for (TaskItem task : project.tasks()) {
                notifyMonitor("TASK_CREATED", describeTask(task));
            }
        }
    }

    private void dispatchPendingTasks() {
        List<AID> workers = discoverWorkers();
        if (workers.isEmpty()) {
            System.out.printf("[%s] No task workers available in DF%n", getLocalName());
            return;
        }

        List<TaskItem> pending = projects.values().stream()
                .flatMap(project -> project.tasks().stream())
                .filter(task -> task.status() == TaskStatus.TODO && task.assignedWorker() == null)
                .sorted(Comparator.comparing(TaskItem::taskId))
                .toList();

        for (TaskItem task : pending) {
            AID worker = workers.get(workerCursor % workers.size());
            workerCursor++;
            task.setAssignedWorker(worker.getLocalName());

            ACLMessage assignment = new ACLMessage(ACLMessage.REQUEST);
            assignment.addReceiver(worker);
            assignment.setConversationId(ProjectManagementProtocol.TASK_ASSIGNMENT);
            assignment.setContent(ProjectManagementProtocol.encodeTask(task));
            send(assignment);

            System.out.printf("[%s] Assigned %s to %s%n", getLocalName(), describeTask(task), worker.getLocalName());
            notifyMonitor("TASK_ASSIGNED", describeTask(task) + " -> " + worker.getLocalName());
        }
    }

    private List<AID> discoverWorkers() {
        DFAgentDescription template = new DFAgentDescription();
        ServiceDescription service = new ServiceDescription();
        service.setType(ProjectManagementProtocol.SERVICE_TYPE);
        template.addServices(service);

        SearchConstraints constraints = new SearchConstraints();
        constraints.setMaxResults(20L);

        try {
            DFAgentDescription[] results = DFService.search(this, template, constraints);
            List<AID> workers = new ArrayList<>(results.length);
            for (DFAgentDescription result : results) {
                workers.add(result.getName());
            }
            workers.sort(Comparator.comparing(AID::getLocalName));
            return workers;
        } catch (FIPAException e) {
            throw new IllegalStateException("Failed to discover task workers", e);
        }
    }

    private void announceCompletedProjects() {
        for (ProjectData project : projects.values()) {
            if (project.isCompleted() && !project.completionAnnounced()) {
                project.setCompletionAnnounced(true);
                String summary = project.projectId() + " " + project.name() + " completed";
                System.out.printf("[%s] %s%n", getLocalName(), summary);
                notifyMonitor("PROJECT_COMPLETED", summary);
            }
        }
    }

    private boolean allProjectsCompleted() {
        return projects.values().stream().allMatch(ProjectData::isCompleted);
    }

    private int taskCount() {
        return projects.values().stream().mapToInt(project -> project.tasks().size()).sum();
    }

    private TaskItem findTask(String projectId, String taskId) {
        ProjectData project = projects.get(projectId);
        if (project == null) {
            return null;
        }

        return project.tasks().stream()
                .filter(task -> task.taskId().equals(taskId))
                .findFirst()
                .orElse(null);
    }

    private void notifyMonitor(String eventType, String details) {
        ACLMessage event = new ACLMessage(ACLMessage.INFORM);
        event.addReceiver(new AID("monitor", AID.ISLOCALNAME));
        event.setConversationId(ProjectManagementProtocol.PROJECT_EVENTS);
        event.setContent(ProjectManagementProtocol.formatEvent(eventType, details));
        send(event);
    }

    private String describeTask(TaskItem task) {
        return task.projectId() + "/" + task.taskId() + " " + task.title();
    }

    private final class TaskUpdatesServer extends CyclicBehaviour {
        private final MessageTemplate template = MessageTemplate.or(
                MessageTemplate.MatchConversationId(ProjectManagementProtocol.TASK_ASSIGNMENT),
                MessageTemplate.MatchConversationId(ProjectManagementProtocol.TASK_PROGRESS)
        );

        @Override
        public void action() {
            ACLMessage message = myAgent.receive(template);
            if (message == null) {
                block();
                return;
            }

            if (message.getConversationId().equals(ProjectManagementProtocol.TASK_ASSIGNMENT)) {
                handleAssignmentReply(message);
                return;
            }

            handleProgressUpdate(message);
        }

        private void handleAssignmentReply(ACLMessage message) {
            Map<String, String> payload = ProjectManagementProtocol.decodeTask(message.getContent());
            TaskItem task = findTask(payload.get("projectId"), payload.get("taskId"));
            if (task == null) {
                return;
            }

            if (message.getPerformative() == ACLMessage.REFUSE) {
                System.out.printf("[%s] Worker %s refused %s%n",
                        getLocalName(), message.getSender().getLocalName(), describeTask(task));
                task.setAssignedWorker(null);
                notifyMonitor("TASK_REFUSED", describeTask(task) + " by " + message.getSender().getLocalName());
            }
        }

        private void handleProgressUpdate(ACLMessage message) {
            Map<String, String> payload = ProjectManagementProtocol.decodeProgress(message.getContent());
            TaskItem task = findTask(payload.get("projectId"), payload.get("taskId"));
            if (task == null) {
                return;
            }

            TaskStatus status = TaskStatus.valueOf(payload.get("status"));
            task.setStatus(status);
            task.setAssignedWorker(message.getSender().getLocalName());

            System.out.printf("[%s] %s moved to %s by %s%n",
                    getLocalName(), describeTask(task), status, message.getSender().getLocalName());
            notifyMonitor("TASK_STATUS", describeTask(task) + " -> " + status + " by " + message.getSender().getLocalName());
        }
    }
}
