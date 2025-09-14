from process_node import ProcessNode
import time

p1 = ProcessNode(1)
p1.start()

while True:
    time.sleep(1)
