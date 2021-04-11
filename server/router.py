import time
from server.events import EventScheduler, Priority, event_handler

class Router:
    def __init__(self):
        self.scheduler = EventScheduler()
        self.ongoing = False
        self.__cycled = 0
        self.properties = {}
        self.__default_properties()
        self.__register_default_events()

    def __default_properties(self):
        self.properties = {
            "sleep_time": 1
        }

    def get_cycles(self):
        return self.__cycled

    def set_property(self, key, value):
        self.properties[key] = value

    def __register_default_events(self):
        @event_handler(priority=Priority.ULTRA_LOW)
        def sleep_esr(serv, cancelled):
            if (serv.get_cycles() > 0) and (serv.get_cycles() % 50) == 0:
                print(f"Heartbeat: Router ran for {serv.get_cycles()} cycles")
            time.sleep(self.properties["sleep_time"])
            self.__cycled += 1

        @event_handler
        def exception_esr(serv, exception, cancelled):
            if isinstance(exception, (KeyboardInterrupt, SystemExit)):
                print("Gracefully stopping router")
                serv.ongoing = False
                return True

        @event_handler
        def shutdown_esr(serv, cancelled):
            print("Shutting down")

        @event_handler(Priority.ULTRA_LOW)
        def startup_esr(serv, cancelled):
            print("Router started successfully")

        self.scheduler.register_event(RouterEvents.TICK, sleep_esr)
        self.scheduler.register_event(RouterEvents.EXCEPTION, exception_esr)
        self.scheduler.register_event(RouterEvents.SHUTDOWN, shutdown_esr)
        self.scheduler.register_event(RouterEvents.STARTUP, startup_esr)

    def loop(self):
        self.ongoing = True

        self.scheduler.throw_event(RouterEvents.STARTUP, self)

        try:
            while self.ongoing:
                try:
                    self.scheduler.throw_event(RouterEvents.TICK, self)
                except BaseException as e:
                    c = self.scheduler.throw_event(RouterEvents.EXCEPTION, self, e)
                    if not c:
                        raise
        finally:
            self.scheduler.throw_event(RouterEvents.SHUTDOWN, self)

class RouterEvents:
    TICK = "tick"
    EXCEPTION = "exception"
    STARTUP = "startup"
    SHUTDOWN = "shutdown"
