// import redis.clients.jedis.Jedis;
// import redis.clients.jedis.JedisPubSub;

// public class RedisSubscriber {
//     public static void main(String[] args) {
//         Jedis jedis = new Jedis("localhost", 6379);

//         // สร้าง listener
//         JedisPubSub subscriber = new JedisPubSub() {
//             public void onMessage(String channel, String message) {
//                 System.out.println("Received message: " + message + " from channel: " + channel);
//             }
//         };

//         System.out.println("Waiting for messages on channel 'chat'...");
//         // Subscribe ไปที่ channel ชื่อ "chat"
//         jedis.subscribe(subscriber, "chat");
//     }
// }