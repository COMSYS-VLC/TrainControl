package de.rwthaachen.comsys.laboratory.vlc.traincontrol;

import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;


public class ControlPanelFragment extends Fragment implements ReceivingFragment {

    private static final int ID_TURNOUT = 0;
    private static final int ID_SPEED = 1;
    private static final int ID_LED0 = 2;

    private ImageView mTurnout;
    private boolean mTurnoutStraight = true;

    private final View.OnClickListener mTurnoutClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            toggleTurnout();
        }
    };

    private ImageView mLEDViews[] = new ImageView[4];
    private LEDState mLEDStates[] = new LEDState[] { LEDState.OFF, LEDState.OFF,
            LEDState.OFF, LEDState.OFF };
    private Handler mBlinkHandler = new Handler();
    private boolean mBlinkState = false;

    private final View.OnClickListener mLEDClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            for (int i = 0; i < mLEDViews.length; ++i) {
                if (mLEDViews[i] == v) {
                    toggleLED(i);
                    return;
                }
            }
        }
    };
    private final Runnable mBlinkRunnable = new Runnable() {
        @Override
        public void run() {
            for(int i = 0; i < mLEDViews.length; ++i) {
                if(LEDState.BLINKING == mLEDStates[i]) {
                    if(mBlinkState) {
                        mLEDViews[i].setImageTintMode(PorterDuff.Mode.SRC_IN);
                    } else {
                        mLEDViews[i].setImageTintMode(PorterDuff.Mode.DST);
                    }
                }
            }
            mBlinkState = !mBlinkState;
            mBlinkHandler.postDelayed(this, 250);
        }
    };

    private ImageView mForward;
    private ImageView mBackward;
    private SeekBar mSpeed;
    private int mCurrentSpeed = 63;
    private final SeekBar.OnSeekBarChangeListener mSpeedListener = new SeekBar.OnSeekBarChangeListener() {
        @Override
        public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
            int speed = progress - 63;
            if(speed == 0) {
                mForward.setImageTintMode(PorterDuff.Mode.DST);
                mBackward.setImageTintMode(PorterDuff.Mode.DST);
            } else if(speed > 0) {
                mForward.setImageTintMode(PorterDuff.Mode.SRC_IN);
                mBackward.setImageTintMode(PorterDuff.Mode.DST);
            } else {
                mForward.setImageTintMode(PorterDuff.Mode.DST);
                mBackward.setImageTintMode(PorterDuff.Mode.SRC_IN);
            }
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBar) {
            // ignored
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBar) {
            setSpeed(seekBar.getProgress() - 63, true);
        }
    };
    private final View.OnClickListener mDirectionClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v == mForward) {
                setSpeed(Math.abs(mCurrentSpeed), true);
            } else {
                setSpeed(Math.abs(mCurrentSpeed) * -1, true);
            }
        }
    };

    public ControlPanelFragment() {
        // Required empty public constructor
    }

    @Override
    public void handlePayload(byte[] data) {
        final int id = data[0];
        if(ID_TURNOUT == id) {
            mTurnoutStraight = (0 == data[1]);
            applyTurnout();
        } else if(ID_SPEED == id) {
            int speed = data[1] & 0x3F;
            if(0 != (data[1] & 0x80)) {
                speed *= -1;
            }
            setSpeed(speed, false);
        } else if(ID_LED0 <= id && (ID_LED0 + mLEDViews.length) > id) {
            int index = id - ID_LED0;
            mLEDStates[index] = LEDState.intToValue(data[1]);
            applyLEDState(index);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_control_panel, container, false);

        mTurnout = (ImageView) view.findViewById(R.id.image_turnout);
        mTurnout.setOnClickListener(mTurnoutClickListener);

        mLEDViews[0] = (ImageView) view.findViewById(R.id.image_led_front_left);
        mLEDViews[1] = (ImageView) view.findViewById(R.id.image_led_front_right);
        mLEDViews[2] = (ImageView) view.findViewById(R.id.image_led_rear_left);
        mLEDViews[3] = (ImageView) view.findViewById(R.id.image_led_rear_right);

        final ColorStateList tintColor = ColorStateList.valueOf(getResources().getColor(R.color.colorAccent));
        for(int i = 0; i < 4; ++i) {
            mLEDViews[i].setOnClickListener(mLEDClickListener);
            mLEDViews[i].setImageTintMode(PorterDuff.Mode.DST);
            mLEDViews[i].setImageTintList(tintColor);
        }

        mForward = (ImageView) view.findViewById(R.id.image_forward);
        mBackward = (ImageView) view.findViewById(R.id.image_backward);
        mForward.setImageTintList(tintColor);
        mForward.setImageTintMode(PorterDuff.Mode.DST);
        mBackward.setImageTintList(tintColor);
        mBackward.setImageTintMode(PorterDuff.Mode.DST);
        mForward.setOnClickListener(mDirectionClickListener);
        mBackward.setOnClickListener(mDirectionClickListener);

        mSpeed = (SeekBar) view.findViewById(R.id.slider_speed);
        mSpeed.setOnSeekBarChangeListener(mSpeedListener);

        return view;
    }

    @Override
    public void onPause() {
        mBlinkHandler.removeCallbacksAndMessages(null);

        super.onPause();
    }

    @Override
    public void onResume() {
        super.onResume();

        mBlinkHandler.postDelayed(mBlinkRunnable, 250);

        // Request initial state
        if(getParentFragment() instanceof SendingFragment) {
            ((SendingFragment) getParentFragment()).sendPayload(new byte[] {(byte)0xFE, (byte)0x00});
        }
    }

    private void toggleTurnout() {
        mTurnoutStraight = !mTurnoutStraight;
        applyTurnout();

        byte[] data = new byte[2];
        data[0] = (byte) ID_TURNOUT;
        data[1] = (byte) (mTurnoutStraight ? 0 : 1);
        if(getParentFragment() instanceof SendingFragment) {
            ((SendingFragment) getParentFragment()).sendPayload(data);
        }
    }

    private void applyTurnout() {
        if(mTurnoutStraight) {
            mTurnout.setImageResource(R.drawable.ic_straight);
        } else {
            mTurnout.setImageResource(R.drawable.ic_diverging);
        }
    }

    private void toggleLED(int index) {
        switch(mLEDStates[index]) {
            case OFF:
                mLEDStates[index] = LEDState.BLINKING;
                break;
            case BLINKING:
                mLEDStates[index] = LEDState.ON;
                break;
            case ON:
                mLEDStates[index] = LEDState.OFF;
                break;
        }

        applyLEDState(index);

        byte[] data = new byte[2];
        data[0] = (byte) (ID_LED0 + index);
        data[1] = (byte) LEDState.valueToInt(mLEDStates[index]);
        for(int i = 0; i < LEDState.values().length; ++i) {
            if(LEDState.values()[i] == mLEDStates[index]) {
                data[1] = (byte) i;
                break;
            }
        }
        if(getParentFragment() instanceof SendingFragment) {
            ((SendingFragment) getParentFragment()).sendPayload(data);
        }
    }

    private void applyLEDState(int index) {
        switch(mLEDStates[index]) {
            case OFF:
                mLEDViews[index].setImageTintMode(PorterDuff.Mode.DST);
                break;
            case BLINKING:
                if(mBlinkState) {
                    mLEDViews[index].setImageTintMode(PorterDuff.Mode.DST);
                } else {
                    mLEDViews[index].setImageTintMode(PorterDuff.Mode.SRC_IN);
                }
                break;
            case ON:
                mLEDViews[index].setImageTintMode(PorterDuff.Mode.SRC_IN);
                break;
        }
    }

    private void setSpeed(int speed, boolean notify) {
        if(speed != mCurrentSpeed) {
            mCurrentSpeed = speed;
            mSpeed.setProgress(mCurrentSpeed + 63);

            if(notify) {
                byte[] data = new byte[2];
                data[0] = ID_SPEED;
                data[1] = (byte) Math.abs(mCurrentSpeed);
                if(mCurrentSpeed < 0) {
                    data[1] |= 0x80;
                }
                if(getParentFragment() instanceof SendingFragment) {
                    ((SendingFragment) getParentFragment()).sendPayload(data);
                }
            }
        }
    }

    private enum LEDState {
        OFF,
        BLINKING,
        ON;

        public static int valueToInt(LEDState state) {
            switch(state) {
                case OFF:
                    return 0;
                case BLINKING:
                    return 1;
                case ON:
                    return 2;
            }
            return -1;
        };

        public static LEDState intToValue(int value) {
            switch(value) {
                case 1:
                    return BLINKING;
                case 2:
                    return ON;
                default:
                    return OFF;
            }
        }
    }
}
