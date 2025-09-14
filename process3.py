from process_node import ProcessNode
import time

p3 = ProcessNode(3)
p3.start()

while True:
    time.sleep(1)
