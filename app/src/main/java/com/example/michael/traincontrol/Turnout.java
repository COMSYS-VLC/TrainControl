package com.example.michael.traincontrol;


/**
 * Created by Michael on 02.03.2016.
 * Describes a turnout.
 */
class Turnout extends ControllableObject {
    // Is the turnout straight?
    private boolean straight;

    /**
     * Constructor.
     * @param id The id of the turnout.
     * @param color The color of the turnout.
     * @param name The name of the turnout.
     * @param straight Is the turnout straight?
     */
    public Turnout(byte id, int color, String name, boolean straight) {
        super(id, color, name);
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
        this.userInputEvent.userInputOccurred(this);
    }

    /**
     * Set straight without invocation.
     * @param straight True if the turnout is straight, false if it is diverging.
     */
    public void setSilentStraight(boolean straight) {
        this.straight = straight;
    }
}
