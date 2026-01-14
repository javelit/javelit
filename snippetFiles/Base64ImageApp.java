import io.javelit.core.Jt;

public class Base64ImageApp {
    public static void main(String[] args) {
        // A small 10x10 red PNG image encoded as base64
        // This is a minimal valid PNG that displays a solid red square
        String base64Png = "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVR42mP8z8DwHwYYGBgYBhBHAQBVhQOBMN0vkwAAAABJRU5ErkJggg==";

        Jt.title("Base64 Image Demo").use();
        Jt.text("This image is loaded directly from a base64 string:").use();
        Jt.imageFromBase64(base64Png)
                .caption("A red square loaded from base64")
                .use();

        // Example with data URI prefix (also supported)
        String base64WithPrefix = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAAFklEQVR42mNk+M/wnwEJMDIyMgxgjgIAXjAJAZBRxZYAAAAASUVORK5CYII=";
        Jt.text("This one includes the data URI prefix:").use();
        Jt.imageFromBase64(base64WithPrefix)
                .caption("A green square with data URI prefix")
                .use();
    }
}
