package Audio;

import javax.sound.sampled.*;
import java.io.File;
import java.util.concurrent.CompletableFuture;

public class AudioRecorder {

    private TargetDataLine microphone = null;
    private final String path;

    public AudioRecorder(String path){
        this.path = path;}

    private static AudioFormat getAudioFormat() {
        float sampleRate = 44100;
        int sampleSizeInBits = 16;
        int channels = 2;
        boolean signed = true;
        boolean bigEndian = false;
        return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian);
    }

    public void start() {
        AudioFormat format = getAudioFormat();
        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        if (!AudioSystem.isLineSupported(info)) {
            return;
        }
        try {
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);
            microphone.start();
            File file = new File(path);
            CompletableFuture.runAsync(() -> {
                try (AudioInputStream audioStream = new AudioInputStream(microphone)) {
                    AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, file);
                } catch (Exception ignored) {}
            });
        } catch (Exception ignored) {
        }
    }

    public void stop() {
        if (isRecording()) {
            microphone.stop();
            microphone.close();
        }
    }

    public boolean isRecording(){
        return microphone != null && microphone.isRunning();
    }
}
