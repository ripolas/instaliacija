import Audio.AudioPlayer;
import Audio.AudioRecorder;
import Buttons.Button;
import Buttons.LightableButton;

public class Main {
    public static AudioRecorder recorder;
    public static AudioPlayer player;

    public static LightableButton recordButton;
    public static LightableButton playButton;

    public static void main(String[] args) {
        recordButton.setOnClick(() -> {

        });


        /*recorder = new AudioRecorder("test2.mp3");
        recorder.start();
        try {
            Thread.sleep(10000);
        } catch (Exception ignored) {
        }
        recorder.stop();*/
        AudioPlayer player = new AudioPlayer("test2.mp3");
        player.play();
        try {
            Thread.sleep(2000);
        } catch (Exception ignored) {
        }
        player.stop();
    }
}