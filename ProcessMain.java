import redis.clients.jedis.*;
import java.util.*;
import java.util.concurrent.*;

public class ProcessMain {
    static String REDIS_CHANNEL = "heartbeat";
    static int TIMEOUT_SEC = 20;

    static Set<Integer> activePids = ConcurrentHashMap.newKeySet();
    static Map<Integer, Long> lastHeartbeatTime = new ConcurrentHashMap<>();
    static volatile int currentBoss = -1;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ProcessMain <PID>");
            return;
        }
        int myPid = Integer.parseInt(args[0]);
        System.out.println("Process Started. PID = " + myPid);

        // Thread 1: Heartbeat Sender
        new Thread(() -> {
            Jedis jedis = new Jedis("localhost");
            try {
                while (true) {
                    jedis.publish(REDIS_CHANNEL, String.valueOf(myPid));
                    Thread.sleep(1000); // send every second
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                jedis.close();
            }
        }, "Sender").start();

        // Thread 2: Heartbeat Listener
        new Thread(() -> {
            Jedis jedis = new Jedis("localhost");
            jedis.subscribe(new JedisPubSub() {
                @Override
                public void onMessage(String channel, String message) {
                    int pid = Integer.parseInt(message.trim());
                    activePids.add(pid);
                    lastHeartbeatTime.put(pid, System.currentTimeMillis());
                }
            }, REDIS_CHANNEL);
        }, "Listener").start();

        // Thread 3: Failure Detector & Boss Election
        new Thread(() -> {
            while (true) {
                try {
                    // Remove PIDs that haven't sent heartbeat > 20 sec
                    long now = System.currentTimeMillis();
                    for (Integer pid : new HashSet<>(activePids)) {
                        long last = lastHeartbeatTime.getOrDefault(pid, 0L);
                        if (now - last > TIMEOUT_SEC * 1000) {
                            activePids.remove(pid);
                            lastHeartbeatTime.remove(pid);
                        }
                    }

                    // Boss Election: Highest PID wins
                    if (!activePids.isEmpty()) {
                        int newBoss = Collections.max(activePids);
                        if (currentBoss != newBoss) {
                            currentBoss = newBoss;
                            if (myPid == newBoss) {
                                System.out.println("[BOSS] I am the boss now: PID=" + newBoss);
                            } else {
                                System.out.println("[INFO] Current Boss: PID=" + newBoss);
                            }
                        }
                    }
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, "FailureDetector").start();
    }
}