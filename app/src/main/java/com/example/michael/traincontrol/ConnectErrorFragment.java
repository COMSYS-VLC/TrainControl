package com.example.michael.traincontrol;


import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;


public class ConnectErrorFragment extends Fragment {
    public interface ConnectErrorHandler {
        void retryConnect();
        void scanForOtherDevice();
    }

    private static final String ARG_DEVICE_NAME = "devname";
    private static final String ARG_ERROR_STR = "errstr";

    public ConnectErrorFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param error Error to display.
     * @return A new instance of fragment ConnectErrorFragment.
     */
    public static ConnectErrorFragment newInstance(String bleDeviceName, String error) {
        ConnectErrorFragment fragment = new ConnectErrorFragment();
        Bundle args = new Bundle();
        args.putString(ARG_DEVICE_NAME, bleDeviceName);
        args.putString(ARG_ERROR_STR, error);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_connect_error, container, false);

        TextView name = (TextView) view.findViewById(R.id.text_connect_error_name);
        TextView msg = (TextView) view.findViewById(R.id.text_connect_error_msg);
        Button retry = (Button) view.findViewById(R.id.button_retry);
        Button scan = (Button) view.findViewById(R.id.button_scan);

        name.setText(getString(R.string.connect_device_error_name, getArguments().getString(ARG_DEVICE_NAME)));
        msg.setText(getString(R.string.connect_error_msg, getArguments().getString(ARG_ERROR_STR)));

        retry.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragment() instanceof ConnectErrorHandler) {
                    ((ConnectErrorHandler) getParentFragment()).retryConnect();
                }
            }
        });

        scan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (getParentFragment() instanceof ConnectErrorHandler) {
                    ((ConnectErrorHandler) getParentFragment()).scanForOtherDevice();
                }
            }
        });

        return view;
    }

}
