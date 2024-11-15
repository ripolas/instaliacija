import Audio.AudioPlayer;
import Audio.AudioRecorder;
import Buttons.LightableButton;
import com.diozero.devices.Button;

public class Main {
    public static long index = 10000L;

    public static AudioRecorder recorder;
    public static AudioPlayer player;

    public static final long delay = 5000L;

    public static LightableButton recordButton = new LightableButton();
    public static LightableButton playButton = new LightableButton();

    public static void turnOff(LightableButton button){
        button.clearNextAction();
        button.setLit(false);
    }
    public static void turnOffButtons(){
        turnOff(recordButton);
        turnOff(playButton);
    }
    public static void setLit(boolean lit){
        recordButton.setLit(lit);
        playButton.setLit(lit);
    }
    public static void clearNextActions(){
        recordButton.clearNextAction();
        playButton.clearNextAction();
    }
    public static void setDefaultActions(){
        recordButton.setNextAction(getDefaultRecordAction());
        playButton.setNextAction(getDefaultPlayAction());
    }
    public static void executeDelay(){
        clearNextActions();
        try {
            Thread.sleep(delay);
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        setDefaultActions();
        setLit(true);
    }

    public static Runnable getDefaultRecordAction(){
        return () -> {
            turnOffButtons();
            recorder = new AudioRecorder("test" + index + ".wav");
            index ++;
            recorder.start();
            recordButton.setNextAction(() -> {
                recorder.stop();
                executeDelay();
            });
        };
    }
    public static Runnable getDefaultPlayAction(){
        return () -> {
            turnOffButtons();
            player = new AudioPlayer("test" + (index-1) + ".wav");
            player.play();
            playButton.setNextAction(() -> {
                player.stop();
                executeDelay();
            });
        };
    }
    public static void main(String[] args) {
        setDefaultActions();
        setLit(true);
        try (Button button = new Button(23)) {
            button.whenPressed(n -> {
                System.out.println("PRESSED1");
                recordButton.press();
            });
            button.whenReleased(n -> {
                System.out.println("RELEASED1");
                recordButton.release();
            });
        }
        try (Button button = new Button(24)) {
            button.whenPressed(n -> {
                System.out.println("PRESSED2");
                playButton.press();
            });
            button.whenReleased(n -> {
                System.out.println("RELEASED2");
                playButton.release();
            });
        }
        try {
            Thread.currentThread().join();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        /*Scanner scanner = new Scanner(System.in);
        while(true){
            int in = scanner.nextInt();
            if(in == 1){
                recordButton.press();
            }else{
                playButton.press();
            }
        }*/
    }
}