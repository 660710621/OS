import redis.clients.jedis.Jedis;

public class RedisPublisher {
    public static void main(String[] args) {
        try (Jedis jedis = new Jedis("localhost", 6379)) {
            // ส่งข้อความไปที่ channel ชื่อ "chat"
            jedis.publish("chat", "สวัสดีจาก process A!");
            System.out.println("Published message to channel 'chat'");
        }
    }
}
