import java.util.List;

import io.javelit.core.Jt;
import io.javelit.core.JtUploadedFile;

 public class FileUploadApp {
     public static void main(String[] args) {
         var uploadedFiles = Jt.fileUploader("Choose a CSV file")
                               .type(List.of(".csv"))
                               .use();

         if (!uploadedFiles.isEmpty()) {
             JtUploadedFile file = uploadedFiles.getFirst();
             Jt.text("Uploaded file: " + file.filename()).use();
             Jt.text("File size: " + file.content().length + " bytes").use();
             Jt.text("Content type: " + file.contentType()).use();
         }
     }
 }
