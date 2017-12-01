package com.phuket.tour.mediacodec.audio.encoder;

import java.io.IOException;
import java.nio.ByteBuffer;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.os.Build;
import android.util.Log;

interface OutputAACDelegate {
	public void outputAACPacket(byte[] data);
}

@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
public class AudioEncoder {
	private final static String MINE_TYPE = "audio/mp4a-latm";
	private MediaCodec mediaCodec;
	private ByteBuffer[] inputBuffers;
	private ByteBuffer[] outputBuffers;
	private OutputAACDelegate outputAACDelegate;

	public AudioEncoder(OutputAACDelegate outputAACDelegate, int sampleRate, int channels, int bitRate) throws IOException {
		this.outputAACDelegate = outputAACDelegate;
		MediaFormat encodeFormat = MediaFormat.createAudioFormat(MINE_TYPE, sampleRate, channels);
		encodeFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitRate);//比特率
		encodeFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
		encodeFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 10 * 1024);
		mediaCodec = MediaCodec.createEncoderByType(MINE_TYPE);
		mediaCodec.configure(encodeFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
		if (mediaCodec == null) {
			Log.e("problem", "create mediaEncode failed");
			return;
		}
		mediaCodec.start();
		inputBuffers = mediaCodec.getInputBuffers();
		outputBuffers = mediaCodec.getOutputBuffers();
	}

	@TargetApi(Build.VERSION_CODES.JELLY_BEAN)
	public void fireAudio(byte[] data, int len) {
		int inputBufferIndex = mediaCodec.dequeueInputBuffer(-1);
		if (inputBufferIndex >= 0) {
			ByteBuffer inputBuffer = inputBuffers[inputBufferIndex];
			inputBuffer.clear();
			inputBuffer.put(data);
			mediaCodec.queueInputBuffer(inputBufferIndex, 0, len, System.nanoTime(), 0);
		}
		MediaCodec.BufferInfo bufferInfo = new MediaCodec.BufferInfo();
		int outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
		while (outputBufferIndex >= 0) {
			ByteBuffer outputBuffer = outputBuffers[outputBufferIndex];
			if (outputAACDelegate != null) {
				int outPacketSize = bufferInfo.size + 7;// 7为ADTS头部的大小
				outputBuffer.position(bufferInfo.offset);
				outputBuffer.limit(bufferInfo.offset + bufferInfo.size);
				byte[] outData = new byte[outPacketSize];
				addADTStoPacket(outData, outPacketSize);//添加ADTS 代码后面会贴上
				outputBuffer.get(outData, 7, bufferInfo.size);//将编码得到的AAC数据 取出到byte[]中 偏移量offset=7
				outputBuffer.position(bufferInfo.offset);
				outputAACDelegate.outputAACPacket(outData);
			}
			mediaCodec.releaseOutputBuffer(outputBufferIndex, false);
			outputBufferIndex = mediaCodec.dequeueOutputBuffer(bufferInfo, 0);
		}
	}

	/**
	 * 添加ADTS头
	 * 
	 * @param packet
	 * @param packetLen
	 */
	private void addADTStoPacket(byte[] packet, int packetLen) {
		int profile = 2; // AAC LC
		int freqIdx = 4; // 44.1KHz
		int chanCfg = 2; // CPE
		// fill in ADTS data 70 
		packet[0] = (byte) 0xFF;
		packet[1] = (byte) 0xF9;
		packet[2] = (byte) (((profile - 1) << 6) + (freqIdx << 2) + (chanCfg >> 2));
		packet[3] = (byte) (((chanCfg & 3) << 6) + (packetLen >> 11));
		packet[4] = (byte) ((packetLen & 0x7FF) >> 3);
		packet[5] = (byte) (((packetLen & 7) << 5) + 0x1F);
		packet[6] = (byte) 0xFC;
	}

	public void stop() {
		if (null != mediaCodec) {
			mediaCodec.stop();
			mediaCodec.release();
		}
	}
}
