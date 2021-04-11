from server.events import EventScheduler, event_handler
from server.router import Router, RouterEvents

class Agent(Router):
    def __init__(self):
        super().__init__()

class AgentEvents(RouterEvents):
    pass
