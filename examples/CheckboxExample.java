///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.68.0

import io.javelit.core.Jt;
import io.javelit.core.JtComponent;

public class CheckboxExample {
    public static void main(String[] args) {
        Jt.title("# Checkbox Component Demo").use();
        
        Jt.markdown("## Basic Checkbox").use();
        boolean basicCheckbox = Jt.checkbox("Enable notifications").use();
        if (basicCheckbox) {
            Jt.text("☑️ Notifications are **enabled**").use();
        } else {
            Jt.text("☐ Notifications are **disabled**").use();
        }

        Jt.markdown("## Checkbox with Help").use();
        boolean helpCheckbox = Jt.checkbox("Dark mode")
                .help("Enable dark theme for the interface")
                .use();
        if (helpCheckbox) {
            Jt.text("🌙 Dark mode is **enabled**").use();
        } else {
            Jt.text("☀️ Light mode is **enabled**").use();
        }

        Jt.markdown("## Checkbox with Default Value").use();
        boolean defaultCheckbox = Jt.checkbox("Auto-save")
                .value(true)// Default to checked
                .help("Automatically save your work")
                .use();
        if (defaultCheckbox) {
            Jt.text("💾 Auto-save is **enabled** (default)").use();
        } else {
            Jt.text("💾 Auto-save is **disabled**").use();
        }

        Jt.markdown("## Disabled Checkbox").use();
        Jt.checkbox("Premium feature")
                .disabled(true)
                .help("Upgrade to premium to access this feature")
                .use();
        Jt.text("⚪ This checkbox is disabled").use();

        Jt.markdown("## Label Visibility Options").use();
        
        // Visible label (default)
        boolean visibleCheckbox = Jt.checkbox("**Visible** label checkbox")
                .labelVisibility(JtComponent.LabelVisibility.VISIBLE)
                .use();
        
        // Hidden label (spacer)
        boolean hiddenCheckbox = Jt.checkbox("Hidden label checkbox")
                .labelVisibility(JtComponent.LabelVisibility.HIDDEN)
                .use();
        
        // Collapsed label (no space)
        boolean collapsedCheckbox = Jt.checkbox("Collapsed label checkbox")
                .labelVisibility(JtComponent.LabelVisibility.COLLAPSED)
                .use();

        if (visibleCheckbox || hiddenCheckbox || collapsedCheckbox) {
            Jt.text("At least one label visibility checkbox is checked").use();
        }

        Jt.markdown("## Width Options").use();
        
        // Content width (default)
        Jt.text("Content width:").use();
        boolean contentWidth = Jt.checkbox("Content width checkbox")
                .width("content")
                .use();
        
        // Stretch width
        Jt.text("Stretch width:").use();
        boolean stretchWidth = Jt.checkbox("Stretch width checkbox")
                .width("stretch")
                .use();
        
        // Fixed pixel width
        Jt.text("Fixed 300px width:").use();
        boolean fixedWidth = Jt.checkbox("Fixed width checkbox")
                .width(300)
                .use();

        Jt.markdown("## Checkbox with onChange Callback").use();
        Jt.checkbox("Checkbox with callback")
                .onChange(checked -> {
                    if (checked) {
                        Jt.text("🎉 Callback triggered: Checkbox is **CHECKED**").use();
                    } else {
                        Jt.text("😴 Callback triggered: Checkbox is **UNCHECKED**").use();
                    }
                })
                .use();

        Jt.markdown("## Markdown Support in Labels").use();
        Jt.checkbox("Checkbox with **bold**, *italic*, and `code` text").use();
        Jt.checkbox("Checkbox with [link](https://example.com) and ~~strikethrough~~").use();

        // Summary section
        Jt.markdown("---").use();
        Jt.markdown("### Summary").use();
        
        int checkedCount = 0;
        if (basicCheckbox) {
            checkedCount++;
        }
        if (helpCheckbox) {
            checkedCount++;
        }
        if (defaultCheckbox) {
            checkedCount++;
        }
        if (visibleCheckbox) {
            checkedCount++;
        }
        if (hiddenCheckbox) {
            checkedCount++;
        }
        if (collapsedCheckbox) {
            checkedCount++;
        }
        if (contentWidth) {
            checkedCount++;
        }
        if (stretchWidth) {
            checkedCount++;
        }
        if (fixedWidth) {
            checkedCount++;
        }
        
        Jt.text("Total checked checkboxes: **" + checkedCount + "**").use();
    }
}
