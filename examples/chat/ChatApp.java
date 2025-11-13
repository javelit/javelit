/// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.javelit:javelit:0.69.0

import java.util.ArrayList;
import java.util.List;

import io.javelit.core.Jt;
import io.javelit.core.JtComponent;
import io.javelit.core.JtContainer;

// chat app example
public class ChatApp {

    public static void main(String[] args) {
        Jt.title("Simple chat").use();

        // get chat history
        List<Message> messages = (List<Message>) Jt
                .sessionState()
                .computeIfAbsent("messages", key -> new ArrayList<>());

        // show messages
        var messagesContainer = Jt.container().use();
        for (Message message : messages) {
            message.use(messagesContainer);
        }

        // Accept user input
        String prompt = Jt
                .textInput("What is up?")
                .labelVisibility(JtComponent.LabelVisibility.COLLAPSED)
                .placeholder("What is up?")
                .use();

        if (prompt != null && !prompt.trim().isEmpty()) {
            // show user message in the chat
            Message userMsg = new Message("user", prompt);
            userMsg.use(messagesContainer);
            // save user message to history
            messages.add(userMsg);

            // generate ai response
            String response = fetchAiResponse(prompt);
            Message aiMsg = new Message("assistant", response);
            aiMsg.use(messagesContainer);
            // save ai message to history
            messages.add(aiMsg);
        }
    }



    private static String fetchAiResponse(String userInput) {
        try {
            //simualte API call time
            Thread.sleep(500);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // Simple mock responses
        String input = userInput.toLowerCase();

        if (input.contains("hello") || input.contains("hi")) {
            return "Hello there! How can I assist you today?";
        } else if (input.contains("how are you")) {
            return "I'm doing great, thanks for asking!";
        } else if (input.contains("name")) {
            return "I'm Javelit Chat Bot, a demo application built with Javelit!";
        } else if (input.contains("bye") || input.contains("goodbye")) {
            return "Goodbye! Have a great day!";
        } else {
            return "Hi, human! Is there anything I can help you with?";
        }
    }
}
