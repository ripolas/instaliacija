import Audio.AudioPlayer;
import Audio.AudioRecorder;

public class Main {
    public static AudioRecorder recorder;

    public static void main(String[] args) {
        /*
        recorder = new AudioRecorder();
        recorder.start("test2.mp3");
        try {
            Thread.sleep(10000);
        } catch (Exception ignored) {
        }
        recorder.stop();
        */
        AudioPlayer player = new AudioPlayer("test2.mp3");
        player.play();
        try {
            Thread.sleep(10000);
        } catch (Exception ignored) {
        }
    }
}