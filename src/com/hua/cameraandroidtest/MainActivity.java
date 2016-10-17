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
	private List<Size> supportSizeList = null;	//支持的分辨率集合

	// 音频获取源
	private int audioSource = MediaRecorder.AudioSource.MIC;
	// 设置音频采样率，44100是目前的标准，但是某些设备仍然支持22050，16000，11025
	private static int sampleRateInHz = 44100;
	// 设置音频的录制的声道CHANNEL_IN_STEREO为双声道，CHANNEL_CONFIGURATION_MONO为单声道
	private static int channelConfig = AudioFormat.CHANNEL_CONFIGURATION_MONO;
	// 音频数据格式:PCM 16位每个样本。保证设备支持。PCM 8位每个样本。不一定能得到设备支持。
	private static int audioFormat = AudioFormat.ENCODING_PCM_16BIT;
	// 缓冲区字节大小
	private int bufferSizeInBytes = 0;
	// 开始按钮
	private Button Start;
	// 结束按钮
	private Button Stop;
	private AudioRecord audioRecord;
	private boolean isRecord = false;// 设置正在录制的状态
	
	
	/**
	 *写入文件数据的类型，一个video，一个audio;
	 */
	private final int VIDEO_TYPE = 1;
	private final int AUDIO_TYPE = 2;

	/**
	 * 创建启动
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
			 * 开始启动编码视频
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
			 * 结束编码视频
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
	 * 创建音频记录器
	 */
	private void creatAudioRecord() {
		// 获得缓冲区字节大小
		bufferSizeInBytes = AudioRecord.getMinBufferSize(sampleRateInHz,
				channelConfig, audioFormat);
		// 创建AudioRecord对象
		audioRecord = new AudioRecord(audioSource, sampleRateInHz,
				channelConfig, audioFormat, bufferSizeInBytes);
	}

	/**
	 * 开始音频编码记录
	 */
	private void startRecord() {
		audioRecord.startRecording();
		// 让录制状态为true
		isRecord = true;
		// 开启音频文件写入线程
		new Thread(new AudioRecordThread()).start();
	}

	/**
	 * 音频编码线程
	 * 
	 * @author zhanghua
	 * 
	 */
	class AudioRecordThread implements Runnable {
		@Override
		public void run() {
			writeDateTOFile();// 往文件中写入裸数据
		}
	}

	/**
	 * 这里将数据写入文件，但是并不能播放，因为AudioRecord获得的音频是原始的裸音频，
	 * 如果需要播放就必须加入一些格式或者编码的头信息。但是这样的好处就是你可以对音频的 裸数据进行处理，比如你要做一个爱说话的TOM
	 * 猫在这里就进行音频的处理，然后重新封装 所以说这样得到的音频比较容易做一些音频的处理。
	 */
	private void writeDateTOFile() {
		// new一个byte数组用来存一些字节数据，大小为缓冲区大小
		short[] audiodata = new short[bufferSizeInBytes];
		int readsize = 0;
		while (isRecord == true && mIsStartPre == true) {
			readsize = audioRecord.read(audiodata, 0, bufferSizeInBytes);
			if (AudioRecord.ERROR_INVALID_OPERATION != readsize) {
				// 在这里讲audiodata数据通过ffmpeg接口写数据;
				writeData(null, AUDIO_TYPE,audiodata);

			}
		}
	}

	/**
	 * 结束音频编码，这里仅仅是关闭音频记录器，没有移除音频记录器
	 */
	private void stopRecord() {
		// close();
		if (audioRecord != null) {
			System.out.println("stopRecord");
			isRecord = false;// 停止文件写入
			audioRecord.stop();
		}
	}

	/**
	 * 关闭音频记录器，并且移除，相当于系统退出的时候调用
	 */
	private void close() {
		if (audioRecord != null) {
			System.out.println("closeRecord");
			isRecord = false;// 停止文件写入
			audioRecord.stop();
			audioRecord.release();// 释放资源
			audioRecord = null;
		}
	}

	/**
	 * 退出
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
	 * 暂停
	 */
	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

	/**
	 * 唤醒
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
	 * surfaceview变换的时候系统回调
	 */
	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		// TODO Auto-generated method stub
		Camera.Parameters p = camera.getParameters();
		p.setPreviewSize(352, 288);
		p.setPictureFormat(PixelFormat.JPEG); // Sets the image format for
												// picture 设定相片格式为JPEG，默认为NV21
		p.setPreviewFormat(PixelFormat.YCbCr_420_SP); // Sets the image format
														// for preview
														// picture，默认为NV21
		// p.setRotation(90);
		// p.setPreviewFrameRate(25);
		camera.setPreviewCallback(new PreviewCallback() {

			/**
			 * 摄像头预览场景，获取每一帧数据，传送给handler
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
	 * surfaceview创建
	 */
	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		// TODO Auto-generated method stu
		camera = Camera.open();
	}

	/**
	 * surfaceview销毁
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
	 * 摄像头聚焦
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
	 * Handler处理各种与界面相关的事物
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
	 * 视频存储接口，每一帧都会调用该接口
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
	 * 真正的数据写入接口
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
	 * 本地方法初始化接口
	 * 
	 * @param filename
	 * @return
	 */
	private native int videoinit(byte[] filename);

	/**
	 * 本地接口写入每一帧数据
	 * 
	 * @param videodataoraudiodata
	 * @param type
	 * @return
	 */
	// /type video是1；audio是2；
	private native int videostart(byte[] videodataoraudiodata, int type,short[] audiodata);
	
	private native int audiostart(short[] audiodata,int type);

	/**
	 * 本地接口，结束写入操作
	 * 
	 * @return
	 */
	private native int videoclose();

	/**
	 * 通过JNI技术导入两个库，ffmpeg和ffmpeg_encoder_jni
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
	 * 开始录制——设置属性
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
		// 设置音频和视频来源
		mediarecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
		mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		// 设置输出格式，3GP.MP4,RAM是视频格式 AAC,AMR,RM是音频格式.
		mediarecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
		String parentPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/mycapture/video/temp";
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");// 锟斤拷锟斤拷锟斤拷锟节革拷式
		String path = parentPath + "/" + format.format(new Date())+".mp4";
        mediarecorder.setOutputFile(path);
		// 单个视频最大字节数
		mediarecorder.setMaxFileSize(1*1024*1024*1024);
		// 最长多少ms
		//mediarecorder.setMaxDuration(1000);
		//设置监听
		mediarecorder.setOnInfoListener(new OnInfoListener() {			
			@Override
			public void onInfo(MediaRecorder mr, int what, int extra) {
				// TODO Auto-generated method stub
				if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED) {
					Toast.makeText(getApplicationContext(), "文件已到达最大限制大小了!", Toast.LENGTH_LONG).show();
					stopRecord();
				} else if(what == MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED){
					Toast.makeText(getApplicationContext(), "文件已到达最大限制时长了!", Toast.LENGTH_LONG).show();
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
			//若上面出错，按最高的质量录制
			profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);	
		}		
		// 编码格式
		mediarecorder.setAudioEncoder(profile.audioCodec);
		mediarecorder.setVideoEncoder(profile.videoCodec);
		// 设置视频编码录音比特率
		mediarecorder.setVideoEncodingBitRate(profile.videoBitRate);
		// 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
		mediarecorder.setVideoFrameRate(profile.videoFrameRate);
		// 设置录制的音频通道数
		mediarecorder.setAudioChannels(profile.audioChannels);
		// 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
		mediarecorder.setVideoSize(optimalSize.width, optimalSize.height);
		// 设置音频编码录音比特率
		mediarecorder.setAudioEncodingBitRate(profile.audioBitRate);
		// 设置音频采样率
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
	 * 计算合适的分辨率
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
		mediarecorder = new MediaRecorder();// 创建mediarecorder对象 
		mediarecorder.reset();
        // 设置录制视频源为Camera(相机)
		mediarecorder.setCamera(camera);
		mediarecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
        mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);  
        // 设置录制完成后视频的封装格式THREE_GPP为3gp.MPEG_4为mp4  
        mediarecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);  
        // 设置录制的视频编码h263 h264  
        mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
        // 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        mediarecorder.setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
        //mediarecorder.setVideoSize(480, 360);  
        // 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错  
        //mediarecorder.setVideoFrameRate(25); 
        mediarecorder.setPreviewDisplay(sView.getHolder().getSurface());
        //mediarecorder.setMaxDuration(1000);
        // 设置视频文件输出的路径 
        String parentPath = Environment.getExternalStorageDirectory()
				.getAbsolutePath() + "/mycapture/video/temp";
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");// 锟斤拷锟斤拷锟斤拷锟节革拷式
		String path = parentPath + "/" + format.format(new Date())+".mp4";
        mediarecorder.setOutputFile(path);
        //CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        //mediarecorder.setAudioEncoder(profile.audioCodec);
        //mediarecorder.setVideoEncoder(profile.videoCodec);
        
        // 设置视频编码录音比特率
        //mediarecorder.setVideoEncodingBitRate(profile.videoBitRate);
     	// 设置录制的视频帧率。必须放在设置编码和格式的后面，否则报错
        //mediarecorder.setVideoFrameRate(profile.videoFrameRate);
     	// 设置录制的音频通道数
        //mediarecorder.setAudioChannels(profile.audioChannels);
     	// 设置视频录制的分辨率。必须放在设置编码和格式的后面，否则报错
        //mediarecorder.setVideoSize(480,360);
     	// 设置音频编码录音比特率
     	//mediarecorder.setAudioEncodingBitRate(profile.audioBitRate);
     	// 设置音频采样率
     	//mediarecorder.setAudioSamplingRate(profile.audioSampleRate);
        
        
        
        try {  
            // 准备录制  
            mediarecorder.prepare();  
            // 开始录制  
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
		              SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd-HHmmss");// 锟斤拷锟斤拷锟斤拷锟节革拷式
		      		String path = parentPath + "/" + format.format(new Date())+".mp4";
		              File file = new File(path);
		              if (file.exists()) {
		                  // 如果文件存在，删除它，演示代码保证设备上只有一个录音文件
		                  file.delete();
		              }
		              camera.unlock();
		              mediarecorder = new MediaRecorder();
		              mediarecorder.reset();
		              // 设置音频录入源
		              mediarecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
		              // 设置视频图像的录入源
		              mediarecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
		              // 设置录入媒体的输出格式
		              mediarecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
		              // 设置音频的编码格式
		              mediarecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);
		              // 设置视频的编码格式
		              mediarecorder.setVideoEncoder(MediaRecorder.VideoEncoder.MPEG_4_SP);
		              // 设置视频的采样率，每秒4帧
		              //mediarecorder.setVideoFrameRate(4);
		              // 设置录制视频文件的输出路径
		              mediarecorder.setOutputFile(file.getAbsolutePath());
		              // 设置捕获视频图像的预览界面
		              mediarecorder.setPreviewDisplay(sView.getHolder().getSurface());
		              
		              mediarecorder.setOnErrorListener(new OnErrorListener() {
		                  
		                  @Override
		                  public void onError(MediaRecorder mr, int what, int extra) {
		                      // 发生错误，停止录制
		                      mediarecorder.stop();
		                      mediarecorder.release();
		                      mediarecorder = null;
		                      camera.lock();

		                      Toast.makeText(MainActivity.this, "录制出错", 0).show();
		                  }
		              });
		              
		             // 准备、开始
		             mediarecorder.prepare();
		             mediarecorder.start();
		 
		             Toast.makeText(MainActivity.this, "开始录像", 0).show();
		         } catch (Exception e) {
		             e.printStackTrace();
		         }
		 
		     }
}
