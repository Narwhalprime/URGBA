package com.example.unnamedrhythmgameblindaccessible;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends Activity {

	MediaPlayer mMediaPlayer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mMediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.main_message);
		mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {

            @Override
            public void onCompletion(MediaPlayer mp) {
                mMediaPlayer.release();
            }

        });
		mMediaPlayer.start();
	}
	
	public void switchToGameActivity(View view)
    {
    	Intent intent = new Intent(this, GameActivity.class);
    	startActivity(intent);
    }
    
    public void switchToSettingsActivity(View view)
    {
    	Intent intent = new Intent(this, SettingsActivity.class);
    	startActivity(intent);
    }

}
