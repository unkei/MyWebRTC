package com.example.unno.mywebrtc;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.java_websocket.WebSocketImpl;
import org.java_websocket.client.DefaultSSLWebSocketClientFactory;
import org.java_websocket.client.WebSocketClient;

import org.java_websocket.drafts.Draft_17;
import org.java_websocket.handshake.ServerHandshake;
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

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ssl.SSLContext;


public class MainActivity extends ActionBarActivity {

    class WsOptions {
        private final static boolean DEFAULT_SECURE = true;
        // cilent page http://peerclient-rhj-001.herokuapp.com
        private final static String  DEFAULT_HOST   = "peerserver-rhj-001.herokuapp.com";
//        private final static String  DEFAULT_HOST   = "unpeerjs.herokuapp.com";
        private final static int     DEFAULT_PORT   = 443;
        private final static String  DEFAULT_PATH   = "/";
        private final static String  DEFAULT_KEY    = "peerjs";
//        private final static boolean DEFAULT_SECURE = false;
//        private final static String  DEFAULT_HOST   = "10.54.36.20";
//        private final static int     DEFAULT_PORT   = 9000;
//        private final static String  DEFAULT_PATH   = "/";
//        private final static String  DEFAULT_KEY    = "peerjs";

        boolean mSecure;
        String  mHost;
        int     mPort;
        String  mPath;
        String  mKey;
        String  mClientId;
        String  mTargetId = "xxx";
        String  mToken;

        public WsOptions() {
            this(DEFAULT_SECURE, DEFAULT_HOST, DEFAULT_PORT, DEFAULT_PATH, DEFAULT_KEY);
        }

        public WsOptions(boolean secure, String host, int port, String path, String key) {
            mSecure = secure;
            mHost = host;
            mPort = port;
            mPath = path;
            mKey  = key;
            mToken = randomString();
        }

        private String randomString() {
            Random random = new Random();
            return String.valueOf(random.nextInt(Integer.MAX_VALUE));
        }

        public String getClientId() {
            return mClientId;
        }

        public void setClientId(String clientId) {
            mClientId = clientId;
        }

        public String getTargetId() {
            return mTargetId;
        }

        public void setTargetId(String targetId) {
            mTargetId = targetId;
        }

        public String getClientIdUri() {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(mSecure ? "https" : "http");
            builder.encodedAuthority(mHost + ":" + mPort);
            builder.path(mPath + mKey + "/id");

            return builder.build().toString();
        }

        public String getWsServerUri() {
            Uri.Builder builder = new Uri.Builder();
            builder.scheme(mSecure ? "wss" : "ws");
            builder.encodedAuthority(mHost + ":" + mPort);
            builder.path(mPath + "peerjs");
            builder.appendQueryParameter("key", mKey);
            builder.appendQueryParameter("id", mClientId);
            builder.appendQueryParameter("token", mToken);

            return builder.build().toString();
        }
    }

    private String TAG = MainActivity.class.getSimpleName();
    private GLSurfaceView glview;
    private String VIDEO_TRACK_ID = TAG + "VIDEO";
    private String AUDIO_TRACK_ID = TAG + "AUDIO";
    private String LOCAL_MEDIA_STREAM_ID = TAG + "STREAM";
    private MediaStream mediaStream;
    private VideoTrack localVideoTrack;
    private AudioTrack localAudioTrack;
    private VideoTrack remoteVideoTrack;
    private AudioTrack remoteAudioTrack;
    private VideoRenderer renderer;
    private VideoRenderer renderer_sub;
    private String roomName;
    private String connectionId = "pc_abc";

    private final static String clientIdURIformat = "%s://%s:%d%s/%s/id";
    private WsOptions wsOptions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        wsOptions = new WsOptions();
        getClientId(wsOptions, new ClientIdListener() {
            @Override
            public void onResponse(String clientId) {
                wsOptions.setClientId(clientId);
                sigConnect(wsOptions);
            }
        });
        initWebRTC();

