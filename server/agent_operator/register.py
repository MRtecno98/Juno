from .config import AgentDescriptor

class AgentRegister:
    def __init__(self, operator):
        self.operator = operator
        self.agents = set()

    def register_agent(self, agent: AgentDescriptor):
        cancelled = self.operator.router.scheduler \
            .throw_event(RegisterEvents.AGENT_REGISTERED, agent, register=self)

        if not cancelled:
            self.agents.add(agent)

        return cancelled

    def register_all(self, iterable):
        return tuple(agent for agent in iterable if not self.register_agent(agent))

    def get_agents(self):
        return set(self.agents)

    def get_agent(self, name):
        for agent in self.get_agents():
            if agent.name == name:
                return agent

    def is_registered(self, name):
        return len([agent for agent in self.get_agents() if agent.name == name]) != 0

class RegisterEvents:
    AGENT_REGISTERED = "ag_reg_register"
