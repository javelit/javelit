///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.56.0




import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;import io.javelit.core.Jt;

public class ImageExample {

    public static void main(String[] args) throws IOException {
        Jt.title("# Image Component Showcase").use();
        Jt.markdown("This demo showcases all the capabilities of the `st.image` component in Javelit.").use();

        // Example 1: Public URL
        Jt.title("## 1. Public URL").use();
        Jt.markdown("Load an image from a public URL. This is the simplest way to display an image.").use();
        Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                .caption("Mountain landscape from Unsplash")
                .use();

        // Example 2: Static folder
        Jt.title("## 2. Image from Static Folder").use();
        Jt.markdown("""
                Load an image from your application's static folder. The file path is relative to the static directory.
                This is useful for assets bundled with your application.
                """).use();
        Jt.image("app/static/mountains.jpg")
                .caption("Mountains from static folder")
                .use();

        // Example 3: Local file
        Jt.title("## 3. Image from Local File").use();
        Jt.markdown("""
                Load an image from a local file path. The image data is read from disk and served by Javelit.
                This is useful for accessing images anywhere on the filesystem.
                """).use();
        Jt.image(Path.of("examples/image/mountains.jpg"))
                .caption("Mountains from local file")
                .use();

        // Example 4: From bytes
        Jt.title("## 4. Image from Bytes").use();
        Jt.markdown("""
                Load an image from raw bytes. This is useful when you have image data in memory,
                such as from a database or generated programmatically.
                """).use();
        byte[] imageBytes = generateHexagonImage();
        Jt.image(imageBytes)
                .caption("Programmatically generated hexagon (800x400)")
                .use();

        // Example 5: SVG
        Jt.title("## 5. SVG Image").use();
        Jt.markdown("""
                Display SVG images by providing the SVG string directly.
                This is perfect for vector graphics, icons, and diagrams.
                """).use();
        String svg = """
                <svg width="200" height="200" xmlns="http://www.w3.org/2000/svg">
                    <circle cx="100" cy="100" r="80" fill="#4CAF50" />
                    <path d="M 60 100 L 90 130 L 140 80" stroke="white" stroke-width="8" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
                </svg>
                """;
        Jt.imageFromSvg(svg)
                .caption("Simple SVG checkmark icon")
                .use();

        // Example 6: With rich markdown caption
        Jt.title("## 6. Image with Rich Caption").use();
        Jt.markdown("Captions support full Markdown formatting including **bold**, *italic*, and [links](https://example.com).").use();
        Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                .caption("**Beautiful mountains** in *stunning* detail. [Learn more](https://unsplash.com)")
                .use();

        // Example 7: Width - Content (default)
        Jt.title("## 7. Width: Content (Default)").use();
        Jt.markdown("The image width matches its natural size, but doesn't exceed the container width.").use();
        Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                .width("content")
                .caption("Content width (natural size)")
                .use();

        // Example 8: Width - Stretch
        Jt.title("## 8. Width: Stretch").use();
        Jt.markdown("The image stretches to fill the full width of the parent container.").use();
        Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                .width("stretch")
                .caption("Stretched to full width")
                .use();

        // Example 9: Width - Fixed Pixels
        Jt.title("## 9. Width: Fixed Pixels").use();
        Jt.markdown("Set a fixed width in pixels. If larger than the container, it will be constrained.").use();
        Jt.image("https://raw.githubusercontent.com/javelit/public_assets/refs/heads/main/image/mountains2.jpg")
                .width(400)
                .caption("Fixed width: 400px")
                .use();
    }

    private static byte[] generateHexagonImage() throws IOException {
        int width = 800;
        int height = 400;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Enable anti-aliasing for smoother edges
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Light blue background
        g2d.setColor(new Color(240, 248, 255));
        g2d.fillRect(0, 0, width, height);

        // Draw hexagon
        int centerX = width / 2;
        int centerY = height / 2;
        int radius = 150;

        int[] xPoints = new int[6];
        int[] yPoints = new int[6];

        for (int i = 0; i < 6; i++) {
            double angle = Math.PI / 3 * i - Math.PI / 6;
            xPoints[i] = centerX + (int) (radius * Math.cos(angle));
            yPoints[i] = centerY + (int) (radius * Math.sin(angle));
        }

        // Fill hexagon with teal color
        g2d.setColor(new Color(32, 178, 170));
        g2d.fillPolygon(xPoints, yPoints, 6);

        // Draw hexagon border
        g2d.setColor(new Color(0, 128, 128));
        g2d.setStroke(new BasicStroke(3));
        g2d.drawPolygon(xPoints, yPoints, 6);

        g2d.dispose();

        // Convert to PNG bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "PNG", baos);
        return baos.toByteArray();
    }
}
