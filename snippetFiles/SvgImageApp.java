 import io.javelit.core.Jt;

 public class SvgImageApp {
     public static void main(String[] args) {
         String svg = """
                 <svg width="200" height="200" xmlns="http://www.w3.org/2000/svg">
                     <circle cx="100" cy="100" r="80" fill="#4CAF50" />
                     <path d="M 60 100 L 90 130 L 140 80" stroke="white" stroke-width="8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                 </svg>
                 """;
         Jt.imageFromSvg(svg).use();
     }
 }
