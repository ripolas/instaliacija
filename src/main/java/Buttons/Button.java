package Buttons;

import java.util.concurrent.CountDownLatch;

public abstract class Button {
    private Runnable onClick = () -> {};
    private Runnable onRelease = () -> {};
    private CountDownLatch releaseLatch = new CountDownLatch(0);
    private CountDownLatch clickLatch = new CountDownLatch(1);

    public void setOnClick(Runnable runnable){
        onClick = runnable;
    }
    public void setOnRelease(Runnable runnable){
        onRelease = runnable;
    }
    public void setNextAction(Runnable runnable){
        isClicked()
    }

    public void clearOnClick(){
        onClick = () -> {};
    }
    public void clearOnRelease(){
        onRelease = () -> {};
    }

    public void click(){
        try {
            releaseLatch.await();
            clickLatch = new CountDownLatch(1);
            onClick.run();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public void release(){
        try{
            clickLatch.await();
            releaseLatch = new CountDownLatch(1);
            onRelease.run();
        }catch (Exception e){
            throw new RuntimeException(e);
        }
    }
    public void toggleClick(){
        if(isClicked()){
            release();
        }else{
            click();
        }
    }
    public boolean isClicked(){
        return releaseLatch.getCount() == 1;
    }
}
