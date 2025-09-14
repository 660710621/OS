import threading
import time
import redis
import json

class ProcessNode:
    def __init__(self, pid, all_pids):
        self.pid = pid
        self.all_pids = all_pids
        self.members = {}
        self.boss_id = None
        self.redis = redis.Redis(host='localhost', port=6379, db=0)
        self.channel = "heartbeat"

    def start(self):
        threading.Thread(target=self.start_listener, daemon=True).start()
        threading.Thread(target=self.start_heartbeat_sender, daemon=True).start()
        threading.Thread(target=self.start_failure_detector, daemon=True).start()

    # ฟัง heartbeat จาก Redis
    def start_listener(self):
        pubsub = self.redis.pubsub()
        pubsub.subscribe(self.channel)

        for message in pubsub.listen():
            if message['type'] != 'message':
                continue
            try:
                data = json.loads(message['data'])
                sender_pid = data["pid"]
                if sender_pid != self.pid:  # ไม่รับของตัวเอง
                    self.members[sender_pid] = time.time()
            except:
                pass

    # ส่ง heartbeat ทุก 1 วินาที
    def start_heartbeat_sender(self):
        while True:
            msg = json.dumps({"pid": self.pid, "status": "alive"})
            self.redis.publish(self.channel, msg)
            time.sleep(1)

    # ตรวจสอบ failure และเลือก Boss
    def start_failure_detector(self):
        while True:
            now = time.time()
            alive_pids = [pid for pid, t in self.members.items() if now - t < 5]
            alive_pids.append(self.pid)

            if not alive_pids:
                self.boss_id = self.pid
            else:
                self.boss_id = max(alive_pids)

            print(f"PID {self.pid}: Current Boss is {self.boss_id}, Members: {alive_pids}")
            time.sleep(3)
