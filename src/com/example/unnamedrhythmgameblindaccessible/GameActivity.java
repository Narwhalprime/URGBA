package com.example.unnamedrhythmgameblindaccessible;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class GameActivity extends Activity implements SensorEventListener {

	final long START_DELAY = 1000; // delay before ANY playing happens
    final float SHAKE_THRESHOLD = 9f; // min velocity to trigger 
    final long VIBRATE_DURATION = 200;
    final long RETURN_DELAY = 6000;
    
    // acceleration data from accelerometer
    float[] accel_data;
    float mAccel;
    float mAccelCurrent;
    float mAccelLast;
    float mAccelPeak;
    
    // song info stuff
    // read them from file instead
    int rawID = 0;
    double SONG_LENGTH_MS;
    double SONG_DELAY_MS;// = 140.0;
    double SONG_BPM;// = 58.025;
    double NUMBER_BEATS;
    double BEAT_LENGTH;
    int MAX_SCORE;// = 54;
	
    // gameplay stuff!
    // TODO: even more hard-coding
    
    // the sequence of notes                  
    String BEATMAP;// = "--------tTsS--sStT--sStT--sStTtTsStTtTsS-tT-sS-tTtTtTtTsS-tTsStTsStTtT-sS---";
    Map<String, String> patterns = new HashMap<String, String>(); // TODO: do this once singleton notes work
    boolean songStarted = false;
    int intervalCounter = 0;
    int currentNumTaps = 0; // in current interval
    int currentNumShakes = 0;
    char currentMapGesture = '-';
    char currentUserGesture = '-';
    int score = 0; // +2 for correct note, -2 for missing a note
    
    // UI stuff
	Button tapButton;
	TextView scoreTextView;
	
	// sound stuff
	MediaPlayer mMediaPlayerSong;
	MediaPlayer mMediaPlayerMetronome;
	MediaPlayer mMediaPlayerGestureWarnings;
	MediaPlayer mMediaPlayerGestureFeedback;
	MediaPlayer mMediaPlayerEnding;
	
	// sound option stuff
	float songVolume = 0.7f;
	
	// sensor stuff
    SensorManager mSensorManager;
    Sensor mAccelerometer;
    
    // timer stuff
    Timer mainTimer = new Timer();
    TimerTask beginNextInterval;
    TimerTask takeScore;
    TimerTask beginSong;
    TimerTask returnToMainMenu;
    long timeSongStart; // for debugging
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		Intent intent = getIntent();
		int fileID = intent.getExtras().getInt("file_id", R.raw.daftpunk_info);
		
		InputStream fis;
		try {
			Log.d("read in", "test");
			fis = getResources().openRawResource(fileID);
			Log.d("still reading in", "test 2");
			
			StringBuilder builder = new StringBuilder();
			int ch;
			while((ch = fis.read()) != -1){
			    builder.append((char)ch);
			}
			fis.close();
			
			String songData = builder.toString();
			StringTokenizer st = new StringTokenizer(songData, ";");
			String nameSong = st.nextToken();
			SONG_BPM = Double.parseDouble(st.nextToken());
			SONG_DELAY_MS = Double.parseDouble(st.nextToken());
			BEATMAP = st.nextToken();
			MAX_SCORE = Integer.parseInt(st.nextToken());

		} catch (FileNotFoundException e) {
			Log.d("can't find file", "test 3");
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		tapButton = (Button)findViewById(R.id.tap_button);
		scoreTextView = (TextView)findViewById(R.id.score_text_view);
		
		// TODO: shh, also hack
		if(fileID == R.raw.daftpunk_info)
			rawID = R.raw.daftpunk;
		else
			rawID = R.raw.senbonzakura;
		
		SONG_LENGTH_MS = MediaPlayer.create(getApplicationContext(), rawID).getDuration();
        NUMBER_BEATS = ((SONG_BPM / 60.0) * (SONG_LENGTH_MS / 1000.0));
        BEAT_LENGTH = SONG_LENGTH_MS / NUMBER_BEATS;
        Log.d("GameActivity", "NUMBER_BEATS, BEAT_LENGTH = " + NUMBER_BEATS + " " + BEAT_LENGTH);
        
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	@Override
	protected void onResume() {
		super.onResume();

        mMediaPlayerSong = MediaPlayer.create(GameActivity.this, rawID);
        mMediaPlayerSong.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
            	if(score > MAX_SCORE / 2)
            		mMediaPlayerEnding = MediaPlayer.create(GameActivity.this, R.raw.congrats);
            	else
            		mMediaPlayerEnding = MediaPlayer.create(GameActivity.this, R.raw.keep_trying);
            	mMediaPlayerEnding.start();
                mainTimer.schedule(returnToMainMenu, RETURN_DELAY);
            }

        });
        mMediaPlayerSong.setVolume(songVolume, songVolume);
        
		// timer tasks defined: check if shake or tap done
        beginNextInterval = new TimerTask() {
			public void run()
			{
				if(mMediaPlayerMetronome != null) 
				{
					mMediaPlayerMetronome.release();
				}
				if(mMediaPlayerGestureWarnings != null) 
				{
					mMediaPlayerGestureWarnings.release();
				}
				
				// Clap - to set the beat
				if(currentMapGesture == '-')
				{
					//Log.d("TIMING-CLAP", "TIMING-time = " + (System.currentTimeMillis() - timeSongStart));
			        mMediaPlayerMetronome = MediaPlayer.create(GameActivity.this, R.raw.background_beat);
					mMediaPlayerMetronome.start();
				}
				
				// warning tone before shake
				if(currentMapGesture == 's')
				{
					vibratePhone();
					mMediaPlayerMetronome = MediaPlayer.create(GameActivity.this, R.raw.whistle);
					mMediaPlayerMetronome.start();					
				}
				
				// warning tone before tap
				if(currentMapGesture == 't')
				{
					vibratePhone();
					mMediaPlayerGestureWarnings = MediaPlayer.create(GameActivity.this, R.raw.finish_tap);
					mMediaPlayerGestureWarnings.start();
				}
			}
        };
        takeScore = new TimerTask() {
			public void run()
			{
				// Get current gesture
				updateScore(currentMapGesture);

				// reset user gesture, point to next interval and get next map gesture
				currentUserGesture = '-';
				
				intervalCounter = Math.min(intervalCounter + 1, BEATMAP.length() - 1);
				currentMapGesture = BEATMAP.charAt(intervalCounter);
				currentNumShakes = 0;
				currentNumTaps = 0;
			}
		};
        beginSong = new TimerTask() {
			public void run()
			{
				songStarted = true;
				mMediaPlayerSong.start(); // play the song!
				timeSongStart = System.currentTimeMillis();
			}
        };
        returnToMainMenu = new TimerTask() {
        	public void run()
			{
        		runOnUiThread(new Runnable() {
        			@Override
        			public void run() {
        				Intent returnIntent = new Intent(getBaseContext(), MainActivity.class);
		            	startActivity(returnIntent);
        			}
        		});
			}
        };
		
        // initialize gestureTimer and the sensor manager on starting or resuming app
		mainTimer = new Timer();
		mainTimer.scheduleAtFixedRate(beginNextInterval, (long)(START_DELAY + SONG_DELAY_MS), (long)BEAT_LENGTH);
		mainTimer.scheduleAtFixedRate(takeScore, (long)(START_DELAY + SONG_DELAY_MS + (BEAT_LENGTH / 2)), (long)BEAT_LENGTH);
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
			onScreenShaked();
			mAccelPeak = 0;
		}
	 }
	 
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	// required for implementing interface
    }
	
	// TAP
	public void onButtonTapped(View v) {
		if(mMediaPlayerGestureFeedback != null)
		{
			mMediaPlayerGestureFeedback.release();	
		}
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				currentNumTaps++;
				tapButton.setText(R.string.debug_tap_screen);
//				Log.d("TIMING-TAP", "TIMING-time = " + (System.currentTimeMillis() - timeSongStart));
				currentUserGesture = 'T';
				mMediaPlayerGestureFeedback = MediaPlayer.create(getApplicationContext(), R.raw.clap);
				mMediaPlayerGestureFeedback.start();
			}
		});
    }
	
	// SHAKE
	public void onScreenShaked() {
		if(mMediaPlayerGestureFeedback != null)
		{
			mMediaPlayerGestureFeedback.release();	
		}
		runOnUiThread(new Runnable() {
		     @Override
		     public void run() {
		    	 currentNumShakes++;
		    	 tapButton.setText(R.string.debug_shake_screen);
//		    	 Log.d("TIMING-SHAKE", "TIMING-time = " + (System.currentTimeMillis() - timeSongStart));
		    	 currentUserGesture = 'S';
		    	 mMediaPlayerGestureFeedback = MediaPlayer.create(getApplicationContext(), R.raw.finish_shake);
		    	 mMediaPlayerGestureFeedback.start();
		    }
		});
	}
	
	public void updateScore(char currentGesture) {
//		Log.d("TIMING-SCORE UPDATE", "TIMING-time = " + (System.currentTimeMillis() - timeSongStart));
		switch(currentMapGesture)
		{
			case 'S':
				if(currentUserGesture == 'S')
				{
					score += 2;
					// TODO: positive feedback
				}
				else
				{
					score -= 2;
					// TODO: negative feedback
				}
				break;
				
			case 'T':
				if(currentUserGesture == 'T')
				{
					score += 2;
					// TODO: positive feedback
				}
				else
				{
					score -= 2;
					// TODO: negative feedback
				}
				break;
		}
		runOnUiThread(new Runnable() {
		     @Override
		     public void run() {
		    	 scoreTextView.setText("Score: " + score + "\nNext map gesture = " + currentMapGesture);
		    }
		});
	}
	
	public void vibratePhone() {
		Vibrator v = (Vibrator) this.getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
		v.vibrate(VIBRATE_DURATION);
	}
}
