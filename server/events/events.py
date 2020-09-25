from server.misc import mark_object, is_marked
import enum

EVENT_MARKER = "juno_esr"

class EventScheduler:
    def __init__(self):
        self.routines = dict()

    def register_event(self, event_id, esr):
        self.__get_esr_list(event_id).append(esr)
        self.__sort_esr(event_id)

    def throw_event(self, event_id, *args, **kwargs):
        for esr in self.__get_esr_list(event_id):
            esr(*args, **kwargs)

    def __get_esr_list(self, event_id):
        if event_id not in self.routines.keys():
            self.routines[event_id] = list()

        return self.routines[event_id]

    def __set_esr_list(self, event_id, esr_list):
        self.routines[event_id] = esr_list

    def __sort_esr(self, event_id):
        def get_priority(esr):
            if is_marked(esr, EVENT_MARKER):
                return esr.properties["priority"]
            else:
                return 0

        self.__set_esr_list(event_id,
                            sorted(self.__get_esr_list(event_id),
                                   key=get_priority, reverse=True))

class Priority(enum.IntEnum):
    ULTRA_HIGH = 10
    HIGH = 5
    NORMAL = 0
    LOW = -5
    ULTRA_LOW = -10

def event_handler(priority):
    def event_decorator(handler):
        mark_object(handler, EVENT_MARKER)
        if not (hasattr(handler, "properties") and handler.properties):
            handler.properties = dict()

        handler.properties["priority"] = priority

        return handler
    return event_decorator
