import Audio.AudioPlayer;
import Audio.AudioRecorder;
import Buttons.LightableButton;
import com.diozero.devices.Button;

import java.io.File;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class Main {
    public static long index = 10000L;

    public static AudioRecorder recorder;
    public static AudioPlayer player;

    public static final long delay = 1000L;

    public static LightableButton recordButton = new LightableButton();
    public static LightableButton playButton = new LightableButton();

    public static String constructPath(long fileIndex){
        return "test" + fileIndex + ".wav";
    }
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
            recorder = new AudioRecorder(constructPath(index));
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
            player = new AudioPlayer(constructPath(index-1));
            player.play();
            long length = player.getMicrosecondLength() / 1000L + 1L;
            try {
                Thread.sleep(length);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            executeDelay();
        };
    }
    public static void main(String[] args) {
        turnOffButtons();
        for(long i=0;; i++){
            long newIndex = index + i;
            File file = new File(constructPath(newIndex));
            if(!file.exists()){
                index = newIndex;
                break;
            }
        }
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
        try (Button button = new Button(16)) {
            button.whenPressed(n -> {
                System.out.println("PRESSED2");
                playButton.press();
            });
            button.whenReleased(n -> {
                System.out.println("RELEASED2");
                playButton.release();
            });
        }
        for(int i=2; i<=27; i++) {
            try (Button button = new Button(i)) {
                int finalI = i;
                button.whenPressed(n -> {
                    System.out.println("PRESSED(" + finalI + ")");
                    playButton.press();
                });
                button.whenReleased(n -> {
                    System.out.println("RELEASED(" + finalI + ")");
                    playButton.release();
                });
            }
        }
        try {
            Thread.currentThread().join();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
        /*CompletableFuture.runAsync(() -> {
           while(true){
               try {
                   Thread.sleep(3000L);
               }catch (Exception e){

               }
               System.out.println((recordButton.isLit() ? "X" : ".") + " " + (playButton.isLit() ? "X" : "."));
               System.out.println((recordButton.isPressed() ? "1" : "0") + " " + (playButton.isPressed() ? "1" : "0"));
               System.out.println();
           }
        });
        Scanner scanner = new Scanner(System.in);
        while(true){
            int in = scanner.nextInt();
            if(in == 0){
                recordButton.togglePress();
            }else{
                playButton.togglePress();
            }
        }*/
    }
}