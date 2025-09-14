from process_node import ProcessNode
import time

p2 = ProcessNode(2)
p2.start()

while True:
    time.sleep(1)
