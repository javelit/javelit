package tech.catheu.jeamlit.core;

public class Sleeping {
    public static void main(String[] args) throws InterruptedException {

        for (int i = 0; i < 10; i++) {
            Jt.text("Let me sleep for 1 sec").use();
            Thread.sleep(1000);
        }
    }
}