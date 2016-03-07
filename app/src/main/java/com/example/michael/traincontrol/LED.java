package com.example.michael.traincontrol;

/**
 * Created by Michael on 04.03.2016.
 * Describes an LED.
 */
public class LED extends ControllableObject {
    enum LedState {OFF, BLINKING, ON};
    private LedState ledstate;

    /**
     * Constructor.
     * @param id The id of the LED.
     * @param color The color of the LED.
     * @param name The name of the LED.
     * @param ledState The state of the LED.
     */
    public LED(byte id, int color, String name, LedState ledState) {
        super(id, color, name);
        this.ledstate = ledState;
    }

    /**
     * Getter for LED state.
     * @return LED state.
     */
    public LedState getLedState() {
        return this.ledstate;
    }

    /**
     * Return the LED state spinner index.
     * @return 0 (OFF), 1 (BLINKING), 2 (ON).
     */
    public int getLedStateSpinnerIndex() {
        switch (this.ledstate) {
            case OFF:
                return 0;
            case BLINKING:
                return 1;
            case ON:
                return 2;
            default:
                return 0;
        }
    }

    /**
     * Setter for LED state.
     * @param ledState LED state.
     */
    public void setLedState(LedState ledState) {
        this.ledstate = ledState;
        this.userInputEvent.userInputOccurred(this);
    }

    /**
     * Set the led state with a given spinner index.
     * @param index Spinner index.
     */
    public void setLedStateSpinnerIndex(int index) {
        switch (index) {
            case 0:
                this.ledstate = LedState.OFF;
                break;
            case 1:
                this.ledstate = LedState.BLINKING;
                break;
            case 2:
                this.ledstate = LedState.ON;
                break;
            default:
                break;
        }
        this.userInputEvent.userInputOccurred(this);
    }
}
