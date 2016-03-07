package com.example.michael.traincontrol;


import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


/**
 * A simple {@link Fragment} subclass.
 */
public class ConnectFragment extends Fragment {

    private static final String ARG_DEVICE_NAME = "devname";
    private static final String ARG_DEVICE_ADDR = "devaddr";

    public ConnectFragment() {
        // Required empty public constructor
    }

    public static ConnectFragment newInstance(String deviceName, String deviceAddr) {
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_NAME, deviceName);
        args.putString(ARG_DEVICE_ADDR, deviceAddr);
        ConnectFragment fragment = new ConnectFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect, container, false);

        TextView name = (TextView) view.findViewById(R.id.text_device_name);
        TextView addr = (TextView) view.findViewById(R.id.text_device_id);
        Button abort = (Button) view.findViewById(R.id.button_abort);

        name.setText(getString(R.string.connect_device_name, getArguments().getString(ARG_DEVICE_NAME)));
        addr.setText(getString(R.string.connect_device_addr, getArguments().getString(ARG_DEVICE_ADDR)));

        abort.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(getParentFragment() instanceof ConnectAbortHandler) {
                    ((ConnectAbortHandler) getParentFragment()).abortConnecting();
                }
            }
        });

        return view;
    }

    public interface ConnectAbortHandler {
        void abortConnecting();
    }

}
