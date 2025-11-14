 import io.javelit.core.Jt;

 public class CacheApp {
      public static void main(String[] args) {
          String cacheKey = "long_running_operation";
          Long result = Jt.cache().getLong(cacheKey);

          if (result == null) {
              Jt.text("Performing a long running operation. This will take a few seconds").use();
              result = long_running_operation();
              Jt.cache().put(cacheKey, result);
          }

          Jt.text("Result of long operation: " + result).use();
          Jt.text("Refresh or Open the page in another tab: the long running operation result will be cached").use();
      }

      private static long long_running_operation() {
          try {
              Thread.sleep(5000);
          } catch (InterruptedException ignored) {
          }
          return 42;
      }
  }
