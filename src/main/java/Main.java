import Audio.AudioPlayer;
import Audio.AudioRecorder;
import Buttons.LightableButton;
import com.fazecast.jSerialComm.SerialPort;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

public class Main {
    public static final boolean useArduino = true; //Uses console if false
    public static final String arduinoPort = "/dev/ttyACM0"; //Only used if useArduino is true
    //public static final String arduinoPort = "COM6"; // windows laptop
    public static final long startingIndex = 10000L; //The count from which the recordings start to get counted
    public static final long delay = 3000L; //Delay after finishing a recording / listening (DON'T SET UNDER 1000L! (file deletion delay))
    public static final long minimumRecording = 3000L; //Minimum recording length in milliseconds
    public static final long maximumRecording = 60000L; //Maximum recording length in milliseconds
    public static final long resetChance = -2L; //How many entries in the pool a recording should get after being played

    public static LightableButton recordButton = new LightableButton();
    public static LightableButton playButton = new LightableButton();

    public static long index = startingIndex;

    public static HashMap<Long, Long> chances = new HashMap<>();
    public static HashMap<Long, Long> getPositiveChances(){
        HashMap<Long, Long> positiveChances = new HashMap<>();
        for(Long key : chances.keySet()){
            Long value = chances.get(key);
            if(value > 0){
                positiveChances.put(key, value);
            }
        }
        return positiveChances;
    }
    public static long getResetChance(){
        return Math.max(resetChance, -(getPositiveChances().size() - 2));
    }

