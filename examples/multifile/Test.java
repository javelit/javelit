import model.Car;
import model.Owner;
import tech.catheu.jeamlit.core.Jt;

public class Test {

    public static void main(String[] args) throws InterruptedException {
        Jt.title(String.valueOf(Car.BLUE)).use();
        Jt.title(String.valueOf(Owner.ME)).use();
        Jt.error("some error").use();
    }
}
