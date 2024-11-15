package Buttons;

public class LightableButton extends PhysicalButton {
    private boolean lit = false;

    public boolean isLit(){
        return lit;
    }
    public void setLit(boolean lit){
        this.lit = lit;
    }
    public boolean toggleLit(){
        return (lit = !lit);
    }
}
