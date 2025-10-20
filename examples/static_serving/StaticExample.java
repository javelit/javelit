///usr/bin/env jbang "$0" "$@" ; exit $?
package static_serving;

import io.javelit.core.Jt;

public class StaticExample  {
    public static void main(String[] args) {
        Jt.text("Here is an image from the static folder").use();
        Jt.markdown("![an emoji](/app/static/emoji-mashup.png)").use();
    }
}
