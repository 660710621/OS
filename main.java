import java.util.Scanner;

public class main {
    public static void main(String[] args) {
        Scanner sc =new Scanner(System.in);

        MyRunnable1Sec MyRunnable= new MyRunnable1Sec();
        Thread thread = new Thread(MyRunnable);
        thread.setDaemon(true);
        thread.start();

        System.out.println("5 seconds");

        System.out.print("Name: ");
        String name=sc.nextLine();
        System.out.println("hello "+name);


        sc.close();
    }
}
