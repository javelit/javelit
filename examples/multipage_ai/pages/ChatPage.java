package pages;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;
import io.javelit.core.Jt;
import io.javelit.core.JtContainer;

public class ChatPage {

    // Define the AI Assistant interface
    interface Assistant {
        @SystemMessage("You are a helpful AI assistant. Provide clear, concise, and accurate answers.")
        @UserMessage("{{question}}")
        String chat(@V("question") String question);
    }

    public static void main(String[] args) {
        Jt.title("💬 Chat Assistant").use();

        Jt.markdown("""
                            This page demonstrates a **simple Q&A chat** using LangChain4j with Ollama.
                            Ask any question and get an AI-powered response!
                            """).use();

        // Get model from session state
        String modelName = Jt.sessionState().getString("selected_model", "gemma3:270m");

        // Question input
        JtContainer formContainer = Jt.form().use();
        String question = Jt.textArea("Your question")
                            .value("What is the meaning of life?")
                            .height(100)
                            .placeholder("Enter your question here...")
                            .use(formContainer);

        // Temperature slider
        double temperature = Jt.slider("Temperature - the higher the more creative")
                .min(0.0)
                .max(2.0)
                .value(0.7)
                .step(0.1)
                .use(formContainer);

        // Ask button
        if (Jt.formSubmitButton("Get Answer").type("primary").use(formContainer)) {
            if (question.trim().isEmpty()) {
                Jt.text("Please enter a question.").use();
            } else {
                try {
                    var inPlaceContainer = Jt.empty().use();
                    // Show spinner while processing
                    Jt.text("Thinking...").use(inPlaceContainer);

                    // Create Ollama model
                    var chatModel = OllamaChatModel.builder()
                                                   .baseUrl("http://localhost:11434")
                                                   .modelName(modelName)
                                                   .temperature(temperature)
                                                   .build();

                    // Create AI-powered Assistant
                    Assistant assistant = AiServices.create(Assistant.class, chatModel);

                    // Get answer
                    String answer = assistant.chat(question);

                    // Display answer
                    Jt.markdown("### 💡 Answer\n" + answer).use(inPlaceContainer);

                } catch (Exception e) {
                    Jt.error("Error getting answer: " + e.getMessage()).use();
                }
            }
        }
    }
}
