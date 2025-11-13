///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.69.0



import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import io.javelit.components.media.FileUploaderComponent;import io.javelit.core.Jt;
import io.javelit.core.JtUploadedFile;

public class FileUploaderExample {
    public static void main(String[] args) {
        // Title
        Jt.title("📁 File Uploader Examples");
        
        Jt.text("This example demonstrates the three file upload modes available in Javelit.");
        
        
        
        // Single File Upload
        Jt.title("Single File Upload");
        Jt.text("Upload a single image file (JPG, PNG, or GIF):");
        
        var res = Jt.fileUploader("Upload an image")
                .type(Arrays.asList(".jpg", ".jpeg", ".png", ".gif"))
                .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.FALSE)
                .help("Only image files are accepted")
                .use();
        if (res.isEmpty()) {
            Jt.text("No image files were uploaded yet");
        } else {
            Jt.text("Uploaded image files:");
            Jt.text(res.getFirst().filename());
        }
        
        // Multiple Files Upload
        Jt.title("Multiple Files Upload");
        Jt.text("Upload multiple CSV files:");
        
        Jt.fileUploader("Upload CSV files")
                .type(Arrays.asList(".csv"))
                .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.TRUE)
                .help("You can select multiple CSV files")
                .width(600)// Fixed width
                .use();
        
        
        
        // Directory Upload
        Jt.title("Directory Upload");
        Jt.text("Upload an entire folder (all text files will be processed):");
        
        Jt.fileUploader("Upload a folder")
                .type(Arrays.asList(".txt", ".md", ".csv"))
                .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.DIRECTORY)
                .help("Select a folder - only text, markdown, and CSV files will be uploaded")
                .disabled(false)
                .use();
        
        // Get the uploaded files (this would typically be done through state management)
        List<JtUploadedFile> directoryFiles = null; // TODO: Get from component state
        
        if (directoryFiles != null && !directoryFiles.isEmpty()) {
            Jt.text("**Directory contents:**");
            
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
                        Jt.text(String.format("- %s: %d file(s)", ext, files.size()));
                    });
            
            // Show file tree
            Jt.text("\n**All uploaded files:**");
            for (JtUploadedFile file : directoryFiles) {
                Jt.text(String.format("📄 %s (%.1f KB)", 
                    file.filename(), 
                    file.content().length / 1024.0));
            }
        }
        
        
        
        // Disabled example
        Jt.title("Disabled File Uploader");
        Jt.text("This uploader is disabled:");
        
        Jt.fileUploader("Disabled uploader")
                .disabled(true)
                .help("This uploader is currently disabled")
                .build();
        
        
        
        // No restrictions example
        Jt.title("Upload Any File Type");
        Jt.text("This uploader accepts any file type:");
        
        Jt.fileUploader("Upload any file")
                .acceptMultipleFiles(FileUploaderComponent.MultipleFiles.TRUE)
                .help("Any file type is accepted")
                .width("stretch")// Full width
                .build();
        
        // Get the uploaded files (this would typically be done through state management)
        List<JtUploadedFile> anyFiles = null; // TODO: Get from component state
        
        if (anyFiles != null && !anyFiles.isEmpty()) {
            Jt.text(String.format("**Uploaded %d file(s)**", anyFiles.size()));
        }
    }
}
