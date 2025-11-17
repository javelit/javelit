
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.71.0

import io.javelit.core.Jt;

public class AudioInputExample {

    public static void main(String[] args) {
        var recording1 = Jt.audioInput("Record a message").use();
        if (recording1 != null) {
            Jt.text("Play it later!").use();
            Jt.audio(recording1).use();
        }

        var recording2 = Jt.audioInput("Record high quality audio").sampleRate(48000).use();
        if (recording2 != null) {
            Jt.text("Play it later in high res!").use();
            Jt.audio(recording2).use();
        }
    }
}
