package com.example.michael.traincontrol;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;


/**
 * A simple {@link Fragment} subclass.
 */
public class BluetoothEnableFragment extends Fragment {
    public interface BluetoothEnabler {
        void enableBluetooth();
    }

    public BluetoothEnableFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_bluetooth_enable, container, false);
        Button enableBtn = (Button) view.findViewById(R.id.EnableBtButton);
        enableBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getActivity() instanceof BluetoothEnabler) {
                    ((BluetoothEnabler) getActivity()).enableBluetooth();
                }
            }
        });
        return view;
    }

}
