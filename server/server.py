import time
from server.events import EventScheduler, ServerEvents, Priority, event_handler

class NetworkServer:
    def __init__(self):
        self.scheduler = EventScheduler()
        self.ongoing = False
        self.properties = {}
        self.__default_properties()
        self.__register_default_events()

    def __default_properties(self):
        self.properties = {
            "sleep_time": 1
        }

    def __register_default_events(self):
        @event_handler(priority=Priority.ULTRA_LOW)
        def sleep_esr(serv, cancelled):
            print("sleeping server \"" + str(serv) + "\"")
            time.sleep(self.properties["sleep_time"])

        @event_handler
        def exception_esr(serv, exception, cancelled):
            if isinstance(exception, (KeyboardInterrupt, SystemExit)):
                print("Gracefully stopping server")
                serv.ongoing = False
                return True

        @event_handler
        def shutdown_esr(serv, cancelled):
            print("Shutting down")

        self.scheduler.register_event(ServerEvents.TICK, sleep_esr)
        self.scheduler.register_event(ServerEvents.EXCEPTION, exception_esr)
        self.scheduler.register_event(ServerEvents.SHUTDOWN, shutdown_esr)

    def set_property(self, key, value):
        self.properties[key] = value

    def loop(self):
        self.ongoing = True

        try:
            while self.ongoing:
                try:
                    self.scheduler.throw_event(ServerEvents.TICK, self)
                except BaseException as e:
                    c = self.scheduler.throw_event(ServerEvents.EXCEPTION, self, e)
                    if not c:
                        raise
        finally:
            self.scheduler.throw_event(ServerEvents.SHUTDOWN, self)
