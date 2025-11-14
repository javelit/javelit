import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import io.javelit.core.Jt;

 public class ByteImageApp {
     public static void main(String[] args) throws IOException {
         byte[] imageBytes = generateHexagonImage();
         Jt.image(imageBytes).use();
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
