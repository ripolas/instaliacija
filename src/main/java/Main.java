import Audio.AudioPlayer;
import Audio.AudioRecorder;
import Buttons.Button;
import Buttons.LightableButton;

public class Main {
    public static long index = 10000L;

    public static AudioRecorder recorder;
    public static AudioPlayer player;

    public static LightableButton recordButton = new LightableButton();
    public static LightableButton playButton = new LightableButton();

    public static Runnable getDefaultRecordAction(){
        return () -> {
            playButton.clearNextAction();
            recordButton.setNextAction(() -> recorder.stop());
            recorder = new AudioRecorder("test" + index + ".wav");
            recorder.start();
        };
    }
    public static Runnable getDefaultPlayAction(){
        return () -> {

        };
    }
    public static void main(String[] args) {
        recordButton.setNextAction(getDefaultRecordAction());
        playButton.setNextAction(getDefaultPlayAction());


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
            Thread.sleep(10000);
        } catch (Exception ignored) {
        }
    }
}