package Audio;

import javax.sound.sampled.*;
import java.io.File;
import java.io.IOException;

public class AudioPlayer{

    private Long currentFrame;
    private final Clip clip;

    private final String filePath;

    private final int loopTimes;

    public AudioPlayer(String filePath) {
        this(filePath, 0);
    }
    public AudioPlayer(String filePath, int loopTimes) {
        this.filePath = filePath;
        this.loopTimes = loopTimes;
        try {
            clip = AudioSystem.getClip();

            resetAudioStream();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public boolean isPlaying(){
        return clip.isRunning();
    }

    public void play() {
        if(isPlaying()) {
            return;
        }
        if(clip.isOpen()) {
            try {
                clip.stop();
                clip.close();
                resetAudioStream();
                clip.setMicrosecondPosition(currentFrame);
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }
        clip.start();
    }

    public void pause()
    {
        if (!isPlaying()) {
            return;
        }
        this.currentFrame = this.clip.getMicrosecondPosition();
        clip.stop();
    }

    public void restart() {
        try {
            clip.stop();
            clip.close();
            resetAudioStream();
            clip.setMicrosecondPosition(0);
            this.play();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        currentFrame = 0L;
        clip.stop();
        clip.close();
    }

    // Method to jump over a specific part
    public void jump(long c) {
        if (c > 0 && c < clip.getMicrosecondLength()) {
            try {
                clip.stop();
                clip.close();
                resetAudioStream();
                currentFrame = c;
                clip.setMicrosecondPosition(c);
                this.play();
            }catch (Exception e){
                throw new RuntimeException(e);
            }
        }
    }

    public long getMicrosecondLength(){
        return clip.getMicrosecondLength();
    }

    private void resetAudioStream() throws Exception {
        if(clip.isOpen()){
            clip.close();
        }
        currentFrame = 0L;
        AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(new File(filePath).getAbsoluteFile());
        clip.open(audioInputStream);
        clip.loop(loopTimes);
    }
}