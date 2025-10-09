import io.jeamlit.core.Jt;


public class CacheExample {
    public static void main(String[] args) {
        var i = Jt.cache().computeIfAbsentInt("res", k -> {
            Jt.text("performing long computation").use();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 3;
        });
        Jt.text("the value is " + i).use();
    }
}
