package com.example.unnamedrhythmgameblindaccessible;

import android.app.Activity;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;

public class GameActivity extends Activity implements SensorEventListener {

    final int MAX_RECORDS = 200;
    final int SHAKE_THRESHOLD = 16; // min z-max/min difference 
    
    float[] accel_data;
    
    float mAccel;
    float mAccelCurrent;
    float mAccelLast;
	
	Button tapButton;
	
    SensorManager mSensorManager;
    Sensor mAccelerometer;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_game);
		
		tapButton = (Button)findViewById(R.id.tap_button);
		
		mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
	}

	@Override
	protected void onResume() {
		super.onResume();
		
		mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_GAME);
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mSensorManager.unregisterListener(this);
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
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
	 }
	 
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
    	// required for implementing interface
    }
	
	public void onButtonTapped(View view) {
		tapButton.setText(R.string.debug_tap_screen);
	}
	
	public void onScreenShaked() {
		tapButton.setText(R.string.debug_shake_screen);
	}
	
}
