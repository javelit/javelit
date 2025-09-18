package pages;

import io.jeamlit.core.Jt;

public class UsersPage {
    public static void main(String[] args) {
        Jt.title("👥 User Management").use();
        Jt.text("Manage your users here.").use();
        
        // Simple user form
        String name = Jt.textInput("User Name")
            .placeholder("Enter user name")
            .use();
        
        if (Jt.button("Add User").use() && !name.isEmpty()) {
            Jt.text("✅ User '" + name + "' added!").use();
        }
    }
}
