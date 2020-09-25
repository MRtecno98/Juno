from server.events import EventScheduler, ServerEvents, Priority, event_handler

class NetworkServer:
    def __init__(self):
        self.scheduler = EventScheduler()
        self.ongoing = False
        self.__register_sleep_event()

    def __register_sleep_event(self):
        @event_handler(priority=Priority.ULTRA_LOW)
        def sleep_esr(serv):
            import time
            print("sleeping server \"" + str(serv) + "\"")
            time.sleep(1)

        self.scheduler.register_event(ServerEvents.TICK, sleep_esr)

    def loop(self):
        self.ongoing = True

        while self.ongoing:
            self.scheduler.throw_event(ServerEvents.TICK, self)
