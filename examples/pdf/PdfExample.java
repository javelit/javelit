///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.70.0



import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;import io.javelit.core.Jt;

public class PdfExample {

    public static void main(String[] args) throws IOException {
        Jt.title("# PDF Component Showcase").use();
        Jt.markdown("This demo showcases all the capabilities of the `Jt.pdf` component in Javelit.").use();

        // Example 1: Public URL
        Jt.title("## 1. Public URL").use();
        Jt.markdown("Load a PDF from a public URL. This is the simplest way to display a PDF.").use();
        Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                .use();

        // Example 2: Static folder
        Jt.title("## 2. PDF from Static Folder").use();
        Jt.markdown("""
                Load a PDF from your application's static folder. The file path is relative to the static directory.
                This is useful for assets bundled with your application.
                """).use();
        Jt.pdf("app/static/sample.pdf")
                .use();

        // Example 3: Local file
        Jt.title("## 3. PDF from Local File").use();
        Jt.markdown("""
                Load a PDF from a local file path. The PDF data is read from disk and served by Javelit.
                This is useful for accessing PDFs anywhere on the filesystem.
                """).use();
        Jt.pdf(Path.of("examples/pdf/sample.pdf"))
                .use();

        // Example 4: From bytes
        Jt.title("## 4. PDF from Bytes").use();
        Jt.markdown("""
                Load a PDF from raw bytes. This is useful when you have PDF data in memory,
                such as from a database or API response.
                """).use();
        byte[] pdfBytes = Files.readAllBytes(Path.of("examples/pdf/sample.pdf"));
        Jt.pdf(pdfBytes)
                .use();

        // Example 5: Height - Stretch
        Jt.title("## 5. Height: Stretch").use();
        Jt.markdown("The PDF viewer stretches to match the container height.").use();
        var container = Jt.container().height(600).use();
        Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                .height("stretch")
                .use(container);

        // Example 6: Height - Fixed Pixels
        Jt.title("## 6. Height: Fixed Pixels").use();
        Jt.markdown("Set a fixed height in pixels (e.g., 300px).").use();
        Jt.pdf("https://cdn.jsdelivr.net/gh/javelit/public_assets@main/pdf/dummy.pdf")
                .height(300)
                .use();
    }
}
