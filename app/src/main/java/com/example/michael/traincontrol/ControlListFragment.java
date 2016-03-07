package com.example.michael.traincontrol;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import java.util.EventListener;


public class ControlListFragment extends Fragment implements ControllableObject.IUserInputEvent {

    public interface IControlListFragmentEvent extends EventListener {
        public void listFragmentEventOccurred(ControllableObject controllableObject);
    }
    IControlListFragmentEvent controlListFragmentEvent;

    // The list of objects (turnouts, tracksignals, LEDs) that are controllable.
    private final ControllableObject[] controllableObjects = new ControllableObject[]{
            new Turnout((byte)0x00, Color.rgb(255, 255, 0), "Turnout 1", true),
            new TrackSignal((byte)0x01, Color.rgb(255, 0, 0), "Red Signal", 0, true),
            new LED((byte)0x02, Color.rgb(0, 0, 255), "Left Front", LED.LedState.OFF),
            new LED((byte)0x03, Color.rgb(0, 0, 255), "Right Front", LED.LedState.BLINKING),
            new LED((byte)0x04, Color.rgb(255, 0, 0), "Left Back", LED.LedState.BLINKING),
            new LED((byte)0x05, Color.rgb(255, 0, 0), "Right Back", LED.LedState.ON)};

    private ListView mListView;

    public ControlListFragment() {
        // Required empty public constructor

        for (ControllableObject controllableObject : this.controllableObjects) {
            controllableObject.setUserInputEvent(this);
        }

    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_control_list, container, false);

        mListView = (ListView) view.findViewById(R.id.listView);
        mListView.setAdapter(new CustomAdapter(getContext(), this.controllableObjects));

        return view;
    }

    @Override
    public void userInputOccurred(ControllableObject controllableObject) {
        if (this.controlListFragmentEvent == null) {
            Fragment fragment = this.getParentFragment();
            if (fragment instanceof  ControlFragment)
                this.controlListFragmentEvent = (ControlFragment)fragment;
        }

        if (this.controlListFragmentEvent != null)
            this.controlListFragmentEvent.listFragmentEventOccurred(controllableObject);
    }

    public void setControlFragment(ControlFragment controlFragment) {
        this.controlListFragmentEvent = controlFragment;
    }
}
