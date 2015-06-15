package com.example.unno.mywebrtc;

import android.opengl.GLSurfaceView;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;
import org.webrtc.AudioSource;
import org.webrtc.AudioTrack;
import org.webrtc.DataChannel;
import org.webrtc.IceCandidate;
import org.webrtc.MediaConstraints;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.PeerConnection;
import org.webrtc.SdpObserver;
import org.webrtc.SessionDescription;
import org.webrtc.VideoCapturerAndroid;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;
import org.webrtc.VideoSource;
import org.webrtc.VideoTrack;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import de.tavendo.autobahn.WebSocket;
import de.tavendo.autobahn.WebSocket.WebSocketConnectionObserver;
import de.tavendo.autobahn.WebSocketConnection;
import de.tavendo.autobahn.WebSocketException;


public class MainActivity extends ActionBarActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private GLSurfaceView glview;
    private String VIDEO_TRACK_ID = TAG + "VIDEO";
    private String AUDIO_TRACK_ID = TAG + "AUDIO";
    private String LOCAL_MEDIA_STREAM_ID = TAG + "STREAM";
    private MediaStream mediaStream;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initWebRTC();
        connect();

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


        mediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);
        mediaStream.addTrack(localVideoTrack);
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

    // webrtc
    PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private MediaConstraints pcConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints audioConstraints;
    private MediaConstraints mediaConstraints;
    private SDPObserver sdpObserver = new SDPObserver();

    private WebSocketConnection ws;
    private String wsServerUrl;
    private boolean peerStarted = false;

    private void initWebRTC() {
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true, true);

        peerConnectionFactory = new PeerConnectionFactory();

        pcConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();
        audioConstraints = new MediaConstraints();
        mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
    }

    private void connect() {
//        sigConnect("ws://10.54.36.19:9001/");
        sigConnect("ws://unwebrtc.herokuapp.com/");
    }

    private void hangUp() {
        stop();
    }

    private void stop() {
        peerConnection.close();
        peerConnection = null;
        peerStarted = false;
    }
    // connection handling
    private PeerConnection prepareNewConnection() {
        List<PeerConnection.IceServer> iceServers = new ArrayList<>();
        PeerConnection.RTCConfiguration rtcConfig = new PeerConnection.RTCConfiguration(iceServers);
        rtcConfig.tcpCandidatePolicy = PeerConnection.TcpCandidatePolicy.DISABLED;
        rtcConfig.bundlePolicy = PeerConnection.BundlePolicy.MAXBUNDLE;
//        rtcConfig.rtcpMuxPolicy = PeerConnection.RtcpMuxPolicy.REQUIRE;

        PeerConnection peerConnection = peerConnectionFactory.createPeerConnection(rtcConfig, pcConstraints, new PeerConnection.Observer() {

            @Override
            public void onSignalingChange(PeerConnection.SignalingState signalingState) {

            }

            @Override
            public void onIceConnectionChange(PeerConnection.IceConnectionState iceConnectionState) {

            }

            @Override
            public void onIceGatheringChange(PeerConnection.IceGatheringState iceGatheringState) {

            }

            @Override
            public void onIceCandidate(IceCandidate candidate) {
                if (candidate != null) {
                    Log.i(TAG, "iceCandidate: " + candidate);
                    JSONObject json = new JSONObject();
                    jsonPut(json, "type", "candidate");
                    jsonPut(json, "sdpMLineIndex", candidate.sdpMLineIndex);
                    jsonPut(json, "sdpMid", candidate.sdpMid);
                    jsonPut(json, "candidate", candidate.sdp);
                    sigSend(json);
                } else {
                    Log.i(TAG, "End of candidates. -------------------");
                }
            }

            @Override
            public void onAddStream(MediaStream mediaStream) {

            }

            @Override
            public void onRemoveStream(MediaStream mediaStream) {

            }

            @Override
            public void onDataChannel(DataChannel dataChannel) {

            }

            @Override
            public void onRenegotiationNeeded() {

            }
        });

        peerConnection.addStream(mediaStream);

        return peerConnection;
    }

    private void onOffer(final SessionDescription sdp) {
        setOffer(sdp);
        sendAnswer();
    }

    private void onAnswer(final SessionDescription sdp) {
        setAnswer(sdp);
    }

    private void onCandidate(final IceCandidate candidate) {
        peerConnection.addIceCandidate(candidate);
    }

    private void sendSDP(final SessionDescription sdp) {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", sdp.type.canonicalForm());
        jsonPut(json, "sdp", sdp.description);
        sigSend(json);
    }

    private void sendOffer() {
        peerConnection = prepareNewConnection();
        peerConnection.createOffer(sdpObserver, mediaConstraints);
    }

    private void setOffer(final SessionDescription sdp) {
        if (peerConnection != null) {
            Log.e(TAG, "peer connection already exists");
        }
        peerConnection = prepareNewConnection();
        peerConnection.setRemoteDescription(sdpObserver, sdp);
    }

    private void sendAnswer() {
        Log.i(TAG, "sending Answer. Creating remote session description...");
        if (peerConnection == null) {
            Log.e(TAG, "peerConnection NOT exist!");
            return;
        }
        peerConnection.createAnswer(sdpObserver, mediaConstraints);
    }

    private void setAnswer(final SessionDescription sdp) {
        if (peerConnection == null) {
            Log.e(TAG, "peerConnection NOT exist!");
            return;
        }
        peerConnection.setRemoteDescription(sdpObserver, sdp);
    }

    private class SDPObserver implements SdpObserver {

        @Override
        public void onCreateSuccess(SessionDescription sessionDescription) {
            peerConnection.setLocalDescription(sdpObserver, sessionDescription);
            Log.i(TAG, "Sending: SDP");
            Log.i(TAG, "" + sessionDescription);
            sendSDP(sessionDescription);
        }

        @Override
        public void onSetSuccess() {

        }

        @Override
        public void onCreateFailure(String s) {

        }

        @Override
        public void onSetFailure(String s) {

        }
    }

    // websocket related operations
    private void sigConnect(final String wsUrl) {
        wsServerUrl = wsUrl;

        ws = new WebSocketConnection();
        try {
            ws.connect(new URI(wsServerUrl), new WebSocketConnectionObserver() {
                @Override
                public void onOpen() {
                    Log.d(TAG, "WebSocket connection opened to: " + wsServerUrl);
                }

                @Override
                public void onClose(WebSocketCloseNotification code, String reason) {
                    Log.d(TAG, "WebSocket connection closed. Code: " + code + ". Reason: " + reason);
                }

                @Override
                public void onTextMessage(String payload) {
                    Log.d(TAG, "WSS->C: " + payload);

                    // from WebSocketChannelEvents::onWebSocketMessage()
                    String msg = payload;
                    try {
                        if (msg != null && msg.length() > 0) {
                            JSONObject json = new JSONObject(msg);
                            String type = json.optString("type");
                            if (type.equals("offer")) {
                                Log.i(TAG, "Received offer, set offer, sending answer....");
                                SessionDescription sdp = new SessionDescription(
                                        SessionDescription.Type.fromCanonicalForm(type),
                                        json.getString("sdp"));
                                onOffer(sdp);
                            } else if (type.equals("answer") && peerStarted) {
                                Log.i(TAG, "Received answer, setting answer SDP");
                                SessionDescription sdp = new SessionDescription(
                                        SessionDescription.Type.fromCanonicalForm(type),
                                        json.getString("sdp"));
                                onAnswer(sdp);
                            } else if (type.equals("candidate") && peerStarted) {
                                Log.i(TAG, "Received ICE candidate...");
                                IceCandidate candidate = new IceCandidate(
                                        json.getString("sdpMid"),
                                        json.getInt("sdpMLineIndex"),
                                        json.getString("candidate"));
                                onCandidate(candidate);
                            } else if (type.equals("user disconnected") && peerStarted) {
//                            } else if (type.equals("bye") && peerStarted) {
                                Log.i(TAG, "disconnected");
                                stop();
                            } else {
                                Log.e(TAG, "Unexpected WebSocket message: " + msg);
                            }
                        } else {
                            Log.e(TAG, "Unexpected WebSocket message: " + msg);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "WebSocket message JSON parsing error: " + e.toString() + " msg=" + msg);
                    }
                }

                @Override
                public void onRawTextMessage(byte[] payload) {
                }

                @Override
                public void onBinaryMessage(byte[] payload) {
                }
            });
            peerStarted = true;
        } catch (URISyntaxException e) {
            Log.e(TAG, "URI error: " + e.getMessage());
        } catch (WebSocketException e) {
            Log.e(TAG, "WebSocket connection error: " + e.getMessage());
        }
    }

    private void sigSend(final JSONObject jsonObject) {
        ws.sendTextMessage(jsonObject.toString());
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }
}
