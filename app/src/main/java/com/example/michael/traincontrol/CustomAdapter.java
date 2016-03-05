package com.example.michael.traincontrol;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.ToggleButton;

/**
 * Created by Michael on 02.03.2016.
 * CustomAdapter to fill the listView with TrackSignals respective Turnout objects.
 */
public class CustomAdapter extends ArrayAdapter<ControllableObject> {
    Context context;
    ControllableObject[] controllableObjects;

    /**
     * Constructor.
     * @param context A reference to the current activity.
     * @param controllableObjects List of listView items.
     */
    public CustomAdapter(Context context, ControllableObject[] controllableObjects) {
        super(context, R.layout.turnout, controllableObjects);
        this.context = context;
        this.controllableObjects = controllableObjects;
    }

    /**
     * Get item from listview.
     * @param position Index of item in controllableObjects.
     * @param convertView The listView item.
     * @param parent The viewGroup to which the item belongs.
     * @return ListView Item.
     */
    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = ((Activity)context).getLayoutInflater();

        // Set Turnout.
        if (this.controllableObjects[position] instanceof Turnout) {
            convertView = inflater.inflate(R.layout.turnout, parent, false);

            TextView textView = (TextView) convertView.findViewById(R.id.textViewTurnout);
            textView.setText(this.controllableObjects[position].getName());

            Switch switcher = (Switch) convertView.findViewById(R.id.switchTurnout);
            switcher.setChecked(((Turnout) this.controllableObjects[position]).getStraight());
            switcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ((Turnout) controllableObjects[position]).setStraight(isChecked);
                }
            });

            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageViewTurnout);
            imageView.setBackgroundColor(this.controllableObjects[position].getColor());
        }
        // Set TrackSignal.
        else if (this.controllableObjects[position] instanceof TrackSignal) {
            convertView = inflater.inflate(R.layout.tracksignal, parent, false);

            TextView textView = (TextView) convertView.findViewById(R.id.textViewTrackSignal);
            textView.setText(this.controllableObjects[position].getName());

            SeekBar seekBar = (SeekBar) convertView.findViewById(R.id.seekBarSpeed);
            seekBar.setProgress(((TrackSignal) this.controllableObjects[position]).getSpeed());
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    // Not used.
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {
                    // Not used.
                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    ((TrackSignal)controllableObjects[position]).setSpeed(seekBar.getProgress());
                }
            });

            ToggleButton toggleButton = (ToggleButton) convertView.findViewById(R.id.toggleButtonTracksignal);
            toggleButton.setChecked(((TrackSignal) controllableObjects[position]).getForward());
            toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    ((TrackSignal) controllableObjects[position]).setForward(isChecked);
                }
            });

            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageViewTracksignal);
            imageView.setBackgroundColor(this.controllableObjects[position].getColor());
        }
        // Set LED.
        else if (this.controllableObjects[position] instanceof LED) {
            convertView = inflater.inflate(R.layout.led, parent, false);

            TextView textView = (TextView) convertView.findViewById(R.id.textViewLED);
            textView.setText(this.controllableObjects[position].getName());

            Spinner spinner = (Spinner) convertView.findViewById(R.id.spinnerLED);
            spinner.setSelection(((LED)this.controllableObjects[position]).getLedStateSpinnerIndex());
            spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int index, long id) {
                    ((LED) controllableObjects[position]).setLedStateSpinnerIndex(index);
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // Not used.
                }
            });

            ImageView imageView = (ImageView) convertView.findViewById(R.id.imageViewLED);
            imageView.setBackgroundColor(this.controllableObjects[position].getColor());
        }

        return convertView;
    }
}
