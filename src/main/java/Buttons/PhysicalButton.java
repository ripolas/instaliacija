package Buttons;

import java.util.concurrent.CountDownLatch;

public class PhysicalButton {
    private Runnable onPress = () -> {};
    private Runnable onRelease = () -> {};
    public boolean pressed = false;
    //private CountDownLatch releaseLatch = new CountDownLatch(0);
    //private CountDownLatch pressLatch = new CountDownLatch(1);

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
            if(!isPressed()) {
                //releaseLatch.await();
                //pressLatch = new CountDownLatch(0);
                //releaseLatch = new CountDownLatch(1);
                pressed = true;
                onPress.run();
            }
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public void release(){
        try{
            if(isPressed()) {
                //pressLatch.await();
                //releaseLatch = new CountDownLatch(0);
                //pressLatch = new CountDownLatch(1);
                pressed = false;
                onRelease.run();
            }
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
        return pressed;
    }
}
