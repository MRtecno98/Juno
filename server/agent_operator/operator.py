import socketserver, threading, related
from server.router import RouterEvents
from .config import DescriptorSet
from .register import AgentRegister

class AgentOperator:
    class VoidHandler(socketserver.BaseRequestHandler):
        def handle(self):
            print("Voiding received request:")
            print(f"\tfrom   : {self.client_address}")
            print(f"\trequest: {self.request}")
            print(f"\tserver : {self.server}")
            print(f"\thandler: {self}")

    def __init__(self, router, host, port, configuration_file=None):
        self.tcp_server = socketserver.ThreadingTCPServer((host, port), AgentOperator.VoidHandler)
        self.server_thread = threading.Thread(target=self.tcp_server.serve_forever)
        self.server_thread.setDaemon(True)

        self.router = router
        self.configuration_file = configuration_file
        self.register = AgentRegister(self)

        router.scheduler.register_event(RouterEvents.STARTUP, self.__on_startup)
        router.scheduler.register_event(RouterEvents.SHUTDOWN, self.__on_shutdown)

    def __on_startup(self, serv, cancelled):
        cancel = self.router.scheduler.throw_event(OperatorEvents.STARTUP, self)

        if not cancel:
            if self.configuration_file:
                with open(self.configuration_file) as file:
                    print("Loading agents descriptors")
                    agents = set(related.from_yaml(file.read().strip(), DescriptorSet))

                registered = self.register.register_all(agents)
                print(f"Loaded {len(registered)} agents from configuration")

            print("Starting up master agent operator")
            self.server_thread.start()

            self.started = True

    def __on_shutdown(self, serv, cancelled):
        cancel = self.router.scheduler.throw_event(OperatorEvents.SHUTDOWN, self)

        if not cancel and self.started:
            print("Shutting down master operator server")
            self.tcp_server.shutdown()
            if self.server_thread.is_alive():
                self.router.scheduler.throw_event(OperatorEvents.SHUTDOWN_ERROR, self)
                print("WARNING: Operator server shut down but server thread is still running!")

class OperatorEvents:
    STARTUP = "ag_start"
    SHUTDOWN = "ag_stop"
    SHUTDOWN_ERROR = "ag_stop_error"
