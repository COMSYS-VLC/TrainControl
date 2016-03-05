package com.example.michael.traincontrol;

/**
 * Created by Michael on 02.03.2016.
 * Describes a controllable object. Either a track signal or a turnout.
 */
public class ControllableObject {
    // Which color does the controllable object have? Indicated in app and with same-colored lego stones on the track.
    int color;

    // What' the name of the controllable object? E.g. "turnout1" or "red signal".
    String name;

    /**
     * Constructor.
     * @param color The color of the controllable object.
     * @param name The name of the controllable object.
     */
    public ControllableObject(int color, String name) {
        this.color = color;
        this.name = name;
    }

    /**
     * Get color.
     * @return color.
     */
    public int getColor() {
        return this.color;
    }

    /**
     * Get name.
     * @return name.
     */
    public String getName() {
        return this.name;
    }

    /**
     * Set color.
     * @param color The color of the controllable object.
     */
    public void setColor(int color) {
        this.color = color;
    }

    /**
     * Set name.
     * @param name The name of the controllable object.
     */
    public void setName(String name) {
        this.name = name;
    }
}
