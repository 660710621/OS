from process_node import ProcessNode
import time

all_pids = [1, 2, 3]
p2 = ProcessNode(2, all_pids)
p2.start()

while True:
    time.sleep(1)