        Log.i(TAG, "VideoCapturerAndroid.getDeviceCount() = " + VideoCapturerAndroid.getDeviceCount());
        String nameOfFrontFacingDevice = VideoCapturerAndroid.getNameOfFrontFacingDevice();
        String nameOfBackFacingDevice = VideoCapturerAndroid.getNameOfBackFacingDevice();
        Log.i(TAG, "VideoCapturerAndroid.getNameOfFrontFacingDevice() = " + nameOfFrontFacingDevice);
        Log.i(TAG, "VideoCapturerAndroid.getNameOfBackFacingDevice() = " + nameOfBackFacingDevice);
        VideoCapturerAndroid capturer = VideoCapturerAndroid.create(nameOfFrontFacingDevice);

        MediaConstraints videoConstraints = new MediaConstraints();
        MediaConstraints audioConstraints = new MediaConstraints();
        VideoSource videoSource = peerConnectionFactory.createVideoSource(capturer, videoConstraints);
        localVideoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource);
        AudioSource audioSource = peerConnectionFactory.createAudioSource(audioConstraints);
        localAudioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource);

        glview = (GLSurfaceView) findViewById(R.id.glview);
        VideoRendererGui.setView(glview, null);
        try {
            renderer = VideoRendererGui.createGui(0, 0, 100, 100, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            renderer_sub = VideoRendererGui.createGui(72, 72, 25, 25, VideoRendererGui.ScalingType.SCALE_ASPECT_FILL, true);
            localVideoTrack.addRenderer(renderer_sub);
        } catch (Exception e) {
            e.printStackTrace();
        }

        mediaStream = peerConnectionFactory.createLocalMediaStream(LOCAL_MEDIA_STREAM_ID);
        mediaStream.addTrack(localVideoTrack);
        mediaStream.addTrack(localAudioTrack);
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

        boolean ret = true;
        switch (id) {
            case R.id.action_call:
                connect();
                break;
            case R.id.action_hangup:
                hangUp();
                break;
            case R.id.action_room:
                showRoomDialog();
                break;
            default:
                ret = super.onOptionsItemSelected(item);
                break;
        }

        return ret;
    }

    private void showRoomDialog() {
        final EditText editView = new EditText(this);
        editView.setText(roomName);
        new AlertDialog.Builder(this)
                .setIcon(android.R.drawable.ic_dialog_info)
                .setTitle("Enter room name")
                .setView(editView)
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        roomName = editView.getText().toString();
                        hangUp();
                        sigReconnect();
                    }
                })
                .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                    }
                })
                .show();
    }

    private String getRoomName() {
        return (roomName == null || roomName.isEmpty())?
                "_defaultroom":
                roomName;
    }

    // webrtc
    private PeerConnectionFactory peerConnectionFactory;
    private PeerConnection peerConnection;
    private MediaConstraints pcConstraints;
    private MediaConstraints videoConstraints;
    private MediaConstraints audioConstraints;
    private MediaConstraints mediaConstraints;
    private SDPObserver sdpObserver = new SDPObserver();

    private WebSocketClient mSocket;
    private URI wsServerUrl;
    private boolean peerStarted = false;

    private void initWebRTC() {
        PeerConnectionFactory.initializeAndroidGlobals(this, true, true, true, VideoRendererGui.getEGLContext());

        peerConnectionFactory = new PeerConnectionFactory();

        pcConstraints = new MediaConstraints();
        videoConstraints = new MediaConstraints();
        audioConstraints = new MediaConstraints();
        mediaConstraints = new MediaConstraints();
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveAudio", "true"));
        mediaConstraints.mandatory.add(new MediaConstraints.KeyValuePair("OfferToReceiveVideo", "true"));
    }

    private void connect() {
        if (!peerStarted) {
            sendOffer();
            peerStarted = true;
        }
    }

    private void hangUp() {
        sendDisconnect();
        stop();
    }

    private void stop() {
        if (peerConnection != null) {
            peerConnection.close();
            peerConnection = null;
            peerStarted = false;
        }
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
                    jsonPut(json, "type", "CANDIDATE");
                    JSONObject JSONpayload = new JSONObject();
                    {
                        jsonPut(JSONpayload, "type", "media");
                        jsonPut(JSONpayload, "connectionId", connectionId);
                        JSONObject JSONcandidate= new JSONObject();
                        {
                            jsonPut(JSONcandidate, "sdpMLineIndex", candidate.sdpMLineIndex);
                            jsonPut(JSONcandidate, "sdpMid", candidate.sdpMid);
                            jsonPut(JSONcandidate, "candidate", candidate.sdp);
                        }
                        jsonPut(JSONpayload, "candidate", JSONcandidate);
                    }
                    jsonPut(json, "payload", JSONpayload);
                    sigSend(json);
                } else {
                    Log.i(TAG, "End of candidates. -------------------");
                }
            }

            @Override
            public void onAddStream(MediaStream stream) {
                if (MainActivity.this.peerConnection == null) {
                    return;
                }
                if (stream.audioTracks.size() > 1 || stream.videoTracks.size() > 1) {
                    Log.e(TAG, "Weird-looking stream: " + stream);
                    return;
                }
                if (stream.videoTracks.size() == 1) {
                    remoteVideoTrack = stream.videoTracks.get(0);
//                    remoteVideoTrack.setEnabled(true);
                    remoteVideoTrack.addRenderer(MainActivity.this.renderer);
                }
            }

            @Override
            public void onRemoveStream(MediaStream stream) {
                remoteVideoTrack = null;
                stream.videoTracks.get(0).dispose();
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
        peerStarted = true;
    }

    private void onAnswer(final SessionDescription sdp) {
        setAnswer(sdp);
    }

    private void onCandidate(final IceCandidate candidate) {
        peerConnection.addIceCandidate(candidate);
    }

    private void sendSDP(final SessionDescription sdp) {
        JSONObject json = new JSONObject();
        jsonPut(json, "type", sdp.type.canonicalForm().toUpperCase());
        JSONObject JSONpayload = new JSONObject();
        {
            jsonPut(JSONpayload, "connectionId", connectionId);
            JSONObject JSONsdp = new JSONObject();
            {
                jsonPut(JSONsdp, "type", sdp.type.canonicalForm());
                jsonPut(JSONsdp, "sdp", sdp.description);
            }
            jsonPut(JSONpayload, "sdp", JSONsdp);
        }
        jsonPut(json, "payload", JSONpayload);
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

    private void sendDisconnect() {
        JSONObject json = new JSONObject();
//        jsonPut(json, "type", "user disconnected");
        jsonPut(json, "type", "LEAVE");
        sigSend(json);
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
    private void sigConnect(WsOptions opts) {

        try {
            wsServerUrl = new URI(opts.getWsServerUri());
            mSocket = new WebSocketClient(wsServerUrl) {

                @Override
                public void onOpen(ServerHandshake handshakedata) {
                    Log.d(TAG, "WebSocket connection opened to: " + wsServerUrl);
                    sigEnter();
                }

                @Override
                public void onMessage(String message) {
                    try {
                        if (message.length() > 0) {
                            JSONObject json = new JSONObject(message);
//                            JSONObject json = (JSONObject)(args[0]);
                            Log.d(TAG, "WSS->C: " + json);
                            String type = json.optString("type");
//                            if (type.equals("offer")) {
                            if (type.equals("OFFER")) {
                                Log.i(TAG, "Received offer, set offer, sending answer....");
                                String src = json.getString("src");
                                if (src != null && !src.isEmpty()) {
                                    wsOptions.setTargetId(src);
                                }
                                JSONObject JSONpayload = json.getJSONObject("payload");
                                JSONObject JSONsdp = JSONpayload.getJSONObject("sdp");
                                SessionDescription sdp = new SessionDescription(
                                        SessionDescription.Type.fromCanonicalForm(type),
                                        JSONsdp.getString("sdp"));
                                connectionId = JSONpayload.getString("connectionId");
                                onOffer(sdp);
//                            } else if (type.equals("answer") && peerStarted) {
                            } else if (type.equals("ANSWER") && peerStarted) {
                                Log.i(TAG, "Received answer, setting answer SDP");
                                JSONObject JSONpayload = json.getJSONObject("payload");
                                JSONObject JSONsdp = JSONpayload.getJSONObject("sdp");
                                SessionDescription sdp = new SessionDescription(
                                        SessionDescription.Type.fromCanonicalForm(type),
                                        JSONsdp.getString("sdp"));
                                onAnswer(sdp);
//                            } else if (type.equals("candidate") && peerStarted) {
                            } else if (type.equals("CANDIDATE") && peerStarted) {
                                Log.i(TAG, "Received ICE candidate...");
                                JSONObject JSONpayload = json.getJSONObject("payload");
                                JSONObject JSONcandidate = JSONpayload.getJSONObject("candidate");
                                IceCandidate candidate = new IceCandidate(
                                        JSONcandidate.getString("sdpMid"),
                                        JSONcandidate.getInt("sdpMLineIndex"),
                                        JSONcandidate.getString("candidate"));
                                onCandidate(candidate);
//                            } else if (type.equals("user disconnected") && peerStarted) {
                            } else if (type.equals("LEAVE") && peerStarted) {
//                            } else if (type.equals("bye") && peerStarted) {
                                Log.i(TAG, "disconnected");
                                stop();
                            } else {
                                Log.e(TAG, "Unexpected WebSocket message: " + message);
                            }
                        } else {
                            Log.e(TAG, "Unexpected WebSocket message: " + message);
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "WebSocket message JSON parsing error: " + e.toString() + " message=" + message);
                    }
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    Log.d(TAG, "WebSocket connection closed.");
                }

                @Override
                public void onError(Exception ex) {

                }
            };

            SSLContext sslContext = null;
            try {
                sslContext = SSLContext.getInstance("TLS");
                sslContext.init(null, null, null); // will use java's default key and trust store which is sufficient unless you deal with self-signed certificates
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            } catch (KeyManagementException e) {
                e.printStackTrace();
            }
            mSocket.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(sslContext));

            mSocket.connect();
        } catch (URISyntaxException e) {
            Log.e(TAG, "URI error: " + e.getMessage());
        }
    }

    private void sigReconnect() {
        mSocket.close();
        mSocket.connect();
    }

    private void sigEnter() {
//        mSocket.emit("enter", getRoomName());
    }

    private void sigSend(final JSONObject jsonObject) {
        jsonPut(jsonObject, "src", wsOptions.getClientId());
        jsonPut(jsonObject, "dst", wsOptions.getTargetId());
        mSocket.send(jsonObject.toString());
    }

    // Put a |key|->|value| mapping in |json|.
    private static void jsonPut(JSONObject json, String key, Object value) {
        try {
            json.put(key, value);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    interface ClientIdListener {
        void onResponse(String clientId);
    }

    private void getClientId(WsOptions opts, final ClientIdListener listener) {

        final HttpGet request = new HttpGet(opts.getClientIdUri());

        final DefaultHttpClient httpClient = new DefaultHttpClient();

        AsyncTask<Void, Void, String> task = new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    HttpResponse response = httpClient.execute(request);
                    return EntityUtils.toString(response.getEntity());
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return null;
            }

            @Override
            protected void onPostExecute(String result) {
                super.onPostExecute(result);
                if (listener != null) {
                    listener.onResponse(result);
                }
            }
        };
        task.execute();
    }
}
