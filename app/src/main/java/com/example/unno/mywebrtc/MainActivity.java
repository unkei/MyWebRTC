package com.example.unno.mywebrtc;

import android.opengl.GLSurfaceView;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;


public class MainActivity extends ActionBarActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private GLSurfaceView glview;
    private String VIDEO_TRACK_ID = TAG + "VIDEO";
    private String AUDIO_TRACK_ID = TAG + "AUDIO";
    private String LOCAL_MEDIA_STREAM_ID = TAG + "STREAM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true, true);

        PeerConnectionFactory peerConnectionFactory = new PeerConnectionFactory();

        Log.i(TAG, "VideoCapturerAndroid.getDeviceCount() = " + VideoCapturerAndroid.getDeviceCount());
        String nameOfFrontFacingDevice = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        String nameOfBackFacingDevice = VideoCapturerAndroid.getNameOfBackFacingDevice();
        Log.i(TAG, "VideoCapturerAndroid.getNameOfFrontFacingDevice() = " + nameOfFrontFacingDevice);
        Log.i(TAG, "VideoCapturerAndroid.getNameOfBackFacingDevice() = " + nameOfBackFacingDevice);
        VideoCapturerAndroid capturer = VideoCapturerAndroid.create(nameOfFrontFacingDevice);

        MediaConstraints videoConstraints = new MediaConstraints();
//        MediaConstraints audioConstraints = new MediaConstraints();
        VideoSource videoSource = peerConnectionFactory.createVideoSource(capturer, videoConstraints);
        VideoTrack localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
//        AudioSource audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
//        AudioTrack localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);

        glview = (GLSurfaceView) findViewById(R.id.glview);
        VideoRendererGui.setView(glview, new Runnable() {
            @Override
            public void run() {

            }
        });
        try {
            VideoRenderer renderer = VideoRendererGui.createGui(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            localVideoTrack.addRenderer(renderer);
        } catch (Exception e) {
            e.printStackTrace();
        }


//        MediaStream mediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);
//        mediaStream.addTrack(localVideoTrack);
//        mediaStream.addTrack(localAudioTrack);
    }

    @Override
    protected void onResume() {
        super.onResume();
        glview.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        glview.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
