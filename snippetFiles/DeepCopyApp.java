import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import io.javelit.core.Jt;

public class DeepCopyApp {
  public static void main(String[] args) {
    // init
    List<String> sharedList = (List<String>) Jt.cache().get("shared_list");
    if (sharedList == null) {
      sharedList = new ArrayList<>();
      sharedList.add("item1");
      sharedList.add("item2");
      Jt.cache().put("shared_list", sharedList);
    }

    // Create a safe copy to avoid mutations affecting other sessions
    List<String> safeCopy = Jt.deepCopy(sharedList, new TypeReference<>() {
    });

    if (Jt.button("remove elements from user lists").use()) {
      safeCopy.clear();
    }

    Jt.text("Original list size: " + sharedList.size()).use();
    Jt.text("Safe copy size: " + safeCopy.size()).use();
  }
}
