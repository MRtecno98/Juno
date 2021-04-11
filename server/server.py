from .agent_operator import AgentOperator
from .router import Router

class LogisticalRouter(Router):
    def __init__(self, agents_configuration):
        super().__init__()
        self.operator = AgentOperator(self, "localhost", 7777, agents_configuration)
