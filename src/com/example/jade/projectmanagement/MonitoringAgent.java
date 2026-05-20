package com.example.jade.projectmanagement;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class MonitoringAgent extends Agent {
    @Override
    protected void setup() {
        System.out.printf("[%s] Monitoring agent started%n", getLocalName());
        addBehaviour(new EventLogServer());
    }

    private final class EventLogServer extends CyclicBehaviour {
        private final MessageTemplate template = MessageTemplate.MatchConversationId(ProjectManagementProtocol.PROJECT_EVENTS);

        @Override
        public void action() {
            ACLMessage event = myAgent.receive(template);
            if (event == null) {
                block();
                return;
            }

            System.out.printf("[%s] %s%n", getLocalName(), event.getContent());
        }
    }
}
