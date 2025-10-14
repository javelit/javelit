///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.jeamlit:jeamlit:0.44.0

import io.jeamlit.core.Jt;
import io.jeamlit.core.JtComponent;

public class ToggleExample {
    public static void main(String[] args) {
        Jt.title("# Toggle Component Demo").use();
        
        Jt.markdown("## Basic Toggle").use();
        boolean basicToggle = Jt.toggle("Enable notifications").use();
        if (basicToggle) {
            Jt.text("✅ Notifications are **enabled**").use();
        } else {
            Jt.text("❌ Notifications are **disabled**").use();
        }

        Jt.markdown("## Toggle with Help").use();
        boolean helpToggle = Jt.toggle("Dark mode")
                .help("Toggle between light and dark theme")
                .use();
        if (helpToggle) {
            Jt.text("🌙 Dark mode is **on**").use();
        } else {
            Jt.text("☀️ Light mode is **on**").use();
        }

        Jt.markdown("## Toggle with Default Value").use();
        boolean defaultToggle = Jt.toggle("Auto-save")
                .value(true)// Default to enabled
                .help("Automatically save your work")
                .use();
        if (defaultToggle) {
            Jt.text("💾 Auto-save is **enabled** (default)").use();
        } else {
            Jt.text("💾 Auto-save is **disabled**").use();
        }

        Jt.markdown("## Disabled Toggle").use();
        Jt.toggle("Premium feature")
                .disabled(true)
                .help("Upgrade to premium to access this feature")
                .use();
        Jt.text("⚪ This toggle is disabled").use();

        Jt.markdown("## Label Visibility Options").use();
        
        // Visible label (default)
        boolean visibleToggle = Jt.toggle("**Visible** label toggle")
                .labelVisibility(JtComponent.LabelVisibility.VISIBLE)
                .use();
        
        // Hidden label (spacer)
        boolean hiddenToggle = Jt.toggle("Hidden label toggle")
                .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
                .use();
        
        // Collapsed label (no space)
        boolean collapsedToggle = Jt.toggle("Collapsed label toggle")
                .labelVisibility(JtComponent.LabelVisibility.COLLAPSED)
                .use();

        if (visibleToggle || hiddenToggle || collapsedToggle) {
            Jt.text("At least one label visibility toggle is enabled").use();
        }

        Jt.markdown("## Width Options").use();
        
        // Content width (default)
        Jt.text("Content width:").use();
        boolean contentWidth = Jt.toggle("Content width toggle")
                .width("content")
                .use();
        
        // Stretch width
        Jt.text("Stretch width:").use();
        boolean stretchWidth = Jt.toggle("Stretch width toggle")
                .width("stretch")
                .use();
        
        // Fixed pixel width
        Jt.text("Fixed 300px width:").use();
        boolean fixedWidth = Jt.toggle("Fixed width toggle")
                .width(300)
                .use();

        Jt.markdown("## Toggle with onChange Callback").use();
        Jt.toggle("Toggle with callback")
                .onChange(enabled -> {
                    if (enabled) {
                        Jt.text("🎉 Callback triggered: Toggle was **ON**").use();
                    } else {
                        Jt.text("😴 Callback triggered: Toggle was **OFF**").use();
                    }
                })
                .use();

        Jt.markdown("## Markdown Support in Labels").use();
        Jt.toggle("Toggle with **bold**, *italic*, and `code` text").use();
        Jt.toggle("Toggle with [link](https://example.com) and ~~strikethrough~~").use();

        // Summary section
        Jt.markdown("---").use();
        Jt.markdown("### Summary").use();
        
        int enabledCount = 0;
        if (basicToggle) {
            enabledCount++;
        }
        if (helpToggle) {
            enabledCount++;
        }
        if (defaultToggle) {
            enabledCount++;
        }
        if (visibleToggle) {
            enabledCount++;
        }
        if (hiddenToggle) {
            enabledCount++;
        }
        if (collapsedToggle) {
            enabledCount++;
        }
        if (contentWidth) {
            enabledCount++;
        }
        if (stretchWidth) {
            enabledCount++;
        }
        if (fixedWidth) {
            enabledCount++;
        }
        
        Jt.text("Total enabled toggles: **" + enabledCount + "**").use();
    }
}
