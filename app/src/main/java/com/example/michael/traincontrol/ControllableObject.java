package com.example.michael.traincontrol;

import java.util.EventListener;

/**
 * Created by Michael on 02.03.2016.
 * Describes a controllable object. Either a track signal or a turnout.
 */
class ControllableObject {
    // The id of the controllable object.
    private byte id;

    // Which color does the controllable object have? Indicated in app and with same-colored lego stones on the track.
    private int color;

    // What' the name of the controllable object? E.g. "turnout1" or "red signal".
    private String name;

    public interface IUserInputEvent extends EventListener {
        public void userInputOccurred(ControllableObject controllableObject);
    }
    IUserInputEvent userInputEvent;

    /**
     * Constructor.
     * @param id The id of the controllable object.
     * @param color The color of the controllable object.
     * @param name The name of the controllable object.
     */
    ControllableObject(byte id, int color, String name) {
        this.id = id;
        this.color = color;
        this.name = name;
        this.userInputEvent = null;
    }

    /**
     * GEt id.
     * @return id.
     */
    public byte getId() {
        return this.id;
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
     * Set id.
     * @param id id.
     */
    public void setId(byte id) {
        this.id = id;
        this.userInputEvent.userInputOccurred(this);
    }

    /**
     * Set color.
     * @param color The color of the controllable object.
     */
    public void setColor(int color) {
        // TODO: rausnehmen.
        this.color = color;
        this.userInputEvent.userInputOccurred(this);
    }

    /**
     * Set name.
     * @param name The name of the controllable object.
     */
    public void setName(String name) {
        this.name = name;
        this.userInputEvent.userInputOccurred(this);
    }

    public void setUserInputEvent(IUserInputEvent userInputEvent) {
        this.userInputEvent = userInputEvent;
    }
}
