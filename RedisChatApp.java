import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

public class RedisChatApp {
    public static void main(String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("sub")) {
            runSubscriber();
        } else {
            runPublisher();
        }
    }

    // ฟังก์ชัน Publisher
    private static void runPublisher() {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            jedis.publish("chat", "สวัสดีจาก process A!");
            System.out.println("Published message to channel 'chat'");
            
        }
    }

    // ฟังก์ชัน Subscriber
    private static void runSubscriber() {
        Jedis jedis = new Jedis("localhost", 6379);

        JedisPubSub subscriber = new JedisPubSub() {
            public void onMessage(String channel, String message) {
                System.out.println("Received message: " + message + " from channel: " + channel);
            }
        };

        System.out.println("Waiting for messages on channel 'chat'...");
        jedis.subscribe(subscriber, "chat");
    }
}