import sys, os

sys.path.append(os.path.abspath(os.path.join(os.path.split(__file__)[0], "..")))

import server

if __name__ == "__main__":
    n = server.LogisticalRouter()
    n.loop()
