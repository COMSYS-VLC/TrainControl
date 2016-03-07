package com.example.michael.traincontrol;


/**
 * Created by Michael on 02.03.2016.
 * Describes a track signal.
 */
public class TrackSignal extends ControllableObject {
    // What speed should the track signal show?
    int speed;
    // Is the train going forwards or backwards?
    boolean forward;

    /**
     * Constructor.
     * @param id The id of the track signal.
     * @param color The color of the track signal.
     * @param name The name of the track signal.
     * @param speed The speed which the track signal shows.
     * @param forward If the train goes forwards or backwards
     */
    public TrackSignal(byte id, int color, String name, int speed, boolean forward) {
        super(id, color, name);
        this.speed = speed;
        this.forward = forward;
    }

    /**
     * Get speed.
     * @return speed.
     */
    public int getSpeed() {
        return this.speed;
    }

    /**
     * Get forward.
     * @return true if train is moving forward, false if train is moving backwards.
     */
    public boolean getForward() {
        return this.forward;
    }

    /**
     * Set speed.
     * @param speed The speed to which the track signal is set.
     */
    public void setSpeed(int speed) {
        this.speed = speed;
        this.userInputEvent.userInputOccurred(this);
    }

    /**
     * Set forward.
     * @param forward Should the train move forwards?
     */
    public void setForward(boolean forward) {
        this.forward = forward;
        this.userInputEvent.userInputOccurred(this);
    }
}
