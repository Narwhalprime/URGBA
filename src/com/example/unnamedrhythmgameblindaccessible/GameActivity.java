package com.example.unnamedrhythmgameblindaccessible;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.accessibility.AccessibilityManager.TouchExplorationStateChangeListener;
import android.widget.Button;

public class GameActivity extends Activity implements SensorEventListener {

	final long START_DELAY = 8000; // delay before ANY playing happens
    final int MAX_RECORDS = 200;
    final float SHAKE_THRESHOLD = 12.5f; // min velocity to trigger 
    
    // acceleration data from accelerometer
    float[] accel_data;
    float mAccel;
    float mAccelCurrent;
    float mAccelLast;
    float mAccelPeak;
    
    // do I need these? more to come
    // TODO: hard-coded values
    double SONG_LENGTH_MS;
    double SONG_DELAY_MS = 130.0;
    double SONG_BPM = 58.025;
    double NUMBER_BEATS;
    double BEAT_LENGTH;
	
    // gameplay stuff!
    // TODO: even more hard-coding;
    String beatmap = "--------tTsS--"; // the sequence of notes
    int intervalCounter = 0;
    boolean hasTapped = false; // in current interval
    boolean hasShaked = false;
    char currentMapGesture = '-';
    char currentUserGesture = '-';
    
    // UI
	Button tapButton;
	
	// sound stuff
	MediaPlayer mMediaPlayerSong;
	MediaPlayer mMediaPlayerMetronome;
	MediaPlayer mMediaPlayerGestureWarnings;
	MediaPlayer mMediaPlayerGestureFeedback;
	
	// sensor stuff
    SensorManager mSensorManager;
    Sensor mAccelerometer;
    
    // timer stuff
    Timer mainTimer = new Timer();
    TimerTask beginNextInterval;
    TimerTask endNextInterval;
    TimerTask beginSong;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		tapButton = (Button)findViewById(R.id.tap_button);
		
		SONG_LENGTH_MS = MediaPlayer.create(getApplicationContext(), R.raw.daftpunk).getDuration();
        NUMBER_BEATS = ((SONG_BPM / 60.0) * (SONG_LENGTH_MS / 1000.0));
        BEAT_LENGTH = SONG_LENGTH_MS / NUMBER_BEATS;
        Log.d("GameActivity", "NUMBER_BEATS, BEAT_LENGTH = " + NUMBER_BEATS + " " + BEAT_LENGTH);
        
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	@Override
	protected void onResume() {
		super.onResume();

        mMediaPlayerSong = MediaPlayer.create(GameActivity.this, R.raw.daftpunk);
		// timer tasks defined: check if shake or tap done
        beginNextInterval = new TimerTask() {
			public void run()
			{
				// Get current gesture
				currentMapGesture = beatmap.charAt(intervalCounter);
				
				
				if(mMediaPlayerMetronome != null) 
				{
					mMediaPlayerMetronome.stop();
					mMediaPlayerMetronome.release();
				}
				
				// Initiate a clap 
		        mMediaPlayerMetronome = MediaPlayer.create(GameActivity.this, R.raw.clap);
				mMediaPlayerMetronome.start();
			}
        };
        endNextInterval = new TimerTask() {
			public void run()
			{
				mMediaPlayerMetronome.stop();
				mMediaPlayerMetronome.release();
			}
        };
        beginSong = new TimerTask() {
			public void run()
			{
				mMediaPlayerSong.start(); // play the song!
			}
        };
		
        // initialize gestureTimer and the sensor manager on starting or resuming app
		mainTimer = new Timer();
		mainTimer.scheduleAtFixedRate(beginNextInterval, (long)(START_DELAY + SONG_DELAY_MS), (long)BEAT_LENGTH);
		mainTimer.schedule(beginSong, START_DELAY);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mainTimer.cancel();
		mSensorManager.unregisterListener(this);
		mMediaPlayerSong.stop();
		mMediaPlayerSong.reset();
	}
	
	public void onSensorChanged(SensorEvent event) {
		if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER)
			return;
		
		float mSensorX = event.values[0];
		float mSensorY = event.values[1];
		float mSensorZ = event.values[2];
		
		// conditions for shake
		// shake code derived from: http://stackoverflow.com/questions/2317428/android-i-want-to-shake-it
		mAccelLast = mAccelCurrent;
		mAccelCurrent = (float)(Math.sqrt((double)(mSensorX*mSensorX + mSensorY*mSensorY + mSensorZ*mSensorZ)));

		float delta = mAccelCurrent - mAccelLast;
		mAccel = mAccel * 0.9f + delta; // low-cut filter
		if(mAccel > mAccelPeak)
		{
			mAccelPeak = mAccel;
		}
		
		if(mAccelPeak > SHAKE_THRESHOLD)
		{
			Log.d("GameActivity", "Shake threshold reached");
			onScreenShaked();
			mAccelPeak = 0;
		}
	 }
	 
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	// required for implementing interface
    }
	
	// TAP
	public void onButtonTapped(View v) {
		Log.d("GameActivity", "onClick");
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				hasTapped = true;
				tapButton.setText(R.string.debug_tap_screen);
				currentUserGesture = 'T';
			}
		});
    }
	
	// SHAKE
	public void onScreenShaked() {
		runOnUiThread(new Runnable() {
		     @Override
		     public void run() {
		    	 hasShaked = true;
		    	 tapButton.setText(R.string.debug_shake_screen);
		    	 currentUserGesture = 'S';
		    }
		});
	}
	
}
