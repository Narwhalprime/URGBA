package com.example.unnamedrhythmgameblindaccessible;

import java.util.Timer;
import java.util.TimerTask;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;

public class WelcomeActivity extends Activity {

	MediaPlayer mMediaPlayer;
	Timer timer;
	TimerTask messageEnd;
	boolean isMessageDone = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		
		timer = new Timer();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.welcome, menu);
		return true;
	}
	
	@Override
	public void onResume() {
		super.onResume();

        mMediaPlayer = MediaPlayer.create(WelcomeActivity.this, R.raw.welcome_message);
        mMediaPlayer.start();
        timer.schedule(new TimerTask() {
        	@Override
        	public void run() {
        		isMessageDone = true;
        	}
        }, mMediaPlayer.getDuration());
	}

	@Override
	public void onPause() {
		super.onPause();
		
		timer.cancel();
		mMediaPlayer.stop();
		mMediaPlayer.reset();
	}
	
	public void onButtonTapped(View v) {
		if(isMessageDone)
		{
			Intent intent = new Intent(this, MainActivity.class);
	    	startActivity(intent);
		}
	}

}
