import Audio.AudioPlayer;
import Audio.AudioRecorder;
import Buttons.LightableButton;
import com.fazecast.jSerialComm.SerialPort;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static long index = 10000L;

    public static final long delay = 3000L;
    public static final long minimumRecording = 3000L; //Minimum recording length in milliseconds
    public static final long maximumRecording = 60000L; //Maximum recording length in milliseconds

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
        setLit(true); //Lights up the lights
        recordButton.setOnPress(getDefaultRecordAction());
        recordButton.clearOnRelease();
        playButton.setOnPress(getDefaultPlayAction());
        playButton.clearOnRelease();
    }
    public static void executeDelay(){
        turnOffButtons(); // Turns off buttons before the delay
        CompletableFuture.runAsync(() -> { //Runs the delay in a CompletableFuture to avoid blocking the main thread
            try {
                Thread.sleep(delay); // Delay
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            setDefaultActions(); // Turns on the buttons after the delay
        });
    }

    public static Runnable getDefaultRecordAction(){
        return () -> {
            turnOffButtons();
            AudioRecorder recorder = new AudioRecorder(constructPath(index)); //Creates a recorder
            index ++; //Increase the timer by one for the next file
            long startTime = System.currentTimeMillis(); //Saves the time when the recording started
            recorder.start(); //Starts recording
            recordButton.setLit(true); //Lights up the record button to indicate recording
            AtomicBoolean running = new AtomicBoolean(true);
            CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(maximumRecording); //Sleeps off-thread
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                if(running.get()){ //Checks if it is still recording
                    System.out.println("Recording trimmed due to being to long!");
                    recordButton.release(); //Forcefully releases the record button if the recording is too long
                }
            });
            recordButton.setNextAction(() -> { //Sets action to do on release
                recordButton.clearOnRelease(); // Clears action in case the button was released artificially and not physically
                running.set(false); //Indicates that the recording stopped
                turnOffButtons(); //Turns off buttons for delay
                long dif = System.currentTimeMillis() - startTime; //Checks the length of the recording
                recorder.stop(); //Stops recording
                CompletableFuture.runAsync(() -> {
                    if(dif < minimumRecording){ //Checks if the recording is under minimum length
                        System.out.println("Recording was deleted due to being too short!");
                        File file = new File(recorder.getPath());
                        if(file.exists()){ //Checks if the file exists (it might not create a file if the recording is too short)
                            file.delete();
                        }
                        index--; //Deducts index because the file wasn't saved
                    }
                    executeDelay();
                });
            });
        };
    }
    public static Runnable getDefaultPlayAction(){
        return () -> {
            turnOffButtons();
            long indexToPlay = index-1;
            AudioPlayer player = new AudioPlayer(constructPath(indexToPlay)); //Creates a player
            player.play(); //Starts playing
            playButton.setLit(true); //Lights up the button to indicate playing
            CompletableFuture.runAsync(() -> {
                long length = player.getMicrosecondLength() / 1000L + 1L; //Gets the length of the recording
                try {
                    Thread.sleep(length);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                player.stop(); //Stops playing (I'm not sure if it doesn't stop playing automatically)
                executeDelay();
            });
        };
    }

    public static void main(String[] args) {
        turnOffButtons(); //turn off buttons while searching for indices

        // loops files and sets the index to the first one missing
        for(long i=0; ; i++){
            long newIndex = index + i;
            File file = new File(constructPath(newIndex));
            if(!file.exists()){
                index = newIndex;
                break;
            }
        }

        setDefaultActions(); //Turns on the buttons
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
        /*
        final String port = "COM6";
        SerialPort serialPort = SerialPort.getCommPort(port);
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
                    inputStream.skip(inputStream.available()); //makes sure to read the last byte
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
         */
        CompletableFuture.runAsync(() -> {
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
        }
    }
}