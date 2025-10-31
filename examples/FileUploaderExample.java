///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.58.0



import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.javelit.components.media.FileUploaderComponent;import io.javelit.core.Jt;
import io.javelit.core.JtUploadedFile;

public class FileUploaderExample {
    public static void main(String[] args) {
        // Title
        Jt.title("📁 File Uploader Examples").use();
        
        Jt.text("This example demonstrates the three file upload modes available in Javelit.").use();
        
        
        
        // Single File Upload
        Jt.title("Single File Upload").use();
        Jt.text("Upload a single image file (JPG, PNG, or GIF):").use();
        
        var res = Jt.fileUploader("Upload an image")
                .type(Arrays.asList(".jpg", ".jpeg", ".png", ".gif"))
                .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.FALSE)
                .help("Only image files are accepted")
                .use();
        if (res.isEmpty()) {
            Jt.text("No image files were uploaded yet").use();
        } else {
            Jt.text("Uploaded image files:").use();
            Jt.text(res.getFirst().filename()).use();
        }
        
        // Multiple Files Upload
        Jt.title("Multiple Files Upload").use();
        Jt.text("Upload multiple CSV files:").use();
        
        Jt.fileUploader("Upload CSV files")
                .type(Arrays.asList(".csv"))
                .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.TRUE)
                .help("You can select multiple CSV files")
                .width(600)// Fixed width
                .use();
        
        
        
        // Directory Upload
        Jt.title("Directory Upload").use();
        Jt.text("Upload an entire folder (all text files will be processed):").use();
        
        Jt.fileUploader("Upload a folder")
                .type(Arrays.asList(".txt", ".md", ".csv"))
                .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.DIRECTORY)
                .help("Select a folder - only text, markdown, and CSV files will be uploaded")
                .disabled(false)
                .use();
        
        // Get the uploaded files (this would typically be done through state management)
        List<JtUploadedFile> directoryFiles = null; // TODO: Get from component state
        
        if (directoryFiles != null && !directoryFiles.isEmpty()) {
            Jt.text("**Directory contents:**").use();
            
            // Group files by extension
            Jt.text("Files by type:");
            directoryFiles.stream()
                    .collect(Collectors.groupingBy(
                            file -> {
                                String name = file.filename();
                                int lastDot = name.lastIndexOf('.');
                                return lastDot > 0 ? name.substring(lastDot) : "no extension";
                            }
                    ))
                    .forEach((ext, files) -> {
                        Jt.text(String.format("- %s: %d file(s)", ext, files.size())).use();
                    });
            
            // Show file tree
            Jt.text("\n**All uploaded files:**");
            for (JtUploadedFile file : directoryFiles) {
                Jt.text(String.format("📄 %s (%.1f KB)", 
                    file.filename(), 
                    file.content().length / 1024.0)).use();
            }
        }
        
        
        
        // Disabled example
        Jt.title("Disabled File Uploader").use();
        Jt.text("This uploader is disabled:").use();
        
        Jt.fileUploader("Disabled uploader")
                .disabled(true)
                .help("This uploader is currently disabled")
                .use();
        
        
        
        // No restrictions example
        Jt.title("Upload Any File Type").use();
        Jt.text("This uploader accepts any file type:").use();
        
        Jt.fileUploader("Upload any file")
                .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.TRUE)
                .help("Any file type is accepted")
                .width("stretch")// Full width
                .use();
        
        // Get the uploaded files (this would typically be done through state management)
        List<JtUploadedFile> anyFiles = null; // TODO: Get from component state
        
        if (anyFiles != null && !anyFiles.isEmpty()) {
            Jt.text(String.format("**Uploaded %d file(s)**", anyFiles.size())).use();
        }
    }
}