    public static final String tooShortSoundPath = "tooshort.wav"; //The file for the sound effect to be played when the recording doesn't get saved due to being too short
    public static final String tooLongSoundPath = "toolong.wav"; //The file for the sound effect to be played when the recording gets cut due to exceeding the time limit
    public static void tryToPlaySound(String path){
        if(new File(path).exists()){
//            new AudioPlayer(path); //Needs to create an audio player before playing or the first time the sound doesn't play
//            AudioPlayer player = new AudioPlayer(path);
//            player.play();
            try {
                // Replace "ls" with your desired Linux command
                Process process = Runtime.getRuntime().exec(new String[]{"bash","-c","cd instaliacija && applay "+path});
                //process = Runtime.getRuntime().exec(new String[]{"bash","/usr/bin/aplay "+path});
                //process = Runtime.getRuntime().exec(new String[]{"bash","-c","which aplay"});
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                String line;
                System.out.println("Standard Output:");
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }

                System.out.println("Error Output:");
                while ((line = errorReader.readLine()) != null) {
                    System.out.println(line);
                }
                process.waitFor(); // Wait for the command to complete
                System.out.println("Command executed successfully.");
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static final String statsPath = "stats.txt"; //The file where stats should be saved
    public static final long statsSaveInterval = 15000L; //Interval in which stats get saved
    public static final Stats stats = new File(statsPath).exists() ? Stats.loadFromFile(statsPath) : new Stats(); //Checks if a stats file exists, loads from there if it does, creates a new stats file if it doesn't

    public static class Stats{
        public static Stats loadFromFile(String path){
            try {
                File myObj = new File(path);
                Scanner myReader = new Scanner(myObj);
                long[] data = new long[4];
                for(int i=0; i<4; i++){
                    String line = myReader.nextLine();
                    String unparsedNum = line.split(":")[1];
                    data[i] = Long.parseLong(unparsedNum);
                }
                HashMap<Long, Long> readData = new HashMap<>();
                while(myReader.hasNextLine()){
                    String line = myReader.nextLine();
                    String[] split = line.split(":");
                    readData.put(Long.parseLong(split[0]), Long.parseLong(split[1]));
                }
                myReader.close();

                Stats stats = new Stats();
                stats.recordClicked = data[0];
                stats.recorded = data[1];
                stats.playClicked = data[2];
                stats.played = data[3];
                stats.indexToTimesPlayed = readData;
                return stats;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        public void saveToFile(String path){
            try {
                File file = new File(path);
                if(!file.exists()){
                    file.createNewFile();
                }

                StringBuilder builder = new StringBuilder(String.format(
                        "recordClicked:%s\nrecorded:%s\nplayClicked:%s\nplayed:%s",
                        recordClicked,
                        recorded,
                        playClicked,
                        played
                ));
                for(Long key : indexToTimesPlayed.keySet()){
                    Long value = indexToTimesPlayed.get(key);
                    builder.append("\n").append(key).append(":").append(value);
                }

                FileWriter myWriter = new FileWriter(path);
                myWriter.write(builder.toString());
                myWriter.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        public long recordClicked = 0L;
        public long recorded = 0L;

        public long playClicked = 0L;
        public long played = 0L;

        public HashMap<Long, Long> indexToTimesPlayed = new HashMap<>();
    }

    public static String constructPath(long fileIndex){
        String name = "Audio/mem" + fileIndex + ".wav";
        new File(name).getParentFile().mkdirs();
        return name;
    }
    public static void turnOff(LightableButton button){
        button.clearOnPress();
        button.clearOnRelease();
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
            String path = constructPath(index);
            AudioRecorder recorder = new AudioRecorder(path); //Creates a recorder
            long startTime = System.currentTimeMillis(); //Saves the time when the recording started
            System.out.println("Recording to \"" + path + "\"");
            recorder.start(); //Starts recording
            recordButton.setLit(true); //Lights up the record button to indicate recording
            AtomicBoolean running = new AtomicBoolean(true);
            AtomicBoolean playSound = new AtomicBoolean(false);
            Thread thread = new Thread(() -> {
                try {
                    Thread.sleep(maximumRecording); //Sleeps off-thread
                } catch (Exception ignored) {}
                if(running.get()){ //Checks if it is still recording
                    playSound.set(true);
                    System.out.println("Recording trimmed due to being too long!");
                    recordButton.release(); //Forcefully releases the record button if the recording is too long
                }
            });
            thread.start();
            recordButton.setNextAction(() -> { //Sets action to do on release
                thread.interrupt(); //Frees up a thread
                recordButton.clearOnRelease(); // Clears action in case the button was released artificially and not physically
                running.set(false); //Indicates that the recording stopped
                turnOffButtons(); //Turns off buttons for delay
                long dif = System.currentTimeMillis() - startTime; //Checks the length of the recording
                recorder.stop(); //Stops recording
                if(playSound.get()){
                    tryToPlaySound(tooLongSoundPath);
                }
                stats.recorded++; //Increments stats
                if(dif < minimumRecording){ //Checks if the recording is under minimum length
                    long start = System.currentTimeMillis();
                    tryToPlaySound(tooShortSoundPath);
                    System.out.println(System.currentTimeMillis() - start);
                    System.out.println("Recording was deleted due to being too short!");
                    CompletableFuture.runAsync(() -> {
                        try {
                            File file = new File(recorder.getPath());
                            Thread.sleep(1000L); // File gets written to after the recorder gets stopped for ??? reason
                            file.delete(); // Deletes the too short recording
                        }catch (Exception e){
                            throw new RuntimeException(e);
                        }
                    });
                }else{
                    chances.put(index, getResetChance());
                    index ++; //Increase the timer by one for the next file
                }
                executeDelay();
            });
        };
    }

    public static Runnable getDefaultPlayAction(){
        return () -> {
            turnOffButtons();

            //randomize index (trust the process)
            HashMap<Long, Long> positiveChances = getPositiveChances();
            chances.replaceAll((k, v) -> v + 1L); //Increments all elements by 1
            if(!positiveChances.isEmpty()) {
                ArrayList<Long> keySet = new ArrayList<>(positiveChances.keySet());
                ArrayList<Long> values = new ArrayList<>(positiveChances.values());
                int chanceSize = positiveChances.size();
                Long[] chance = new Long[chanceSize];
                long prevSum = 0;
                for (int i = 0; i < chanceSize; i++) {
                    prevSum += values.get(i);
                    chance[i] = prevSum;
                }
                long randomLong = new Random().nextLong(prevSum);
                int chanceIndex = 0;
                for (int i = chanceSize - 1; i > 0; i--) {
                    if (randomLong >= chance[i - 1]) {
                        chanceIndex = i;
                        break;
                    }
                }
                long indexToPlay = keySet.get(chanceIndex);
                chances.put(indexToPlay, getResetChance());


                String path = constructPath(indexToPlay);
                new AudioPlayer(path);
                AudioPlayer player = new AudioPlayer(path); //Creates a player
                System.out.println("Playing \"" + path + "\"");
                //player.play(); //Starts playing
                try {
                    // Replace "ls" with your desired Linux command
                    Process process = Runtime.getRuntime().exec(new String[]{"bash","-c","cd instaliacija && applay "+path});
                    //process = Runtime.getRuntime().exec(new String[]{"bash","/usr/bin/aplay "+path});
                    //process = Runtime.getRuntime().exec(new String[]{"bash","-c","which aplay"});
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));

                    String line;
                    System.out.println("Standard Output:");
                    while ((line = reader.readLine()) != null) {
                        System.out.println(line);
                    }

                    System.out.println("Error Output:");
                    while ((line = errorReader.readLine()) != null) {
                        System.out.println(line);
                    }
                    process.waitFor(); // Wait for the command to complete
                    System.out.println("Command executed successfully.");
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                playButton.setLit(true); //Lights up the button to indicate playing
                stats.played++; //updates stats
                stats.indexToTimesPlayed.put(indexToPlay, stats.indexToTimesPlayed.getOrDefault(indexToPlay, 0L) + 1L); //updates stats
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
            }else{
                System.out.println("No files to play!");
                executeDelay();
            }
        };
    }

    public static void main(String[] args) {
//        AudioPlayer player = new AudioPlayer("Audio/mem10000.wav");
//        player.play();
        turnOffButtons(); //turn off buttons while searching for indices

        // loops files and sets the index to the first one missing
        for(long i=0; ; i++){
            long newIndex = index + i;
            File file = new File(constructPath(newIndex));
            if(!file.exists()){
                index = newIndex;
                break;
            }
            chances.put(newIndex, 1L);
        }

        setDefaultActions(); //Turns on the buttons

        CompletableFuture.runAsync(() -> { //
            while(true){
                try {
                    Thread.sleep(statsSaveInterval);
                    stats.saveToFile(statsPath);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        });

        //true if physical buttons, false
        if(useArduino) {
            SerialPort serialPort = SerialPort.getCommPort(arduinoPort);
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
                        if (recordPressed != cachedRecordPressed) {
                            cachedRecordPressed = recordPressed;
                            if (recordPressed) {
                                stats.recordClicked++;
                                System.out.println("Record pressed!");
                                recordButton.press();
                            } else {
                                System.out.println("Record released!");
                                recordButton.release();
                            }
                        }

                        if (playPressed != cachedPlayPressed) {
                            cachedPlayPressed = playPressed;
                            if (playPressed) {
                                stats.playClicked++;
                                System.out.println("Play pressed!");
                                playButton.press();
                            } else {
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
        }else {
            CompletableFuture.runAsync(() -> {
                while (true) {
                    try {
                        Thread.sleep(3000L);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                    System.out.println((recordButton.isLit() ? "X" : ".") + " " + (playButton.isLit() ? "X" : "."));
                    System.out.println((recordButton.isPressed() ? "1" : "0") + " " + (playButton.isPressed() ? "1" : "0"));
                    System.out.println();
                }
            });
            Scanner scanner = new Scanner(System.in);
            while (true) {
                int in = scanner.nextInt();
                if (in == 0) {
                    recordButton.togglePress();
                    if(recordButton.isPressed()){
                        stats.recordClicked++;
                    }
                } else {
                    playButton.togglePress();
                    if(playButton.isPressed()){
                        stats.playClicked++;
                    }
                }
            }
        }
    }
}