/// usr/bin/env jbang "$0" "$@" ; exit $?

//DEPS io.jeamlit:jeamlit:0.49.0
//DEPS dev.langchain4j:langchain4j:0.36.2
//DEPS dev.langchain4j:langchain4j-ollama:0.36.2

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Map;

import io.jeamlit.core.Jt;
import io.jeamlit.core.JtPage;
import io.jeamlit.core.Shared;
import pages.ChatPage;
import pages.ClassificationPage;

public class AIAssistant {
    public static void main(String[] args) {
        // Sidebar configuration
        Jt.title("🤖 AI Assistant").use(Jt.SIDEBAR);

        // About section
        Jt.markdown("""
                This demo showcases [LangChain4j](https://docs.langchain4j.dev) with [Ollama](https://ollama.ai) - completely free local AI!

                Navigate between pages to explore:
                - 🏷️ **Classification**: Sentiment analysis
                - 💬 **Chat**: Simple Q&A assistant
                """).use(Jt.SIDEBAR);

        // Define navigation with multiple pages
        JtPage currentPage = Jt
                .navigation(Jt.page(ClassificationPage.class).title("Classification").home(),
                            Jt.page(ChatPage.class).title("Chat"))
                .use();

        // Model selector
        // Check Ollama connection status
        boolean ollamaRunning = checkOllamaConnection();
        if (ollamaRunning) {
            Jt.markdown("**Model Selection**").use(Jt.SIDEBAR);
            Jt.text("✅ Ollama connected").use(Jt.SIDEBAR);
            String currentModel = Jt.sessionState().getString("selected_model", "gemma3:270m");

            List<String> models = List.of("gemma3:270m", "gemma3:1b");
            String selectedModel = Jt.selectbox("Choose model", models)
                                     .index(models.indexOf(currentModel))
                                     .use(Jt.SIDEBAR);

            // Check if the selected model is loaded in Ollama
            if (isModelLoaded(selectedModel)) {
                Jt.sessionState().put("selected_model", selectedModel);
                currentPage.run();
            } else {
                String errorMsg = """
                       ⚠️ Model not available. \s
                       Run:  \s
                       `ollama run %s` \s
                       and refresh this page.  \s
                       Or make sure the model was not paused automatically by ollama!
                       """.formatted(selectedModel);
                Jt.error(errorMsg).use();
            }
        } else {
            Jt.error("⚠️ Ollama is not running on port `11434`. Start ollama by running `ollama serve` and refresh this page.").use();
        }
    }

    private static boolean checkOllamaConnection() {
        try {
            URL url = new URL("http://localhost:11434/api/tags");
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);
            int responseCode = conn.getResponseCode();
            conn.disconnect();
            return responseCode == 200;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isModelLoaded(String modelName) {
        try {
            final URL url = new URL("http://localhost:11434/api/ps");
            final HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(2000);
            conn.setReadTimeout(2000);

            if (conn.getResponseCode() != 200) {
                return false;
            }

            // Parse JSON response using Jackson
            // Response format: {"models":[{"name":"...","model":"gemma3:270m",...},...]
            final var json = Shared.OBJECT_MAPPER.readValue(conn.getInputStream(), Map.class);
            conn.disconnect();

            // Check if the model field matches our selected model
            final var models = (List<Map<String, Object>>) json.get("models");
            if (models != null) {
                for (var model : models) {
                    if (modelName.equals(model.get("model"))) {
                        return true;
                    }
                }
            }
            return false;

        } catch (Exception e) {
            return false;
        }
    }
}
