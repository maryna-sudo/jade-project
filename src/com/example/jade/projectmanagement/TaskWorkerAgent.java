package com.example.jade.projectmanagement;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.Map;

public class TaskWorkerAgent extends Agent {
    private long executionDelayMs = 2500L;

    @Override
    protected void setup() {
        Object[] args = getArguments();
        if (args != null && args.length > 0) {
            executionDelayMs = Long.parseLong(String.valueOf(args[0]));
        }

        registerTaskExecutionService();
        System.out.printf("[%s] Worker started with simulated execution delay %d ms%n",
                getLocalName(), executionDelayMs);
        addBehaviour(new AssignmentServer());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException e) {
            System.err.printf("[%s] Failed to deregister: %s%n", getLocalName(), e.getMessage());
        }
    }

    private void registerTaskExecutionService() {
        DFAgentDescription description = new DFAgentDescription();
        description.setName(getAID());

        ServiceDescription service = new ServiceDescription();
        service.setType(ProjectManagementProtocol.SERVICE_TYPE);
        service.setName(getLocalName() + "-task-worker");
        description.addServices(service);

        try {
            DFService.register(this, description);
        } catch (FIPAException e) {
            throw new IllegalStateException("Unable to register task worker", e);
        }
    }

    private void sendProgress(String projectId, String taskId, TaskStatus status, ACLMessage assignment) {
        ACLMessage update = assignment.createReply();
        update.setPerformative(ACLMessage.INFORM);
        update.setConversationId(ProjectManagementProtocol.TASK_PROGRESS);
        update.setContent(ProjectManagementProtocol.encodeProgress(projectId, taskId, status));
        send(update);
    }

    private final class AssignmentServer extends CyclicBehaviour {
        private final MessageTemplate template = MessageTemplate.MatchConversationId(ProjectManagementProtocol.TASK_ASSIGNMENT);

        @Override
        public void action() {
            ACLMessage assignment = myAgent.receive(template);
            if (assignment == null) {
                block();
                return;
            }

            Map<String, String> payload = ProjectManagementProtocol.decodeTask(assignment.getContent());
            String projectId = payload.get("projectId");
            String taskId = payload.get("taskId");
            String title = payload.get("title");

            ACLMessage reply = assignment.createReply();
            reply.setContent(assignment.getContent());
            reply.setPerformative(ACLMessage.AGREE);
            myAgent.send(reply);

            System.out.printf("[%s] Accepted %s/%s %s%n", getLocalName(), projectId, taskId, title);
            sendProgress(projectId, taskId, TaskStatus.IN_PROGRESS, assignment);

            addBehaviour(new WakerBehaviour(myAgent, executionDelayMs) {
                @Override
                protected void onWake() {
                    System.out.printf("[%s] Completed %s/%s %s%n", getLocalName(), projectId, taskId, title);
                    sendProgress(projectId, taskId, TaskStatus.DONE, assignment);
                }
            });
        }
    }
}
