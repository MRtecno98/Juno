import socketserver, threading
from server.events import ServerEvents

class AgentOperator:
    class VoidHandler(socketserver.BaseRequestHandler):
        def handle(self):
            print(f"Voiding received request: {self.request}")

    def __init__(self, router, host, port):
        self.tcp_server = socketserver.ThreadingTCPServer((host, port), AgentOperator.VoidHandler)
        self.server_thread = threading.Thread(target=self.tcp_server.serve_forever)
        self.server_thread.setDaemon(True)

        router.scheduler.register_event(ServerEvents.STARTUP, self.__on_startup)
        router.scheduler.register_event(ServerEvents.SHUTDOWN, self.__on_shutdown)

    def __on_startup(self, serv, cancelled):
        print("Starting up master agent operator")
        self.server_thread.start()

    def __on_shutdown(self, serv, cancelled):
        print("Shutting down master operator server")
        self.tcp_server.shutdown()
        if self.server_thread.is_alive():
            print("WARNING: Operator server shut down but server thread still running!")
