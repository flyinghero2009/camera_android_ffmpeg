package com.hua.cameraandroidtest;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.Size;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaRecorder.OnErrorListener;
import android.media.MediaRecorder.OnInfoListener;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.text.format.Formatter;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends Activity implements Callback, PictureCallback {

	SurfaceView sView;
	SurfaceHolder surfaceHolder;
	RelativeLayout mButtonsLayout;
	RelativeLayout mMainLayout;
	Button mStartButton, mStopButton;
	ButtonsHandler mHandler;
	Camera camera;
	double mVisibityTime;
	boolean mIsVisibity;
	boolean mIsStartPre;
	final int MSG_CHECK_PROESS = 10001;// "msg_check_proess";
	final int MSG_CHECK_TOUCH = 10002;// "msg_check_touch";
	final int MSG_WRITE_YUVDATA = 10003;
	
	private MediaRecorder mediarecorder;
	private List<Size> supportSizeList = null;	//֧�ֵķֱ��ʼ���

	// ��Ƶ��ȡԴ
	private int audioSource = MediaRecorder.AudioSource.MIC;
	// ������Ƶ�����ʣ�44100��Ŀǰ�ı�׼������ĳЩ�豸��Ȼ֧��22050��16000��11025
	private static int sampleRateInHz = 44100;
	// ������Ƶ��¼�Ƶ�����CHANNEL_IN_STEREOΪ˫������CHANNEL_CONFIGURATION_MONOΪ������
	private static int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	// ��Ƶ���ݸ�ʽ:PCM 16λÿ����������֤�豸֧�֡�PCM 8λÿ����������һ���ܵõ��豸֧�֡�
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	// �������ֽڴ�С
	private int bufferSizeInBytes = 0;
	// ��ʼ��ť
	private Button Start;
	// ������ť
	private Button Stop;
	private AudioRecord audioRecord;
	private boolean isRecord = false;// ��������¼�Ƶ�״̬
	
	
	/**
	 *д���ļ����ݵ����ͣ�һ��video��һ��audio;
	 */
	private final int VIDEO_TYPE = 1;
	private final int AUDIO_TYPE = 2;

	/**
	 * ��������
	 * 
	 * @param Bundle
	 *            savedInstanceState
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		getWindow().setFormat(PixelFormat.TRANSLUCENT);
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.mainlayout);

		sView = (SurfaceView) this.findViewById(R.id.surfaceid);
		sView.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View arg0, MotionEvent arg1) {
				// TODO Auto-generated method stub
				mHandler.sendEmptyMessage(MSG_CHECK_TOUCH);
				AutoFocus();
				return false;
			}

		});
		mButtonsLayout = (RelativeLayout) this.findViewById(R.id.buttonsid);
		mStartButton = (Button) this.findViewById(R.id.button1);
		mStartButton.setOnClickListener(new OnClickListener() {

			/**
			 * ��ʼ����������Ƶ
			 */
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				startRecording();
				//start();
				/*
				if (mIsStartPre == false) {
					mIsStartPre = true;
					AutoFocus();
					Calendar cc = Calendar.getInstance();
					cc.setTimeInMillis(System.currentTimeMillis());
					String filename = Environment.getExternalStorageDirectory()
							.getAbsolutePath()
							+ "/"
							+ String.valueOf(cc.get(Calendar.YEAR))
							+ "-"
							+ String.valueOf(cc.get(Calendar.MONTH))
							+ "-"
							+ String.valueOf(cc.get(Calendar.DAY_OF_YEAR))
							+ "-"
							+ String.valueOf(cc.get(Calendar.HOUR_OF_DAY))
							+ "-"
							+ String.valueOf(cc.get(Calendar.MINUTE))
							+ "-"
							+ String.valueOf(cc.get(Calendar.SECOND))
							+ ".mp4";
					videoinit(filename.getBytes());
					startRecord();
				}
				*/

			}

		});
		mStopButton = (Button) this.findViewById(R.id.button2);
		mStopButton.setOnClickListener(new OnClickListener() {

			/**
			 * ����������Ƶ
			 */
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				stopRecord1();
				/*
				if (mIsStartPre == true) {
					mIsStartPre = false;
					videoclose();
					stopRecord1();

				}
				*/

			}

		});
		mIsStartPre = false;
		surfaceHolder = sView.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		mHandler = new ButtonsHandler();
		mHandler.sendEmptyMessage(MSG_CHECK_PROESS);
		mHandler.sendEmptyMessage(MSG_CHECK_TOUCH);
		//creatAudioRecord();
		String a1 = Util.getSDAvailableSize(this.getApplicationContext());
		String a2 = Util.getSDTotalSize(this.getApplicationContext());
		String a3 = Util.getRomAvailableSize(this.getApplicationContext());
		String a4 = Util.getRomTotalSize(this.getApplicationContext());
	}

	/**
	 * ������Ƶ��¼��
	 */
	private void creatAudioRecord() {
		// ��û������ֽڴ�С
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
				channelConfig, audioFormat);
		// ����AudioRecord����
		audioRecord = new AudioRecord(audioSource, sampleRateInHz,
				channelConfig, audioFormat, bufferSizeInBytes);
	}

	/**
	 * ��ʼ��Ƶ�����¼
	 */
	private void startRecord() {
		audioRecord.startRecording();
		// ��¼��״̬Ϊtrue
		isRecord = true;
		// ������Ƶ�ļ�д���߳�
		new Thread(new AudioRecordThread()).start();
	}

	/**
	 * ��Ƶ�����߳�
	 * 
	 * @author zhanghua
	 * 
	 */
	class AudioRecordThread implements Runnable {
		@Override
		public void run() {
			writeDateTOFile();// ���ļ���д��������
		}
	}

	/**
	 * ���ｫ����д���ļ������ǲ����ܲ��ţ���ΪAudioRecord��õ���Ƶ��ԭʼ������Ƶ��
	 * �����Ҫ���žͱ������һЩ��ʽ���߱����ͷ��Ϣ�����������ĺô���������Զ���Ƶ�� �����ݽ��д���������Ҫ��һ����˵����TOM
	 * è������ͽ�����Ƶ�Ĵ���Ȼ�����·�װ ����˵�����õ�����Ƶ�Ƚ�������һЩ��Ƶ�Ĵ���
	 */
	private void writeDateTOFile() {
		// newһ��byte����������һЩ�ֽ����ݣ���СΪ��������С
		short[] audiodata = new short[bufferSizeInBytes];
		int readsize = 0;
		while (isRecord == true && mIsStartPre == true) {
			readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
			if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
				// �����ｲaudiodata����ͨ��ffmpeg�ӿ�д����;
				writeData(null, AUDIO_TYPE,audiodata);

			}
		}
	}

	/**
	 * ������Ƶ���룬��������ǹر���Ƶ��¼����û���Ƴ���Ƶ��¼��
	 */
	private void stopRecord() {
		// close();
		if (audioRecord != null) {
			System.out.println("stopRecord");
			isRecord = false;// ֹͣ�ļ�д��
			audioRecord.stop();
		}
	}

	/**
	 * �ر���Ƶ��¼���������Ƴ����൱��ϵͳ�˳���ʱ�����
	 */
	private void close() {
		if (audioRecord != null) {
			System.out.println("closeRecord");
			isRecord = false;// ֹͣ�ļ�д��
			audioRecord.stop();
			audioRecord.release();// �ͷ���Դ
			audioRecord = null;
		}
	}

	/**
	 * �˳�
	 */
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		close();
		if (mIsStartPre == true) {
			mIsStartPre = false;
			videoclose();

		}
	}

	/**
	 * ��ͣ
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	/**
	 * ����
	 */
	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
	}

	/**
	 * 
	 */
	@Override
	public void onPictureTaken(byte[] arg0, Camera arg1) {
		// TODO Auto-generated method stub

	}

	/**
	 * surfaceview�任��ʱ��ϵͳ�ص�
	 */
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		Camera.Parameters p = camera.getParameters();
		p.setPreviewSize(352, 288);
		p.setPictureFormat(PixelFormat.JPEG); // Sets the image format for
												// picture �趨��Ƭ��ʽΪJPEG��Ĭ��ΪNV21
		p.setPreviewFormat(PixelFormat.YCbCr_420_SP); // Sets the image format
														// for preview
														// picture��Ĭ��ΪNV21
		// p.setRotation(90);
		// p.setPreviewFrameRate(25);
		camera.setPreviewCallback(new PreviewCallback() {

			/**
			 * ����ͷԤ����������ȡÿһ֡���ݣ����͸�handler
			 * 
			 * @author zhanghua
			 */
			@Override
			public void onPreviewFrame(byte[] arg0, Camera arg1) {
				// TODO Auto-generated method stub
				if (mIsStartPre == true) {
					Message msg = new Message();
					Bundle bl = new Bundle();
					bl.putByteArray("messageyuvdata", arg0);
					msg.setData(bl);
					msg.what = MSG_WRITE_YUVDATA;
					mHandler.sendMessage(msg);
				}
			}

		});
		camera.setParameters(p);
		try {
			camera.setPreviewDisplay(surfaceHolder);
		} catch (Exception E) {

		}
		camera.startPreview();
	}

	/**
	 * surfaceview����
	 */
	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO Auto-generated method stu
		camera = Camera.open();
	}

	/**
	 * surfaceview����
	 */
	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		// TODO Auto-generated method stub
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
		}
	}

	/**
	 * ����ͷ�۽�
	 */
	public void AutoFocus() {
		if (camera != null) {
			camera.autoFocus(new AutoFocusCallback() {

				@Override
				public void onAutoFocus(boolean arg0, Camera arg1) {
					// TODO Auto-generated method stub
					//
				}

			});
		}
	}

	/**
	 * Handler��������������ص�����
	 * 
	 * @author Administrator
	 * 
	 */
	@SuppressLint("HandlerLeak")
	class ButtonsHandler extends Handler {

		public ButtonsHandler() {
			super();
		}

		public void handleMessage(android.os.Message msg) {
			switch (msg.what) {
			case MSG_CHECK_PROESS:
				if (mIsVisibity
						&& (System.currentTimeMillis() - mVisibityTime > 7000)) {
					mButtonsLayout.setVisibility(View.INVISIBLE);
					// mLinerLaryout.setFocusable(false);
					mIsVisibity = false;
					mVisibityTime = 0;
				}
				sendEmptyMessageDelayed(MSG_CHECK_PROESS, 500);

				break;
			case MSG_CHECK_TOUCH:
				if (mButtonsLayout.getVisibility() != View.VISIBLE) {
					mButtonsLayout.setVisibility(View.VISIBLE);
					// mLinerLaryout.setFocusable(true);

				}
				mIsVisibity = true;
				mVisibityTime = System.currentTimeMillis();
				break;
			case MSG_WRITE_YUVDATA:
				byte[] bytedata = msg.getData().getByteArray("messageyuvdata");
				if (bytedata != null) {
					addVideoData(bytedata);
				}
				break;
			}
		};
	};

	/**
	 * ��Ƶ�洢�ӿڣ�ÿһ֡������øýӿ�
	 * 
	 * @param data
	 */
	public void addVideoData(byte[] data) {
		byte[] yuv420 = new byte[data.length];
		if (data == null || yuv420 == null)
			return;
		int framesize = 352 * 288;
		int i = 0, j = 0;
		// copy y
		for (i = 0; i < framesize; i++) {
			yuv420[i] = data[i];
		}
		i = 0;
		for (j = 0; j < framesize / 2; j += 2) {
			yuv420[i + framesize * 5 / 4] = data[j + framesize];
			i++;
		}
		i = 0;
		for (j = 1; j < framesize / 2; j += 2) {
			yuv420[i + framesize] = data[j + framesize];
			i++;
		}
		writeData(yuv420, VIDEO_TYPE,null);
	}

	/**
	 * ����������д��ӿ�
	 * 
	 * @param data
	 * @param type
	 */
	private synchronized void writeData(byte[] data, int type,short[] audiodata) {
		switch(type)
		{
		case VIDEO_TYPE:
			videostart(data, type,new short[1]);
			break;
		case AUDIO_TYPE:
			videostart(new byte[1],type,audiodata);
			break;
		}
	}

	/**
	 * ���ط�����ʼ���ӿ�
	 * 
	 * @param filename
	 * @return
	 */
	private native int videoinit(byte[] filename);

	/**
	 * ���ؽӿ�д��ÿһ֡����
	 * 
	 * @param videodataoraudiodata
	 * @param type
	 * @return
	 */
	// /type video��1��audio��2��
	private native int videostart(byte[] videodataoraudiodata, int type,short[] audiodata);
	
	private native int audiostart(short[] audiodata,int type);

	/**
	 * ���ؽӿڣ�����д�����
	 * 
	 * @return
	 */
	private native int videoclose();

	/**
	 * ͨ��JNI�������������⣬ffmpeg��ffmpeg_encoder_jni
	 */
	static {
		 //System.loadLibrary("avcodec-56");
		 //System.loadLibrary("avdevice-56");
		 //System.loadLibrary("avfilter-5");
		 //System.loadLibrary("avformat-56");
		 //System.loadLibrary("avutil-54");
		 //System.loadLibrary("postproc-53");
		 //System.loadLibrary("swresample-1");
		 //System.loadLibrary("swscale-3");
		//System.loadLibrary("ffmpeg");
		System.loadLibrary("ffmpeg_encoder_jni");
	}
	
	
	
	
	/**
	 * ��ʼ¼�ơ�����������
	 * @return
	 */
	private boolean startRecording() {
		if (camera != null) {
			Camera.Parameters params = camera.getParameters();
			params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			camera.setParameters(params);
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB)
				supportSizeList = params.getSupportedVideoSizes();
			if(supportSizeList == null)
				supportSizeList = params.getSupportedPreviewSizes();
		}
		camera.unlock();
		 if (mediarecorder == null) {
			 mediarecorder = new MediaRecorder();
			 //mediarecorder.setOnErrorListener(this);
         } else {
        	 mediarecorder.reset();
         }
	
		mediarecorder.setCamera(camera);
		// ������Ƶ����Ƶ��Դ
		mediarecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		// ���������ʽ��3GP.MP4,RAM����Ƶ��ʽ AAC,AMR,RM����Ƶ��ʽ.
		mediarecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
		String parentPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/mycapture/video/temp";
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");// �������ڸ�ʽ
		String path = parentPath + "/" + format.format(new Date())+".mp4";
        mediarecorder.setOutputFile(path);
		// ������Ƶ����ֽ���
		mediarecorder.setMaxFileSize(1*1024*1024*1024);
		// �����ms
		//mediarecorder.setMaxDuration(1000);
		//���ü���
		mediarecorder.setOnInfoListener(new OnInfoListener() {			
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				// TODO Auto-generated method stub
				if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					Toast.makeText(getApplicationContext(), "�ļ��ѵ���������ƴ�С��!", Toast.LENGTH_LONG).show();
					stopRecord();
				} else if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
					Toast.makeText(getApplicationContext(), "�ļ��ѵ����������ʱ����!", Toast.LENGTH_LONG).show();
					stopRecord();
				}
			}
		});
		CamcorderProfile profile = null;		
		mediarecorder.setOnErrorListener(new OnErrorListener() {
			
			@Override
			public void onError(MediaRecorder mr, int what, int extra) {
				// TODO Auto-generated method stub
			}
		});
		mediarecorder.setOnInfoListener(new OnInfoListener() {
			
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				// TODO Auto-generated method stub
			}
		});
		Size optimalSize = null;
		try{
			optimalSize = getOptimalPreviewSize(supportSizeList, 720, 480);
			profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
		} catch(Exception e) {
			e.printStackTrace();
			//�������������ߵ�����¼��
			profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);	
		}		
		// �����ʽ
		mediarecorder.setAudioEncoder(profile.audioCodec);
		mediarecorder.setVideoEncoder(profile.videoCodec);
		// ������Ƶ����¼��������
		mediarecorder.setVideoEncodingBitRate(profile.videoBitRate);
		// ����¼�Ƶ���Ƶ֡�ʡ�����������ñ���͸�ʽ�ĺ��棬���򱨴�
		mediarecorder.setVideoFrameRate(profile.videoFrameRate);
		// ����¼�Ƶ���Ƶͨ����
		mediarecorder.setAudioChannels(profile.audioChannels);
		// ������Ƶ¼�Ƶķֱ��ʡ�����������ñ���͸�ʽ�ĺ��棬���򱨴�
		mediarecorder.setVideoSize(optimalSize.width, optimalSize.height);
		// ������Ƶ����¼��������
		mediarecorder.setAudioEncodingBitRate(profile.audioBitRate);
		// ������Ƶ������
		mediarecorder.setAudioSamplingRate(profile.audioSampleRate);
		mediarecorder.setPreviewDisplay(sView.getHolder().getSurface());
		try {
			mediarecorder.prepare();
			mediarecorder.start();
			return true;
		} catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			mediarecorder = null;
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			mediarecorder = null;
			return false;
		} catch(Exception e) {
			e.printStackTrace();
			mediarecorder = null;
			camera.lock();
			return false;
		}
	}
	
	
	
	
	/**
	 * ������ʵķֱ���
	 * @param sizes
	 * @param w
	 * @param h
	 * @return
	 */
	private Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        double targetRatio=(double)h / w;

        if (sizes == null) return null;

        Camera.Size optimalSize = null;
        double minDiff = Double.MAX_VALUE;

        int targetHeight = h;

        for (Camera.Size size : sizes) {
            double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) continue;
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (Camera.Size size : sizes) {
                if (Math.abs(size.height - targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }
        return optimalSize;
    }
	
	
	private void stopRecord1(){
		if(mediarecorder != null){
			mediarecorder.stop();
			mediarecorder.release();
			mediarecorder = null;
			camera.lock();
		}
	}
	
	
	private void startRecord1(){
		camera.unlock();
		if(mediarecorder != null)
			return;
		mediarecorder = new MediaRecorder();// ����mediarecorder���� 
		mediarecorder.reset();
        // ����¼����ƵԴΪCamera(���)
		mediarecorder.setCamera(camera);
		mediarecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);  
        // ����¼����ɺ���Ƶ�ķ�װ��ʽTHREE_GPPΪ3gp.MPEG_4Ϊmp4  
        mediarecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);  
        // ����¼�Ƶ���Ƶ����h263 h264  
        mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        // ������Ƶ¼�Ƶķֱ��ʡ�����������ñ���͸�ʽ�ĺ��棬���򱨴�
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        mediarecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        //mediarecorder.setVideoSize(480, 360);  
        // ����¼�Ƶ���Ƶ֡�ʡ�����������ñ���͸�ʽ�ĺ��棬���򱨴�  
        //mediarecorder.setVideoFrameRate(25); 
        mediarecorder.setPreviewDisplay(sView.getHolder().getSurface());
        //mediarecorder.setMaxDuration(1000);
        // ������Ƶ�ļ������·�� 
        String parentPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/mycapture/video/temp";
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");// �������ڸ�ʽ
		String path = parentPath + "/" + format.format(new Date())+".mp4";
        mediarecorder.setOutputFile(path);
        //CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        //mediarecorder.setAudioEncoder(profile.audioCodec);
        //mediarecorder.setVideoEncoder(profile.videoCodec);
        
        // ������Ƶ����¼��������
        //mediarecorder.setVideoEncodingBitRate(profile.videoBitRate);
     	// ����¼�Ƶ���Ƶ֡�ʡ�����������ñ���͸�ʽ�ĺ��棬���򱨴�
        //mediarecorder.setVideoFrameRate(profile.videoFrameRate);
     	// ����¼�Ƶ���Ƶͨ����
        //mediarecorder.setAudioChannels(profile.audioChannels);
     	// ������Ƶ¼�Ƶķֱ��ʡ�����������ñ���͸�ʽ�ĺ��棬���򱨴�
        //mediarecorder.setVideoSize(480,360);
     	// ������Ƶ����¼��������
     	//mediarecorder.setAudioEncodingBitRate(profile.audioBitRate);
     	// ������Ƶ������
     	//mediarecorder.setAudioSamplingRate(profile.audioSampleRate);
        
        
        
        try {  
            // ׼��¼��  
            mediarecorder.prepare();  
            // ��ʼ¼��  
            mediarecorder.start();  
        } catch (IllegalStateException e) {
			// TODO Auto-generated catch block
			mediarecorder = null;
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			mediarecorder = null;
		} catch(Exception e) {
			e.printStackTrace();
			mediarecorder = null;
			camera.lock();
		}  
	}
	
	
	
	
	protected void start() {
		          try {
		        	  String parentPath = Environment.getExternalStorageDirectory()
		      				.getAbsolutePath() + "/mycapture/video/temp";
		              SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");// �������ڸ�ʽ
		      		String path = parentPath + "/" + format.format(new Date())+".mp4";
		              File file = new File(path);
		              if (file.exists()) {
		                  // ����ļ����ڣ�ɾ��������ʾ���뱣֤�豸��ֻ��һ��¼���ļ�
		                  file.delete();
		              }
		              camera.unlock();
		              mediarecorder = new MediaRecorder();
		              mediarecorder.reset();
		              // ������Ƶ¼��Դ
		              mediarecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		              // ������Ƶͼ���¼��Դ
		              mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		              // ����¼��ý��������ʽ
		              mediarecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		              // ������Ƶ�ı����ʽ
		              mediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		              // ������Ƶ�ı����ʽ
		              mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
		              // ������Ƶ�Ĳ����ʣ�ÿ��4֡
		              //mediarecorder.setVideoFrameRate(4);
		              // ����¼����Ƶ�ļ������·��
		              mediarecorder.setOutputFile(file.getAbsolutePath());
		              // ���ò�����Ƶͼ���Ԥ������
		              mediarecorder.setPreviewDisplay(sView.getHolder().getSurface());
		              
		              mediarecorder.setOnErrorListener(new OnErrorListener() {
		                  
		                  @Override
		                  public void onError(MediaRecorder mr, int what, int extra) {
		                      // ��������ֹͣ¼��
		                      mediarecorder.stop();
		                      mediarecorder.release();
		                      mediarecorder = null;
		                      camera.lock();

		                      Toast.makeText(MainActivity.this, "¼�Ƴ���", 0).show();
		                  }
		              });
		              
		             // ׼������ʼ
		             mediarecorder.prepare();
		             mediarecorder.start();
		 
		             Toast.makeText(MainActivity.this, "��ʼ¼��", 0).show();
		         } catch (Exception e) {
		             e.printStackTrace();
		         }
		 
		     }
}
