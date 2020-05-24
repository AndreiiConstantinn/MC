package com.application.alarmflip;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Calendar;


@SuppressLint("Registered")
public class MainActivity extends AppCompatActivity implements TimePickerDialog.OnTimeSetListener, SensorEventListener
{
    private TextView mTextView;
    RadioGroup radioGroup;
    RadioButton radioButton;

    private static final long AGGRESSIVE_DELAY = 2131165299;
    //private static final long GENTLE_DELAY = 2131165300;
    private static final int SAMPLING_PERIOD_ROTATION = 75000;
    private static final double NEGATIVE_ROLL_BOUND = -1.5;
    private static final double POSITIVE_ROLL_BOUND = 1.5;
    private static final boolean FACED_UP = true;
    private static final boolean FACED_DOWN = false;
    private static final String SHARED_PREF = "shared";
    private static final String TEXT = "text";

    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private SensorEventListener sensorListener;

    private static boolean facePosition_now = false;
    private static boolean facePosition_previous = false;
    private static double roll = 0;
    private static String timeText = "abcd";
    public static boolean startAlarmFlag = false;
    public static boolean typeOfDelay = false;
    public static int delayTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        assert sensorManager != null;
        rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

        mTextView = findViewById(R.id.text_alarm_status);
        radioGroup = findViewById(R.id.radioGroup);

        Button buttonTimePicker = findViewById(R.id.button_set_alarm);
        buttonTimePicker.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment timePicker = new TimePickerFragment();
                timePicker.show(getSupportFragmentManager(), "time picker");
            }
        });

        final MediaPlayer alarmSound = MediaPlayer.create(this, R.raw.alarm_clock_sound);

        AlertReceiver.alarmSound = alarmSound;

        Button buttonCancelAlarm = findViewById(R.id.button_cancel_alarm);
        buttonCancelAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                delayTime = 0;

                for (int i = 0; i < radioGroup.getChildCount(); i++) {
                    radioGroup.getChildAt(i).setEnabled(true);
                }

                cancelAlarm();
                alarmSound.stop();
                try {
                    alarmSound.prepare();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });


        if (rotationSensor == null) {
            Toast.makeText(this, "Rotation sensor not available!", Toast.LENGTH_LONG).show();
            finish();
        } else {
            sensorManager.registerListener(MainActivity.this, rotationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        }
    }

    private void update() {
        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        timeText = sharedPreferences.getString(TEXT, "");
        mTextView.setText(timeText);
    }

    @Override
    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {

        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);

        for (int i = 0; i < radioGroup.getChildCount(); i++) {
            radioGroup.getChildAt(i).setEnabled(false);
        }

        updateTimeText(c);
        startAlarm(c);
    }

    private void updateTimeText(Calendar c) {
        timeText = "Alarm set for: ";
        timeText += DateFormat.getTimeInstance(DateFormat.SHORT).format(c.getTime());

        mTextView.setText(timeText);

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TEXT, mTextView.getText().toString());
        editor.apply();
    }

    private void startAlarm(Calendar c) {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);

        if (c.before(Calendar.getInstance())) {
            c.add(Calendar.DATE, 1);
        }
        assert alarmManager != null;
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, c.getTimeInMillis(), pendingIntent);
        update();
    }

    @SuppressLint("SetTextI18n")
    private void cancelAlarm() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(this, AlertReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 1, intent, 0);

        assert alarmManager != null;
        alarmManager.cancel(pendingIntent);
        mTextView.setText("Alarm canceled");

        SharedPreferences sharedPreferences = getSharedPreferences(SHARED_PREF, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(TEXT, mTextView.getText().toString());
        editor.apply();
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }

    @Override
    public void onSensorChanged(final SensorEvent sensorEvent) {
        float[] R = new float[16];
        SensorManager.getRotationMatrixFromVector(R, sensorEvent.values);

        float[] orientation = new float[3];
        SensorManager.getOrientation(R, orientation);

        roll = orientation[2];

        facePosition_previous = facePosition_now;
        facePosition_now = getFacePosition_now();

        if ((facePosition_now != facePosition_previous) && startAlarmFlag == true) {
            startAlarmFlag = false;
            delayAlarm(typeOfDelay);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        sensorManager.registerListener(sensorListener, rotationSensor, SAMPLING_PERIOD_ROTATION);
        update();
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(sensorListener);
        update();
    }

    protected static boolean getFacePosition_now() {
        if (roll > NEGATIVE_ROLL_BOUND && roll < POSITIVE_ROLL_BOUND) {
            return FACED_UP;
        } else //if(roll < NEGATIVE_ROLL_BOUND && roll > POSITIVE_ROLL_BOUND)
        {
            return FACED_DOWN;
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        String stateToSave = mTextView.getText().toString();
        savedInstanceState.putString("saved_state", stateToSave);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        String stateSaved = savedInstanceState.getString("saved_state");

        if (stateSaved != null) {
            mTextView.setText(stateSaved);
        }
    }

    //true -> aggressive
    //false -> gentle
    protected void delayAlarm(boolean delayType) {
        cancelAlarm();
        AlertReceiver.alarmSound.stop();
        try {
            AlertReceiver.alarmSound.prepare();
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Set a new alarm after the current one is cancelled
        Calendar rightNow = Calendar.getInstance();

        int hourOfDay = rightNow.get(Calendar.HOUR_OF_DAY);
        int minute = rightNow.get(Calendar.MINUTE);

        delayTime++;

        if (delayType == true){
            switch (delayTime){
                case 1 : minute += 5; break;
                case 2 : minute += 3; break;
                case 3 : minute += 2; break;
                default: minute += 1;

                    //Test purpose:
                    //case 1 : minute += 3; break;
                    //case 2 : minute += 2; break;
                    //default: minute += 1;
            }
        } else {
            switch (delayTime){
                case 1 : minute += 15; break;
                case 2 : minute += 10; break;
                case 3 : minute += 7; break;
                case 4 : minute += 5; break;
                case 5 : minute += 3; break;
                case 6 : minute += 2; break;
                default: minute += 1;
            }
        }

        rightNow.set(Calendar.HOUR_OF_DAY, hourOfDay);
        rightNow.set(Calendar.MINUTE, minute);
        rightNow.set(Calendar.SECOND, 0);

        updateTimeText(rightNow);
        startAlarm(rightNow);
    }

    public void checkButton(View v) {
        int radioId = radioGroup.getCheckedRadioButtonId();

        radioButton = findViewById(radioId);

        if (radioId == AGGRESSIVE_DELAY) {
            typeOfDelay = true;
        } else {
            typeOfDelay = false;
        }
    }
}
