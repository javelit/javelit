package pages;

import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.UserMessage;
import io.jeamlit.core.Jt;

public class ClassificationPage {

    // Define the Sentiment enum
    enum Sentiment {
        POSITIVE,
        NEUTRAL,
        NEGATIVE
    }

    // Define the AI-powered Sentiment Analyzer interface
    interface SentimentAnalyzer {
        @UserMessage("Analyze sentiment of {{it}}")
        Sentiment analyzeSentimentOf(String text);
    }

    public static void main(String[] args) {
        Jt.title("🏷️ Sentiment Classification").use();

        Jt.markdown("""
                           This page demonstrates **sentiment classification** using LangChain4j with Ollama. \s
                           Enter some text below and the AI will classify its sentiment as POSITIVE, NEUTRAL, or NEGATIVE.
                           """).use();

        // Get model from session state
        String modelName = Jt.sessionState().getString("selected_model", "gemma3:270m");
        // Use fixed temperature for deterministic classification

        // Text input for classification
        Jt.markdown("""
                            ### 📝 Input
                            *💡 Try these examples:*
                            """).use();
        // Examples section
        String exampleValue = Jt.sessionState().computeIfAbsentString("input", k -> "I love this product! It's amazing and works perfectly.");

        var col1 = Jt.columns(3).use();
        if (Jt.button("Positive example").use(col1.col(0))) {
            exampleValue = "This is absolutely wonderful! Best experience ever!";
        }
        if (Jt.button("Neutral example").use(col1.col(1))) {
            exampleValue = "The product works as described. Nothing special.";
        }
        if (Jt.button("Negative example").use(col1.col(2))) {
            exampleValue = "Terrible quality. Very disappointed with this purchase.";
        }
        Jt.sessionState().put("input", exampleValue);

        String text = Jt
                .textArea("Enter text to analyze")
                .value(exampleValue)
                .height(150)
                .use();

        // Classify button
        if (Jt.button("Classify Sentiment").type("primary").use()) {
            if (text.trim().isEmpty()) {
                Jt.text("Please enter some text to analyze.").use();
            } else {
                try {
                    // Show spinner while processing
                    Jt.text("Analyzing sentiment...").use();

                    // Create Ollama model
                    var chatModel = OllamaChatModel
                            .builder()
                            .baseUrl("http://localhost:11434")
                            .modelName(modelName)
                            .temperature(0.3)
                            .build();

                    // Create AI-powered Sentiment Analyzer
                    SentimentAnalyzer analyzer = AiServices.create(SentimentAnalyzer.class, chatModel);

                    // Analyze sentiment
                    Sentiment sentiment = analyzer.analyzeSentimentOf(text);

                    // Display result with color coding
                    Jt.markdown("### 🎯 Result").key("classi-result").use();

                    String emoji = switch (sentiment) {
                        case POSITIVE -> "😊";
                        case NEUTRAL -> "😐";
                        case NEGATIVE -> "😞";
                    };

                    Jt.markdown("**Sentiment:** %s %s".formatted(emoji, sentiment)).use();

                } catch (Exception e) {
                    throw e;
                }
            }
        }
    }
}
