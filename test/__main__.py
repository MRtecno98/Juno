import sys, os

sys.path.append(os.path.abspath(os.path.join(os.path.split(__file__)[0], "..")))

import server
from server.agent_operator.register import RegisterEvents

class DebugServer(server.LogisticalRouter):
    def __init__(self):
        super().__init__(os.path.join(os.path.split(__file__)[0], "agents.yml"))

        def registered_esr(agent, cancelled, register):
            print(f"Registering {agent}")

        self.scheduler.register_event(RegisterEvents.AGENT_REGISTERED, registered_esr)


if __name__ == "__main__":
    n = DebugServer()
    n.loop()
