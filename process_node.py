import threading
import time
import redis
import json

class ProcessNode:
    def __init__(process, pid):
        process.pid = pid
        process.members = {}
        process.boss_id = None
        process.redis = redis.Redis(host='localhost', port=6379, db=0)
        process.channel = "Heartbeat"

    def start(process):
        threading.Thread(target=process.heartbeat_sender).start()
        threading.Thread(target=process.heartbeat_Listener).start()
        threading.Thread(target=process.failure_detectorAndBoss_Election).start()

    # ส่ง heartbeat ทุก 1 วินาที
    def heartbeat_sender(process):
        while True:
            msg = json.dumps({"pid": process.pid, "status": "alive"})
            process.redis.publish(process.channel, msg)
            time.sleep(1)

    # ฟัง heartbeat จาก Redis
    def heartbeat_listener(process):
        pubsub = process.redis.pubsub()
        pubsub.subscribe(process.channel)

        for message in pubsub.listen():
            if message['type'] != 'message':
                continue

            data = json.loads(message['data'])
            sender_pid = data["pid"]
            if sender_pid != process.pid:  # ไม่รับของตัวเอง
                process.members[sender_pid] = time.time()


    # ตรวจสอบ failure และเลือก Boss
    def failure_detectorandboss_election(process):
        while True:
            now = time.time()
            alive_pids = [pid for pid, t in process.members.items() if now - t < 20]
            alive_pids.append(process.pid)

            dead_pids = []
            for pid, t in process.members.items():
                if now - t >= 20:  # ถ้า heartbeat ล่าสุดเกิน 20 วินาที
                    dead_pids.append(pid)

            if dead_pids:
                print(f" Dead PIDs detected: {dead_pids}")

            process.boss_id = max(alive_pids)

            print("Members List :",alive_pids)
            print(f"PID {process.pid}: Current Boss is {process.boss_id}")
            time.sleep(3)