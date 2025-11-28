
/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.75.0

import java.util.List;

import io.javelit.core.Jt;
import io.javelit.core.JtComponent;

public class RadioExample {

  enum Priority {
    LOW,
    MEDIUM,
    HIGH,
    CRITICAL
  }

  public static void main(String[] args) {
    Jt.title("# Radio Component Demo").use();

    Jt.markdown("## Basic Radio Button Group").use();
    String basicChoice = Jt.radio("Choose your favorite color",
                                  List.of("Red", "Green", "Blue", "Yellow"))
                           .use();
    if (basicChoice != null) {
      Jt.text("You selected: **" + basicChoice + "**").use();
    } else {
      Jt.text("No color selected").use();
    }

    Jt.markdown("## Radio with Help").use();
    String themeChoice = Jt.radio("Select UI theme",
                                  List.of("Light", "Dark", "Auto"))
                           .help("Choose your preferred interface theme")
                           .use();
    if (themeChoice != null) {
      Jt.text("🎨 Selected theme: **" + themeChoice + "**").use();
    }

    Jt.markdown("## Radio with Default Selection").use();
    String defaultChoice = Jt.radio("Select difficulty level",
                                    List.of("Easy", "Medium", "Hard", "Expert"))
                             .index(1)// Pre-select "Medium" (index 1)
                             .help("Choose the difficulty level for your session")
                             .use();
    if (defaultChoice != null) {
      Jt.text("🎯 Difficulty: **" + defaultChoice + "**").use();
    }

    Jt.markdown("## Radio with No Default Selection").use();
    String noDefaultChoice = Jt.radio("Select payment method",
                                      List.of("Credit Card", "PayPal", "Bank Transfer", "Cryptocurrency"))
                               .index(null)// Explicitly no pre-selection
                               .help("Choose your preferred payment method")
                               .use();
    if (noDefaultChoice != null) {
      Jt.text("💳 Payment method: **" + noDefaultChoice + "**").use();
    } else {
      Jt.text("⚠️ No payment method selected yet").use();
    }

    Jt.markdown("## Disabled Radio Group").use();
    Jt.radio("Premium features",
             List.of("Advanced Analytics", "Custom Themes", "Priority Support"))
      .disabled(true)
      .help("Upgrade to premium to access these features")
      .use();
    Jt.text("⚪ This radio group is disabled").use();

    Jt.markdown("## Horizontal Radio Layout").use();
    String layoutChoice = Jt.radio("Select size",
                                   List.of("Small", "Medium", "Large"))
                            .horizontal(true)
                            .use();
    if (layoutChoice != null) {
      Jt.text("📏 Selected size: **" + layoutChoice + "**").use();
    }

    Jt.markdown("## Radio with Captions").use();
    String planChoice = Jt.radio("Choose subscription plan",
                                 List.of("Basic", "Pro", "Enterprise"))
                          .captions(List.of(
                              "Free forever - Basic features",
                              "$9/month - Advanced features",
                              "$49/month - Full feature set"))
                          .use();
    if (planChoice != null) {
      Jt.text("💳 Selected plan: **" + planChoice + "**").use();
    }

    Jt.markdown("## Horizontal Radio with Captions").use();
    String shippingChoice = Jt.radio("Select shipping speed",
                                     List.of("Standard", "Express", "Priority", "Same Day"))
                              .horizontal(true)
                              .captions(List.of(
                                  "5-7 business days",
                                  "2-3 business days",
                                  "Next business day",
                                  "Within 4 hours"))
                              .help("Choose your preferred shipping option")
                              .use();
    if (shippingChoice != null) {
      Jt.text("📦 Shipping method: **" + shippingChoice + "**").use();
    }

    Jt.markdown("## Label Visibility Options").use();

    // Visible label (default)
    String visibleChoice = Jt.radio("**Visible** label radio",
                                    List.of("Option A", "Option B"))
                             .labelVisibility(JtComponent.LabelVisibility.VISIBLE)
                             .use();

    // Hidden label (spacer)
    String hiddenChoice = Jt.radio("Hidden label radio",
                                   List.of("Choice 1", "Choice 2"))
                            .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
                            .use();

    // Collapsed label (no space)
    String collapsedChoice = Jt.radio("Collapsed label radio",
                                      List.of("Item X", "Item Y"))
                               .labelVisibility(JtComponent.LabelVisibility.COLLAPSED)
                               .use();

    Jt.markdown("## Width Options").use();

    // Content width (default)
    Jt.text("Content width:").use();
    String contentWidth = Jt.radio("Content width radio",
                                   List.of("First", "Second", "Third"))
                            .width("content")
                            .use();

    // Stretch width
    Jt.text("Stretch width:").use();
    String stretchWidth = Jt.radio("Stretch width radio",
                                   List.of("Alpha", "Beta", "Gamma"))
                            .width("stretch")
                            .use();

    // Fixed pixel width
    Jt.text("Fixed 400px width:").use();
    String fixedWidth = Jt.radio("Fixed width radio",
                                 List.of("One", "Two", "Three"))
                          .width(400)
                          .use();

    Jt.markdown("## Radio with onChange Callback").use();
    Jt.radio("Priority level", List.of("Low", "Medium", "High"))
      .onChange(selected -> {
        if (selected != null) {
          Jt.text("🚨 Callback triggered: Priority set to **" + selected + "**").use();
        }
      })
      .use();

    Jt.markdown("## Enum-based Radio Group").use();
    Priority priority = Jt.radio("Select priority", List.of(Priority.values()))
                          .use();
    if (priority != null) {
      Jt.text("⭐ Selected priority: **" + priority + "**").use();
    }

    Jt.markdown("## Markdown Support in Options").use();
    String markdownChoice = Jt.radio("Choose documentation format",
                                     List.of(
                                         "**Bold** text format",
                                         "*Italic* text format",
                                         "`Code` text format",
                                         "[Link](https://example.com) format",
                                         "~~Strikethrough~~ format"
                                     ))
                              .use();

    Jt.markdown("## Complex Example: Survey Question").use();
    String surveyResponse = Jt.radio("How satisfied are you with this component?",
                                     List.of("Very Dissatisfied",
                                             "Dissatisfied",
                                             "Neutral",
                                             "Satisfied",
                                             "Very Satisfied"))
                              .horizontal(false)
                              .captions(List.of("😞", "😕", "😐", "🙂", "😄"))
                              .help("Your feedback helps us improve")
                              .use();

    if (surveyResponse != null) {
      Jt.text("📊 Your response: **" + surveyResponse + "**").use();
    }

    // Summary section
    Jt.markdown("---").use();
    Jt.markdown("### Summary").use();

    int selectedCount = 0;
    if (basicChoice != null) {
      selectedCount++;
    }
    if (themeChoice != null) {
      selectedCount++;
    }
    if (defaultChoice != null) {
      selectedCount++;
    }
    if (noDefaultChoice != null) {
      selectedCount++;
    }
    if (layoutChoice != null) {
      selectedCount++;
    }
    if (planChoice != null) {
      selectedCount++;
    }
    if (shippingChoice != null) {
      selectedCount++;
    }
    if (visibleChoice != null) {
      selectedCount++;
    }
    if (hiddenChoice != null) {
      selectedCount++;
    }
    if (collapsedChoice != null) {
      selectedCount++;
    }
    if (contentWidth != null) {
      selectedCount++;
    }
    if (stretchWidth != null) {
      selectedCount++;
    }
    if (fixedWidth != null) {
      selectedCount++;
    }
    if (markdownChoice != null) {
      selectedCount++;
    }
    if (priority != null) {
      selectedCount++;
    }
    if (surveyResponse != null) {
      selectedCount++;
    }

    Jt.text("Total radio groups with selections: **" + selectedCount + "**").use();
  }
}
