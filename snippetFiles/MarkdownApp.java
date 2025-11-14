 import io.javelit.core.Jt;

 public class MarkdownApp {
     public static void main(String[] args) {
         // Basic text formatting
         Jt.markdown("*Javelit* is **really** ***cool***.").use();

         // Divider
         Jt.markdown("---").use();

         // Emoji and line breaks
         Jt.markdown("Here's a bouquet — :tulip::cherry_blossom::rose::hibiscus::sunflower::blossom:").use();
     }
 }
