///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.47.0

import io.jeamlit.core.Jt;

public class NumberInputExample {
    public static void main(String[] args) {
        Jt.title("Number Input Component Test").use();
        //

        // Integer input with range
        Number age = Jt.numberInput("Age")
                .minValue(0L)
                .maxValue(120L)
                .help("Enter your age in years")
                .use();

        Jt.text("Your age: " + age + " (type: " + (age != null ? age.getClass().getSimpleName() : "null") + ")").use();

        // Float input with step and formatting
        Number price = Jt.numberInput("Price")
                .minValue(0.0)
                .step(0.01)
                .value(9.99)
                .format("%.2f")
                .placeholder("Enter price...")
                .icon("💰")
                .use();

        Jt.text("Price: $" + price + " (type: " + (price != null ? price.getClass().getSimpleName() : "null") + ")").use();

        // No constraints - defaults to Double
        Number anyNumber = Jt.numberInput("Any Number")
                .placeholder("Enter any number...")
                .use();

        Jt.text("Any number: " + anyNumber + " (type: " + (anyNumber != null ? anyNumber.getClass().getSimpleName() : "null") + ")").use();

        // Integer with large range
        Number count = Jt.numberInput("Item Count")
                .minValue(1L)
                .value(100L)
                .help("Number of items to process")
                .use();

        Jt.text("Count: " + count + " (type: " + (count != null ? count.getClass().getSimpleName() : "null") + ")").use();

        // Percentage (0.0 to 1.0)
        Integer percentage = Jt.numberInput("Completion %", Integer.class)
                .minValue(0)
                .maxValue(100)
                .step(1)
                .value(3)
                .format("%0.0f")
                .use();

        Jt.text("Completion: " + (percentage != null ? percentage + "%" : "null")).use();

        // Formatted number with custom step
        Double measurement = Jt.numberInput("Scientific Measurement", Double.class)
                .minValue(0.0)
                .maxValue(10000.0)
                .step(100.1)
                .value(500.5)
                .format("%.3e")// Scientific notation with 3 decimal places
                .help("Uses scientific notation formatting")
                .icon(":material/science:")
                .use();

        Jt.text("Measurement: " + measurement + " (formatted with scientific notation)").use();

        if (Jt.button("Show All Values").use()) {
            Jt.text("=== Summary ===").use();
            Jt.text("Age: " + age).use();
            Jt.text("Price: " + price).use();
            Jt.text("Any Number: " + anyNumber).use();
            Jt.text("Count: " + count).use();
            Jt.text("Percentage: " + percentage).use();
            Jt.text("Measurement: " + measurement).use();
        }
    }
}
