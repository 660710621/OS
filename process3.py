from process_node import ProcessNode
import time

all_pids = [1, 2, 3]
p3 = ProcessNode(3, all_pids)
p3.start()

while True:
    time.sleep(1)
