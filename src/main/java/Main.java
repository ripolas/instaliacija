import Audio.AudioPlayer;
import Audio.AudioRecorder;
import Buttons.LightableButton;
import com.fazecast.jSerialComm.SerialPort;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static long index = 10000L;

    public static AudioRecorder recorder;
    public static AudioPlayer player;

    public static final long delay = 3000L;
    public static final long minimumRecording = 3000L;
    public static final long maximumRecording = 5000L;

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
        recordButton.setOnPress(getDefaultRecordAction());
        playButton.setOnPress(getDefaultPlayAction());
    }
    public static void executeDelay(){
        clearNextActions();
        CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(delay);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            setDefaultActions();
            setLit(true);
        });
    }

    public static Runnable getDefaultRecordAction(){
        return () -> {
            turnOffButtons();
            recorder = new AudioRecorder(constructPath(index));
            index ++;
            long startTime = System.currentTimeMillis();
            recorder.start();
            AtomicBoolean running = new AtomicBoolean(true);
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(maximumRecording);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if(running.get()){
                    System.out.println("Recording trimmed due to being to long!");
                    recordButton.release();
                }
            });
            recordButton.setNextAction(() -> {
                recordButton.clearOnRelease();
                running.set(false);
                turnOffButtons();
                long dif = System.currentTimeMillis() - startTime;
                recorder.stop();
                CompletableFuture.runAsync(() -> {
                    if(dif < minimumRecording){
                        System.out.println("Recording was deleted due to being too short!");
                         File file = new File(recorder.getPath());
                         if(file.exists()){
                             file.delete();
                         }
                    }
                    executeDelay();
                });
            });
        };
    }
    public static Runnable getDefaultPlayAction(){
        return () -> {
            turnOffButtons();
            player = new AudioPlayer(constructPath(index-1L));
            player.play();
            CompletableFuture.runAsync(() -> {
                long length = player.getMicrosecondLength() / 1000L + 1L;
                try {
                    Thread.sleep(length);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                player.stop();
                executeDelay();
            });
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
        /*
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
        }*/
        SerialPort serialPort = SerialPort.getCommPort("COM6");
        serialPort.setComPortParameters(9600, 8, SerialPort.ONE_STOP_BIT, SerialPort.NO_PARITY);
        serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);

        if (serialPort.openPort()) {
            System.out.println("Port opened successfully!");
            try {
                InputStream inputStream = serialPort.getInputStream();
                OutputStream outputStream = serialPort.getOutputStream();
                boolean cachedRecordPressed = false;
                boolean cachedPlayPressed = false;
                while (true) {
                    inputStream.skip(inputStream.available());
                    int lastByte = inputStream.read();
                    int receivedByte = lastByte & 0xFF;
                    boolean recordPressed = (receivedByte / 2 == 1);
                    boolean playPressed = (receivedByte % 2 == 1);
                    if(recordPressed != cachedRecordPressed){
                        cachedRecordPressed = recordPressed;
                        if(recordPressed){
                            System.out.println("Record pressed!");
                            recordButton.press();
                        }else{
                            System.out.println("Record released!");
                            recordButton.release();
                        }
                    }

                    if(playPressed != cachedPlayPressed){
                        cachedPlayPressed = playPressed;
                        if(playPressed){
                            System.out.println("Play pressed!");
                            playButton.press();
                        }else{
                            System.out.println("Play released!");
                            playButton.release();
                        }
                    }

                    int recordLit = recordButton.isLit() ? 1 : 0;
                    int playLit = playButton.isLit() ? 1 : 0;

                    outputStream.write(recordLit * 2 + playLit);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            } finally {
                serialPort.closePort();
                System.out.println("Port closed.");
            }
        } else {
            System.out.println("Failed to open port.");
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