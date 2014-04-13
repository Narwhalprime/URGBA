package com.example.unnamedrhythmgameblindaccessible;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.view.View;

public class WelcomeActivity extends Activity {

	MediaPlayer mMediaPlayer;
	boolean isMessageDone = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		
		mMediaPlayer = MediaPlayer.create(WelcomeActivity.this, R.raw.welcome_message);
		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mMediaPlayer.release();
                isMessageDone = true;
            }
        });
		mMediaPlayer.start();
	}
	
	public void onButtonTapped(View v) {
		if(isMessageDone)
		{
			Intent intent = new Intent(this, MainActivity.class);
	    	startActivity(intent);
		}
	}

}
