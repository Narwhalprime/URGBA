package com.example.unnamedrhythmgameblindaccessible;

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
import android.widget.Button;

public class GameActivity extends Activity implements SensorEventListener {

	final long START_DELAY = 5000; // delay before ANY playing happens
    final int MAX_RECORDS = 200;
    final float SHAKE_THRESHOLD = 9.5f; // min z-max/min difference 
    
    // acceleration data from accelerometer
    float[] accel_data;
    float mAccel;
    float mAccelCurrent;
    float mAccelLast;
    float mAccelPeak;
    
    // do I need these? more to come
    // TODO: hardcoded for Get Lucky
    long SONG_LENGTH_MS = 408413;
    long SONG_DELAY_MS = 140;
    double SONG_BPM = 116.05;
    long NUMBER_BEATS;
    double BEAT_LENGTH;
	
    // UI
	Button tapButton;
	
	// sound stuff
	SoundManager mSoundManager;
	MediaPlayer mMediaPlayer;
	
	// sensor stuff
    SensorManager mSensorManager;
    Sensor mAccelerometer;
    
    // timer stuff
    Timer mainTimer = new Timer();
    TimerTask beginNextInterval;
    TimerTask beginSong;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		tapButton = (Button)findViewById(R.id.tap_button);

        NUMBER_BEATS = (long)((SONG_BPM / 60.0) * (SONG_LENGTH_MS / 1000.0));
        BEAT_LENGTH = SONG_LENGTH_MS / NUMBER_BEATS;
        Log.d("GameActivity", "NUMBER_BEATS, BEAT_LENGTH = " + NUMBER_BEATS + " " + BEAT_LENGTH);
        
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mSoundManager = new SoundManager();
        mSoundManager.initSounds(getBaseContext());
        
        // sound effects
        mSoundManager.addSound(1, R.raw.whistle);
        
        // music
        mSoundManager.addSound(10, R.raw.daftpunk);
	}

	@Override
	protected void onResume() {
		super.onResume();

        mMediaPlayer = MediaPlayer.create(GameActivity.this, R.raw.daftpunk);
		// timer tasks defined: check if shake or tap done
        beginNextInterval = new TimerTask() {
			public void run()
			{
				// Log.d("GameActivity", "playing metronome sound");
				mSoundManager.playSound(1);
				if(mAccelPeak > SHAKE_THRESHOLD)
				{
					Log.d("GameActivity", "Shake threshold reached");
					onScreenShaked();
					mAccelPeak = 0;
				}
			}
        };
        beginSong = new TimerTask() {
			public void run()
			{
				mMediaPlayer.start(); // play the song!
			}
        };
		
        // initialize gestureTimer and the sensor manager on starting or resuming app
		mainTimer = new Timer();
		mainTimer.scheduleAtFixedRate(beginNextInterval, START_DELAY + SONG_DELAY_MS, (long)BEAT_LENGTH);
		mainTimer.schedule(beginSong, START_DELAY);
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mainTimer.cancel();
		mSensorManager.unregisterListener(this);
		mMediaPlayer.stop();
		mMediaPlayer.reset();
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
			// Log.d("GameActivity", "mAccelPeak = " + mAccelPeak);
		}
	 }
	 
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	// required for implementing interface
    }
	
	public void onButtonTapped(View view) {
		runOnUiThread(new Runnable() {
		     @Override
		     public void run() {
		 		tapButton.setText(R.string.debug_tap_screen);
		    }
		});
	}
	
	public void onScreenShaked() {
		runOnUiThread(new Runnable() {
		     @Override
		     public void run() {
		 		tapButton.setText(R.string.debug_shake_screen);
		    }
		});
	}
	
}
