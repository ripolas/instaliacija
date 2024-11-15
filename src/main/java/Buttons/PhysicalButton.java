package Buttons;

import java.util.concurrent.CountDownLatch;

public class PhysicalButton {
    private Runnable onPress = () -> {};
    private Runnable onRelease = () -> {};
    private CountDownLatch releaseLatch = new CountDownLatch(0);
    private CountDownLatch pressLatch = new CountDownLatch(1);

    public void setOnPress(Runnable runnable){
        onPress = runnable;
    }
    public void setOnRelease(Runnable runnable){
        onRelease = runnable;
    }
    public void setNextAction(Runnable runnable){
        if(isPressed()){
            onRelease = runnable;
        }else{
            onPress = runnable;
        }
    }

    public void clearOnPress(){
        onPress = () -> {};
    }
    public void clearOnRelease(){
        onRelease = () -> {};
    }
    public void clearNextAction(){
        setNextAction(() -> {});
    }

    public void press(){
        try {
            releaseLatch.await();
            pressLatch = new CountDownLatch(1);
            onPress.run();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public void release(){
        try{
            pressLatch.await();
            releaseLatch = new CountDownLatch(1);
            onRelease.run();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public void togglePress(){
        if(isPressed()){
            release();
        }else{
            press();
        }
    }
    public boolean isPressed(){
        return releaseLatch.getCount() == 1;
    }
}
