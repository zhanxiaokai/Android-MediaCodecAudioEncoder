package com.phuket.tour.mediacodec.audio.encoder;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity implements OutputAACDelegate {

	private Button encode_btn;

	private FileInputStream inputStream = null;
	private String pcmFilePath = "/mnt/sdcard/vocal.pcm";
	private FileOutputStream outputStream = null;
	private String aacFilePath = "/mnt/sdcard/vocal_mediacodec.aac";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		encode_btn = (Button) findViewById(R.id.encode_btn);
		encode_btn.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				encode();
			}
		});
	}

	private void encode() {
		long startTimeMills = System.currentTimeMillis();
		AudioEncoder audioEncoder = null;
		try {
			audioEncoder = new AudioEncoder(this, 44100, 2, 128 * 1024);
			inputStream = new FileInputStream(pcmFilePath);
			outputStream = new FileOutputStream(aacFilePath);
			int bufferSize = 1024 * 256;
			byte[] buffer = new byte[bufferSize];
			int encodeBufferSize = 1024 * 10;
			byte[] encodeBuffer = new byte[encodeBufferSize];
			int len = -1;
			while ((len = inputStream.read(buffer)) > 0) {
				int offset = 0;
				while(offset < len) {
					int encodeBufferLenth = Math.min(len - offset, encodeBufferSize);
					System.arraycopy(buffer, offset, encodeBuffer, 0, encodeBufferLenth);
					audioEncoder.fireAudio(encodeBuffer, encodeBufferLenth);
					offset+=encodeBufferLenth;
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (null != inputStream) {
				try {
					inputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(null != outputStream) {
				try {
					outputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		audioEncoder.stop();
		int wasteTimeMills = (int)(System.currentTimeMillis() - startTimeMills);
		Log.i("success", "wasteTimeMills is : " + wasteTimeMills);
	}

	@Override
	public void outputAACPacket(byte[] data) {
		try {
			outputStream.write(data);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
