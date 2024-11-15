package Buttons;

public abstract class Button {
    private Runnable onClick = () -> {};
    private Runnable onRelease = () -> {};
    private boolean clicked = false;

    public void setOnClick(Runnable runnable){
        onClick = runnable;
    }
    public void setOnRelease(Runnable runnable){
        onRelease = runnable;
    }

    public void click(){
        clicked = true;
        onClick.run();
    }
    public void release(){
        clicked = false;
        onRelease.run();
    }
    public void toggleClick(){
        if(isClicked()){
            release();
        }else{
            click();
        }
    }
    public boolean isClicked(){
        return clicked;
    }
}
