package tech.catheu.jeamlit.components.input;

import tech.catheu.jeamlit.core.Jt;

public class NumberInputExample {
    public static void main(String[] args) {
        Jt.title("Number Input Component Test").use();
        //

        // Integer input with range
        Number age = Jt.numberInput("Age")
                .minValue(0L)
                .maxValue(120L)
                .valueMin() // Uses min value (0) as default
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
        Double percentage = Jt.numberInput("Completion %", Double.class)
                .minValue(0.0)
                .maxValue(1.0)
                .step(10.)
                .value(0.5)
                .format("%0.1f")
                .use();

        Jt.text("Completion: " + (percentage != null ? (percentage * 100) + "%" : "null")).use();

        // Formatted number with custom step
        Double measurement = Jt.numberInput("Scientific Measurement", Double.class)
                .minValue(0.0)
                .maxValue(10000.0)
                .step(100.1)
                .value(500.5)
                .format("%.3e")  // Scientific notation with 3 decimal places
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