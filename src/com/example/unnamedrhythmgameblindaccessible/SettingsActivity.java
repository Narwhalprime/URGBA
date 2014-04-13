package com.example.unnamedrhythmgameblindaccessible;

import android.app.Activity;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;

public class SettingsActivity extends Activity {

	MediaPlayer mMediaPlayer;
	
	// TODO: shh, hack
	boolean doSwitch = false;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_settings);
		
		mMediaPlayer = MediaPlayer.create(SettingsActivity.this, R.raw.settings);
		mMediaPlayer.start();
	}

	public void changeSong(View v) {
		doSwitch = !doSwitch;
	}
	
	public void switchToMainActivity(View v) {
		Intent intent = new Intent(this, MainActivity.class);
		intent.putExtra("switch_song", doSwitch);
		startActivity(intent);
	}
}
