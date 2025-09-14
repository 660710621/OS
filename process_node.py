import threading
import time
import redis
import json

class ProcessNode:
    def __init__(self, pid):
        self.pid = pid
        self.members = {}
        self.boss_id = None
        self.redis = redis.Redis(host='localhost', port=6379, db=0)
        self.channel = "Heartbeat"

    def start(self):
        threading.Thread(target=self.heartbeat_sender, daemon=True).start()
        threading.Thread(target=self.heartbeat_Listener, daemon=True).start()
        threading.Thread(target=self.failure_detectorAndBoss_Election, daemon=True).start()

    # ส่ง heartbeat ทุก 1 วินาที
    def heartbeat_sender(self):
        while True:
            msg = json.dumps({"pid": self.pid,"status":"alive"})
            self.redis.publish(self.channel, msg)
            time.sleep(1)

    # ฟัง heartbeat จาก Redis
    def heartbeat_Listener(self):
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

    # ตรวจสอบ failure และเลือก Boss
    def failure_detectorAndBoss_Election(self):
        while True:
            now = time.time()
            alive_pids = [pid for pid, t in self.members.items() if now - t < 20]
            alive_pids.append(self.pid)

            dead_pids = []
            for pid, t in self.members.items():
                if now - t >= 20:  # ถ้า heartbeat ล่าสุดเกิน 20 วินาที
                    dead_pids.append(pid)

            if dead_pids:
                print(f" Dead PIDs detected: {dead_pids}")

            self.boss_id = max(alive_pids)

            print("Members List :",alive_pids)
            print(f"PID {self.pid}: Current Boss is {self.boss_id}")
            time.sleep(3)