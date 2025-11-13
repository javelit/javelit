
///usr/bin/env jbang "$0" "$@" ; exit $?
//DEPS io.javelit:javelit:0.67.0

import io.javelit.core.Jt;

public class AudioInputExample {

    public static void main(String[] args) {
        var res1 = Jt.audioInput("Say something").sampleRate(48000).use();
        var res2 = Jt.audioInput("Say something").use();
        if (res1 != null) {
            Jt.text("Play it later!").use();
            Jt.audio(res1).use();
        }
        if (res2 != null) {
            Jt.text("Play it later! low qual").use();
            Jt.audio(res2).use();
        }
    }
}
