package com.example.michael.traincontrol;


/**
 * Created by Michael on 02.03.2016.
 * Describes a turnout.
 */
public class Turnout extends ControllableObject {
    // Is the turnout straight?
    boolean straight;

    /**
     * Constructor.
     * @param color The color of the turnout.
     * @param name The name of the turnout.
     * @param straight Is the turnout straight?
     */
    public Turnout(int color, String name, boolean straight) {
        super(color, name);
        this.straight = straight;
    }

    /**
     * Get straight.
     * @return Is the turnout straight (true) or diverging (false)?
     */
    public boolean getStraight() {
        return this.straight;
    }

    /**
     * Set straight.
     * @param straight True if the turnout is straight, false if it is diverging.
     */
    public void setStraight(boolean straight) {
        this.straight = straight;
    }
}
