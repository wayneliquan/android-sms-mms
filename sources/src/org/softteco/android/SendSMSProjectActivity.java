package org.softteco.android;

import java.io.IOException;
import java.io.OutputStream;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

public class SendSMSProjectActivity extends Activity {
	private int interval = 60;
	private int series = 1;
	private Button buttonStart;
	private Button buttonStop;
	private boolean started = false;
	private Switch periodicSwitch;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main);

		final TextView intervalField = (TextView) findViewById(R.id.interval);
		final TextView seriesField = (TextView) findViewById(R.id.series);
		intervalField.setText("" + interval);
		seriesField.setText("" + series);
		
		buttonStart = (Button) findViewById(R.id.start);
		buttonStart.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				try {
					interval = Integer.parseInt(intervalField.getText().toString());
					series = Integer.parseInt(seriesField.getText().toString());
				} catch(Exception e) {
					Toast.makeText(SendSMSProjectActivity.this, R.string.error_parsing, Toast.LENGTH_LONG);
					return;
				}
				
				started = true;
				buttonStop.setVisibility(View.VISIBLE);
				buttonStart.setVisibility(View.GONE);
				startTask();
			}
		});

		buttonStop = (Button) findViewById(R.id.stop);
		buttonStop.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setStop();
			}
		});
		
		periodicSwitch = (Switch)findViewById(R.id.periodic);
		periodicSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				intervalField.setEnabled(isChecked);
			}
		});

	}

	private void setStop() {
		buttonStop.setVisibility(View.GONE);
		buttonStart.setVisibility(View.VISIBLE);
		started = false;
	}

	private void debug(final String string) {
		runOnUiThread(new Runnable() {
			public void run() {
				Toast.makeText(getApplicationContext(), string,
						Toast.LENGTH_SHORT).show();
			}
		});
		Log.d("TESTSMS", string);
	}

	private void startTask() {
		if(!periodicSwitch.isChecked()) {
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					sendMessageAfterDelay();
					runOnUiThread(new Runnable(){
						public void run() {
							setStop();
						}
					});
					return null;
				}
			}.execute((Void) null);
		} else {
			debug("Interval set to " + interval + " seconds.");
			
			new AsyncTask<Void, Void, Void>() {
				@Override
				protected Void doInBackground(Void... params) {
					while (true && started) {
						sendMessageAfterDelay();
					}
					return null;
				}
			}.execute((Void) null);
		}
	}

	private void runSendingProcess() {
		for(int i = 0 ; i < series; i++) {
			if (((RadioButton) findViewById(R.id.sms)).isChecked()) {
				sendSms();
			} else if (((RadioButton) findViewById(R.id.mms)).isChecked()) {
				sendMms();
			}
		}
	}
	
	private void sendMms() {
		Log.e("TESTMMS", "Creating MMS");
		
		Uri uri = Uri.parse("content://mms/inbox");
		
		ContentValues contentValues = new ContentValues();
		contentValues.put("sub", "Hello, dude!");
		contentValues.put("ct_t", "application/vnd.wap.multipart.related");
		contentValues.put("date", System.currentTimeMillis() / 1000);
		Uri id = getContentResolver().insert(uri, contentValues);
		Log.e("TESTMMS", "Url :" + id);

		ContentValues address = new ContentValues();
		address.put("address", "123456");

		Uri addressUri = Uri.withAppendedPath(id, "/addr");
		Uri addr = getContentResolver().insert(addressUri, address);
		Log.e("TESTMMS", "Addr id:" + addr);

		Uri partUri = Uri.withAppendedPath(id, "/part");
		ContentValues partValues = new ContentValues();
		
		Uri mmsPartUri = getContentResolver().insert(partUri, partValues);
		OutputStream os;
		try {
			os = getContentResolver().openOutputStream(mmsPartUri);
		    os.write("Hello, dude!".getBytes());
		} catch (IOException e) {
			e.printStackTrace();
		}
		Log.e("TESTMMS", "Part id:" + mmsPartUri);

		
		Cursor cursor = getContentResolver().query(Uri.parse("content://mms"),
				new String[] { "_id", "date" }, "seen = 0", null, null);
		Log.e("TESTMMS", "Count " + cursor.getCount());
		cursor.close();
	}

	private void sendSms() {
		if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			String[] data = new String[] { "07911326040000F0040B911346610079F60000208062917314080CC8F71D14969741F977FD07" };
			debug("Sending " + data.length + " SMS.");
			for (String messageData : data) {
				Intent intent = new Intent();
				intent.setClassName("com.android.mms",
						"com.android.mms.transaction.SmsReceiverService");
				intent.setAction("android.provider.Telephony.SMS_RECEIVED");
				intent.putExtra("pdus",
						new Object[] { hexStringToByteArray(messageData) });
				intent.putExtra("format", "3gpp");
				startService(intent);
			}
		} else {
			ContentValues values = new ContentValues();
			values.put("address", "123456789");
			values.put("body", "Hey, dude!");
			getContentResolver().insert(Uri.parse("content://sms/inbox"), values);
		}
	}

	public byte[] hexStringToByteArray(String hexString) {
		int length = hexString.length();
		byte[] buffer = new byte[length / 2];
		for (int i = 0; i < length; i += 2) {
			buffer[i / 2] = (byte) ((toByte(hexString.charAt(i)) << 4) | toByte(hexString
					.charAt(i + 1)));
		}
		return buffer;
	}

	private int toByte(char c) {
		if (c >= '0' && c <= '9')
			return (c - '0');
		if (c >= 'A' && c <= 'F')
			return (c - 'A' + 10);
		if (c >= 'a' && c <= 'f')
			return (c - 'a' + 10);
		throw new RuntimeException("Invalid hex char '" + c + "'");
	}

	private void sendMessageAfterDelay() {
		try {
			debug("Waiting for " + interval + " seconds.");
			Thread.sleep(interval * 1000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		try {
			runSendingProcess();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

}