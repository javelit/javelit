///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.52.0


import io.javelit.core.Jt;

public class CodeExample {
    public static void main(String[] args) {
        Jt.title("# Code Component Showcase").use();
        Jt.markdown("This demo showcases all the capabilities of the `st.code` component in Javelit.").use();

        // Language highlighting examples
        Jt.title("## Language Highlighting").use();

        Jt.markdown("### Java Code (Default Language)").use();
        Jt.code("""
                public class HelloWorld { 
                    private static final String GREETING = "Hello, World!";
                
                    public static void main(String[] args) {
                        System.out.println(GREETING);
                
                        // Create a simple list
                        List<String> names = Arrays.asList("Alice", "Bob", "Charlie");
                        names.stream()
                            .map(name -> "Hello, " + name + "!")
                            .forEach(System.out::println);
                    }
                }
                """).use();

        Jt.markdown("### Python Code").use();
        Jt.code("""
                def fibonacci(n):
                    \"\"\"Generate Fibonacci sequence up to n terms\"\"\"
                    if n <= 0:
                        return []
                    elif n == 1:
                        return [0]
                    elif n == 2:
                        return [0, 1]
                
                    sequence = [0, 1]
                    for i in range(2, n):
                        sequence.append(sequence[i-1] + sequence[i-2])
                
                    return sequence
                
                # Generate first 10 Fibonacci numbers
                fib_numbers = fibonacci(10)
                print(f"First 10 Fibonacci numbers: {fib_numbers}")
                
                # List comprehension example
                squares = [x**2 for x in range(1, 11) if x % 2 == 0]
                print(f"Squares of even numbers: {squares}")
                """).language("python").use();

        Jt.markdown("### Plain Text (No Highlighting)").use();
        Jt.code("""
                This is plain text without any syntax highlighting.
                No keywords, strings, or comments will be colored.
                
                function example() {
                    // This looks like JavaScript but won't be highlighted
                    return "plain text";
                }
                
                SELECT * FROM users WHERE active = true;
                -- This looks like SQL but won't be highlighted either
                """).language(null).use();

        // Line numbers and wrap lines combinations
        Jt.title("## Line Numbers & Wrap Lines Combinations").use();

        Jt.markdown("### Default: No Line Numbers, No Wrapping").use();
        Jt.code("""
                // Short lines that don't need wrapping
                public void shortMethod() {
                    String name = "Alice";
                    int age = 25;
                    System.out.println("Name: " + name + ", Age: " + age);
                }
                """).use();

        Jt.markdown("### Line Numbers Only").use();
        Jt.code("""
                public class Calculator {
                    public int add(int a, int b) {
                        return a + b;
                    }
                
                    public int multiply(int a, int b) {
                        return a * b;
                    }
                
                    public double divide(int a, int b) {
                        if (b == 0) throw new IllegalArgumentException("Division by zero");
                        return (double) a / b;
                    }
                }
                """).lineNumbers(true).use();

        Jt.markdown("### Wrap Lines Only (Long Lines)").use();
        Jt.code("""
                // This is a very long comment that demonstrates how line wrapping works when enabled. It will wrap to multiple lines instead of creating a horizontal scroll bar.
                public void processUserData(String firstName, String lastName, String email, String phoneNumber, String address, String city, String country) {
                    String fullName = firstName + " " + lastName;
                    System.out.println("Processing user: " + fullName + " with email: " + email + " and phone: " + phoneNumber + " living at: " + address + ", " + city + ", " + country);
                
                    // Another long line that will wrap when wrap lines is enabled, showing how code remains readable even with very long statements or expressions
                    boolean isValidUser = !firstName.isEmpty() && !lastName.isEmpty() && email.contains("@") && phoneNumber.matches("\\\\d{10}") && !address.isEmpty() && !city.isEmpty() && !country.isEmpty();
                }
                """).wrapLines(true).use();

        Jt.markdown("### Both Line Numbers and Wrap Lines").use();
        Jt.code("""
                /**
                 * This method demonstrates both line numbers and line wrapping functionality together, which is particularly useful for long code blocks with extended lines
                 */
                public class DataProcessor {
                    public void processComplexData(Map<String, List<UserProfile>> userGroups, Set<String> activeRegions, List<String> supportedLanguages) {
                        // This long line will wrap and show line numbers, making it easy to reference specific parts of the code during code reviews or debugging sessions
                        userGroups.entrySet().stream()
                            .filter(entry -> activeRegions.contains(entry.getKey()))
                            .flatMap(entry -> entry.getValue().stream())
                            .filter(user -> supportedLanguages.contains(user.getPreferredLanguage()) && user.isActive() && user.hasValidSubscription())
                            .collect(Collectors.groupingBy(UserProfile::getRegion, Collectors.mapping(UserProfile::getEmail, Collectors.toList())))
                            .forEach((region, emails) -> sendNotificationToUsers(region, emails, "Welcome to our premium service!"));
                    }
                }
                """).lineNumbers(true).wrapLines(true).use();

        // Width and height examples
        Jt.title("## Width and Height Control").use();

        Jt.markdown("### Default Dimensions (Stretch Width, Content Height)").use();
        Jt.code("""
                // This code block uses default dimensions
                public void defaultSize() {
                    System.out.println("Default width stretches to container");
                    System.out.println("Height adjusts to content");
                }
                """).use();

        Jt.markdown("### Fixed Pixel Dimensions").use();
        Jt.code("""
                // This code block has fixed width and height
                public void fixedDimensions() {
                    String[] languages = {"Java", "Python", "JavaScript", "Go", "Rust"};
                    for (String lang : languages) {
                        System.out.println("Language: " + lang);
                    }
                
                    // More content to show scrolling behavior
                    System.out.println("This content might require scrolling");
                    System.out.println("if the fixed height is smaller");
                    System.out.println("than the content height.");
                }
                """).width(600).height(150).lineNumbers(true).use();

        Jt.markdown("### Content Width (Fits Code Width)").use();
        Jt.code("short code").width("content").lineNumbers(true).use();

        Jt.markdown("### Stretch Height (Fills Available Space)").use();
        final var cs = Jt.container().height(500).use();
        Jt.code("""
                // This code block stretches to available height
                public class StretchExample {
                    public void method1() {
                        System.out.println("Method 1");
                    }
                
                    public void method2() {
                        System.out.println("Method 2");
                    }
                }
                """).height("stretch").lineNumbers(true).use(cs);

        // Language variety showcase
        Jt.title("## Different Programming Languages").use();

        Jt.markdown("### JavaScript").use();
        Jt.code("""
                const fetchUserData = async (userId) => {
                    try {
                        const response = await fetch(`/api/users/${userId}`);
                        if (!response.ok) {
                            throw new Error(`HTTP error! status: ${response.status}`);
                        }
                        const userData = await response.json();
                        return userData;
                    } catch (error) {
                        console.error('Failed to fetch user data:', error);
                        throw error;
                    }
                };
                
                // Usage with arrow function and destructuring
                fetchUserData(123).then(({name, email, profile}) => {
                    console.log(`User: ${name} (${email})`);
                    console.log(`Profile: ${JSON.stringify(profile, null, 2)}`);
                });
                """).language("javascript").lineNumbers(true).use();

        Jt.code("""
                const fetchUserData = async (userId) => {
                    try {
                        const response = await fetch(`/api/users/${userId}`);
                        if (!response.ok) {
                            throw new Error(`HTTP error! status: ${response.status}`);
                        }
                        const userData = await response.json();
                        return userData;
                    ile: ${JSON.stringify(profile, null, 2)}`);
                });
                """).language("javascript").lineNumbers(true).use();

        Jt.markdown("### CSS").use();
        Jt.code("""
                /* Modern CSS with custom properties and grid */
                :root {
                    --primary-color: #3498db;
                    --secondary-color: #2c3e50;
                    --border-radius: 8px;
                    --spacing: 1rem;
                }
                
                .card {
                    background: linear-gradient(135deg, var(--primary-color), var(--secondary-color));
                    border-radius: var(--border-radius);
                    padding: var(--spacing);
                    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
                    transition: transform 0.2s ease-in-out;
                }
                
                .card:hover {
                    transform: translateY(-2px);
                }
                
                .grid-container {
                    display: grid;
                    grid-template-columns: repeat(auto-fit, minmax(300px, 1fr));
                    gap: var(--spacing);
                }
                """).language("css").lineNumbers(true).wrapLines(true).use();

        Jt.markdown("### JSON Configuration").use();
        Jt.code("""
                {
                  "name": "javelit-demo",
                  "version": "1.0.0",
                  "description": "A comprehensive demo of code component capabilities",
                  "dependencies": {
                    "spring-boot": "3.2.0",
                    "jackson": "2.15.2",
                    "junit": "5.9.3"
                  },
                  "configuration": {
                    "server": {
                      "port": 8080,
                      "ssl": {
                        "enabled": false,
                        "key-store": "/path/to/keystore.p12"
                      }
                    },
                    "logging": {
                      "level": {
                        "io.javelit": "DEBUG",
                        "org.springframework": "INFO"
                      }
                    }
                  },
                  "features": [
                    "syntax-highlighting",
                    "line-numbers",
                    "code-wrapping",
                    "responsive-design"
                  ]
                }
                """).language("json").lineNumbers(true).use();

        Jt.markdown("---").use();
        Jt
                .markdown(
                        "**Demo Complete!** This showcase demonstrates all the key features of the CodeComponent including syntax highlighting for multiple languages, line numbers, line wrapping, and flexible width/height controls.")
                .use();
    }
}
