from process_node import ProcessNode
import time

all_pids = [1, 2, 3]
p1 = ProcessNode(1, all_pids)
p1.start()

while True:
    time.sleep(1)
