///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.32.0



import java.time.LocalDate;
import java.time.temporal.ChronoUnit;import io.jeamlit.core.Jt;
import io.jeamlit.core.JtComponent;

public class DateInputExample {
    public static void main(String[] args) {
        Jt.title("# DateInput Component Demo").use();

        Jt.markdown("## Basic Date Input").use();
        LocalDate basicDate = Jt.dateInput("Select a date").use();
        if (basicDate != null) {
            Jt.text("Selected date: **" + basicDate + "**").use();
        } else {
            Jt.text("No date selected").use();
        }

        Jt.markdown("## Date Input with Default Value").use();
        LocalDate defaultDate = Jt
                .dateInput("Birth date")
                .value(LocalDate.of(1990, 6, 15))
                .help("Select your birth date")
                .use();
        if (defaultDate != null) {
            Jt.text("Birth date: **" + defaultDate + "**").use();
        }

        Jt.markdown("## Date Input with Min/Max Constraints").use();
        LocalDate constrainedDate = Jt
                .dateInput("Appointment date")
                .value(LocalDate.now())
                .minValue(LocalDate.now())
                .maxValue(LocalDate.now().plusMonths(3))
                .help("Select a date within the next 3 months")
                .use();
        if (constrainedDate != null) {
            Jt.text("Appointment: **" + constrainedDate + "**").use();
        }

        Jt.markdown("## Date Input with Different Formats").use();

        LocalDate usFormat = Jt.dateInput("US format (MM/DD/YYYY)").format("MM/DD/YYYY").use();
        if (usFormat != null) {
            Jt.text("US format: **" + usFormat + "**").use();
        }

        LocalDate euroFormat = Jt.dateInput("European format (DD/MM/YYYY)").format("DD/MM/YYYY").use();
        if (euroFormat != null) {
            Jt.text("European format: **" + euroFormat + "**").use();
        }

        LocalDate dashFormat = Jt.dateInput("Dash separator (YYYY-MM-DD)").format("YYYY-MM-DD").use();
        if (dashFormat != null) {
            Jt.text("Dash format: **" + dashFormat + "**").use();
        }

        LocalDate dotFormat = Jt.dateInput("Dot separator (DD.MM.YYYY)").format("DD.MM.YYYY").use();
        if (dotFormat != null) {
            Jt.text("Dot format: **" + dotFormat + "**").use();
        }

        Jt.markdown("## Date Input with No Default Value").use();
        LocalDate nullDate = Jt.dateInput("Optional date").value(null).help("This field starts with no value").use();
        if (nullDate != null) {
            Jt.text("Optional date: **" + nullDate + "**").use();
        } else {
            Jt.text("No optional date selected yet").use();
        }

        Jt.markdown("## Disabled Date Input").use();
        Jt.dateInput("Disabled date").value(LocalDate.now()).disabled(true).help("This date input is disabled").use();

        Jt.markdown("## Label Visibility Options").use();

        // Visible label (default)
        LocalDate visibleDate = Jt
                .dateInput("**Visible** label date")
                .labelVisibility(JtComponent.LabelVisibility.VISIBLE)
                .use();

        // Hidden label (spacer)
        LocalDate hiddenDate = Jt
                .dateInput("Hidden label date")
                .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
                .use();

        // Collapsed label (no space)
        LocalDate collapsedDate = Jt
                .dateInput("Collapsed label date")
                .labelVisibility(JtComponent.LabelVisibility.COLLAPSED)
                .use();

        Jt.markdown("## Width Options").use();

        // Stretch width (default)
        Jt.text("Stretch width:").use();
        LocalDate stretchDate = Jt.dateInput("Stretch width date").width("stretch").use();

        // Fixed pixel width
        Jt.text("Fixed 250px width:").use();
        LocalDate fixedDate = Jt.dateInput("Fixed width date").width(250).use();

        Jt.markdown("## Date Input with onChange Callback").use();
        Jt.dateInput("Event date").onChange(date -> {
            if (date != null) {
                Jt.text("🔄 Date changed to: **" + date + "**").use();

                // Calculate days from today
                long daysFromNow = ChronoUnit.DAYS.between(LocalDate.now(), date);
                if (daysFromNow > 0) {
                    Jt.text("That's **" + daysFromNow + "** days from now").use();
                } else if (daysFromNow < 0) {
                    Jt.text("That's **" + Math.abs(daysFromNow) + "** days ago").use();
                } else {
                    Jt.text("That's **today**!").use();
                }
            }
        }).use();

        Jt.markdown("## Date Range Selection Example").use();
        Jt.text("Select a date range for your vacation:").use();

        var cols = Jt.columns("date-range", 2).use();

        LocalDate startDate = Jt
                .dateInput("Start date")
                .minValue(LocalDate.now())
                .help("Select vacation start date")
                .use(cols.col(0));
        LocalDate endDate = Jt
                .dateInput("End date")
                .minValue(startDate != null ? startDate : LocalDate.now())
                .value(startDate != null ? startDate : LocalDate.now())
                .help("Select vacation end date")
                .use(cols.col(1));

        if (startDate != null && endDate != null) {
            long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
            Jt.text("🏖️ Vacation: **" + startDate + "** to **" + endDate + "** (" + days + " days)").use();
        }

        // Summary section
        Jt.divider().use();
        Jt.markdown("### Summary").use();

        int selectedCount = 0;
        if (basicDate != null) {
            selectedCount++;
        }
        if (defaultDate != null) {
            selectedCount++;
        }
        if (constrainedDate != null) {
            selectedCount++;
        }
        if (usFormat != null) {
            selectedCount++;
        }
        if (euroFormat != null) {
            selectedCount++;
        }
        if (dashFormat != null) {
            selectedCount++;
        }
        if (dotFormat != null) {
            selectedCount++;
        }
        if (nullDate != null) {
            selectedCount++;
        }
        if (visibleDate != null) {
            selectedCount++;
        }
        if (hiddenDate != null) {
            selectedCount++;
        }
        if (collapsedDate != null) {
            selectedCount++;
        }
        if (stretchDate != null) {
            selectedCount++;
        }
        if (fixedDate != null) {
            selectedCount++;
        }
        if (startDate != null) {
            selectedCount++;
        }
        if (endDate != null) {
            selectedCount++;
        }

        Jt.text("Total date inputs with values: **" + selectedCount + "**").use();
    }
}
