# camera_android_ffmpeg

Android设备摄像头采集数据，通过ffmepg进行编码，采用x264编码格式变成mp4格式视频。




加载编译好的两个库，在ffmpeg_encoder_jni库中定义了三个native接口，这里需要调用。
打开摄像头，点击开始就是调用videoinit，开始初始化编码环境。
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
这个回调接口就是将每一帧的画面数据截取丢给ffmpeg处理。我们发送消息给handler，处理，实际上就是调用videostart处理，这个函数就是将每一帧的数据丢给ffmpeg编码处理。
