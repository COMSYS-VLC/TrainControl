package com.example.michael.traincontrol;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;


public class ControlListFragment extends Fragment {

    // The list of objects (turnouts, tracksignals, LEDs) that are controllable.
    private final ControllableObject[] controllableObjects = new ControllableObject[]{
            new Turnout(Color.rgb(255, 255, 0), "Turnout 1", true),
            new TrackSignal(Color.rgb(255, 0, 0), "Red Signal", 0, true),
            new LED(Color.rgb(0, 0, 255), "Left Front", LED.LedState.OFF),
            new LED(Color.rgb(0, 0, 255), "Right Front", LED.LedState.BLINKING),
            new LED(Color.rgb(255, 0, 0), "Left Back", LED.LedState.BLINKING),
            new LED(Color.rgb(255, 0, 0), "Right Back", LED.LedState.ON)};

    private ListView mListView;

    public ControlListFragment() {
        // Required empty public constructor
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

}
