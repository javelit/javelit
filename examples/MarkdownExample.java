/// usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.75.0

import io.javelit.core.Jt;

public class MarkdownExample {
  public static void main(String[] args) {
    Jt.title("Markdown Component Demo").use();

    Jt.markdown("""
                    This demo showcases the **MarkdownComponent** and all the styles applied in its CSS.
                    The component supports all standard markdown elements with beautiful styling.
                    """).help("This is a help tooltip for markdown content").use();

    // Headers showcase
    Jt.markdown("## Headers").use();
    Jt.markdown("""
                    # Header Level 1
                    ## Header Level 2  
                    ### Header Level 3
                    #### Header Level 4
                    ##### Header Level 5
                    ###### Header Level 6
                    """).use();

    // Text formatting showcase
    Jt.markdown("## Text Formatting").use();
    Jt.markdown("""
                    Here are various text formatting options:
                    
                    - **Bold text** using double asterisks
                    - *Italic text* using single asterisks
                    - `Inline code` using backticks
                    - Regular paragraph text with proper line height and spacing
                    
                    You can also combine **bold and *italic* text** for emphasis.
                    """).use();

    // Lists showcase
    Jt.markdown("## Lists").use();
    Jt.markdown("""
                    ### Unordered Lists
                    - First item
                    - Second item with longer text to show wrapping behavior
                    - Third item
                      - Nested item
                      - Another nested item
                    - Fourth item
                    
                    ### Ordered Lists
                    1. First numbered item
                    2. Second numbered item
                    3. Third numbered item
                       1. Nested numbered item
                       2. Another nested numbered item
                    4. Fourth numbered item
                    """).use();

    // Links showcase
    Jt.markdown("## Links").use();
    Jt.markdown("""
                    Here are some example links:
                    
                    - Visit [the example website](https://example.com) for a great example
                    - Check out [GitHub](https://github.com/javelit/javelit) for the code repository of this project
                    - Learn more about [Markdown](https://www.markdownguide.org/basic-syntax/)
                    """).use();

    // Code blocks showcase
    Jt.markdown("## Code Examples").use();
    Jt.markdown("""
                    ### Inline Code
                    Use the `Jt.markdown()` method to create markdown content.
                    
                    ### Code Blocks
                    Here's a Java example:
                    
                    ```java
                    public class HelloWorld {
                        public static void main(String[] args) {
                            System.out.println("Hello, World!");
                        }
                    }
                    ```
                    
                    And here's some Python code:
                    
                    ```python
                    def greet(name):
                        return f"Hello, {name}!"
                    
                    print(greet("Markdown"))
                    ```
                    """).use();

    // Blockquotes showcase
    Jt.markdown("## Blockquotes").use();
    Jt.markdown("""
                    Here's what Einstein said about simplicity:
                    
                    > Everything should be made as simple as possible, but not simpler.
                    > 
                    > This is a multi-line blockquote that demonstrates
                    > how quoted text is styled in the markdown component.
                    """).use();

    // Width demonstrations
    Jt.markdown("## Width Control").use();

    Jt.markdown("### Content Width").use();
    Jt
        .markdown("This markdown content uses **content** width - it takes only the space it needs.")
        .width("content")
        .use();

    Jt.markdown("### Fixed Width (400px)").use();
    Jt
        .markdown(
            "This markdown content is constrained to exactly **400 pixels** wide, which is useful for creating consistent layouts.")
        .width(400)
        .use();

    Jt.markdown("### Stretch Width (Default)").use();
    Jt
        .markdown(
            "This markdown content uses **stretch** width - it expands to fill the available container space, which is the default behavior.")
        .use();

    // Complex example
    Jt.markdown("## Complex Example").use();
    Jt.markdown("""
                    # API Documentation Example
                    
                    ## Overview
                    This is an example of how you might document an API using markdown.
                    
                    ### Authentication
                    All API requests require authentication using an API key:
                    
                    ```bash
                    curl -H "Authorization: Bearer YOUR_API_KEY" \\
                         -H "Content-Type: application/json" \\
                         https://api.example.com/users
                    ```
                    
                    ### Endpoints
                    
                    #### GET /users
                    Retrieve a list of users.
                    
                    **Parameters:**
                    - `page` (optional): Page number for pagination
                    - `limit` (optional): Number of results per page
                    
                    **Response:**
                    ```json
                    {
                      "users": [
                        {"id": 1, "name": "John Doe"},
                        {"id": 2, "name": "Jane Smith"}
                      ],
                      "pagination": {
                        "page": 1,
                        "total": 42
                      }
                    }
                    ```
                    
                    > **Note:** This endpoint requires the `read:users` scope.
                    
                    #### POST /users
                    Create a new user.
                    
                    **Request body:**
                    ```json
                    {
                      "name": "New User",
                      "email": "user@example.com"
                    }
                    ```
                    
                    ### Error Handling
                    
                    The API returns standard HTTP status codes:
                    
                    - `200` - Success
                    - `400` - Bad Request
                    - `401` - Unauthorized
                    - `404` - Not Found
                    - `500` - Internal Server Error
                    
                    For more information, visit our [documentation portal](https://docs.example.com).
                    """).width(600).help("This is a comprehensive example showing mixed markdown elements").use();

    Jt.markdown("---").use();
    Jt
        .markdown(
            "*This demo showcases all the styling features of the MarkdownComponent. Notice the consistent spacing, typography, and visual hierarchy throughout all examples.*")
        .use();
  }
}
