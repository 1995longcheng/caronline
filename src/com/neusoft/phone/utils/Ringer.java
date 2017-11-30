
package com.neusoft.phone.utils;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;

public class Ringer {

    private final static String TAG = "GeminiPhoneRinger";
    private static Ringer instance = null;
    private Context mContext = null;

    Ringtone mRingtone;
    private Worker mRingThread;
    private Handler mRingHandler;
    private long mFirstRingEventTime = -1;
    private long mFirstRingStartTime = -1;
    //Settings.System.DEFAULT_RINGTONE_URI;
    Uri mCustomRingtoneUri = Uri.parse("content://media/internal/audio/media/35");
    private String mPath =
            Environment.getRootDirectory().getPath() + "/media/audio/notifications/Arcturus.ogg";
    private MediaPlayer mMediaPlayer = new MediaPlayer();

    private static final int PLAY_RING_ONCE = 1;
    private static final int STOP_RING = 3;

    private Ringer(Context context) {
        mContext = context;
        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
    }

    public static Ringer init(Context context) {
        synchronized (Ringer.class) {
            if (instance == null) {
                instance = new Ringer(context);
            }
            return instance;
        }
    }

    private void log(String message) {
        android.util.Log.i(TAG, message);
    }

    public void ring() {
        log("ring()..." + mCustomRingtoneUri);

        synchronized (this) {

            AudioManager audioManager = (AudioManager) mContext
                    .getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
                log("skipping ring because volume is zero");
                return;
            }

            makeLooper();
            if (mFirstRingEventTime < 0) {
                mFirstRingEventTime = SystemClock.elapsedRealtime();
                mRingHandler.sendEmptyMessage(PLAY_RING_ONCE);
            } else {
                // For repeat rings, figure out by how much to delay
                // the ring so that it happens the correct amount of
                // time after the previous ring
                if (mFirstRingStartTime > 0) {
                    // Delay subsequent rings by the delta between event
                    // and play time of the first ring

                    log("delaying ring by "
                            + (mFirstRingStartTime - mFirstRingEventTime));

                    mRingHandler.sendEmptyMessageDelayed(PLAY_RING_ONCE,
                            mFirstRingStartTime - mFirstRingEventTime);
                } else {
                    // We've gotten two ring events so far, but the ring
                    // still hasn't started. Reset the event time to the
                    // time of this event to maintain correct spacing.
                    mFirstRingEventTime = SystemClock.elapsedRealtime();
                }
            }
        }

    }

    public void stopRing() {
        synchronized (this) {
            log("stopRing()...");

            if (mRingHandler != null) {
                mRingHandler.removeCallbacksAndMessages(null);
                Message msg = mRingHandler.obtainMessage(STOP_RING);
                msg.obj = mRingtone;
                mRingHandler.sendMessage(msg);
                mRingThread = null;
                mRingHandler = null;
                mRingtone = null;
                mFirstRingEventTime = -1;
                mFirstRingStartTime = -1;
            } else {
                log("- stopRing: null mRingHandler!");
            }

        }
    }

    private class Worker implements Runnable {
        private final Object mLock = new Object();
        private Looper mLooper;

        Worker(String name) {
            Thread t = new Thread(null, this, name);
            t.start();
            synchronized (mLock) {
                while (mLooper == null) {
                    try {
                        mLock.wait();
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }

        public Looper getLooper() {
            return mLooper;
        }

        public void run() {
            synchronized (mLock) {
                Looper.prepare();
                mLooper = Looper.myLooper();
                mLock.notifyAll();
            }
            Looper.loop();
        }
    }

    private void makeLooper() {
        if (mRingThread == null) {
            mRingThread = new Worker("ringer");
            mRingHandler = new Handler(mRingThread.getLooper()) {
                @Override
                public void handleMessage(Message msg) {
                    switch (msg.what) {
                        case PLAY_RING_ONCE:
                            log("mRingHandler: PLAY_RING_ONCE...");
                            try {
                                mMediaPlayer.reset();
                                mMediaPlayer.setDataSource(mPath);
                                mMediaPlayer.prepare();
                                mMediaPlayer.start();
                            } catch (Exception e) {
                                log("Start exception is " + e);
                            }
                            break;
                        case STOP_RING:
                            log("mRingHandler: STOP_RING...");
                            try {
                                mMediaPlayer.stop();
                            } catch (IllegalStateException e) {
                                log("Stop exception is " + e);
                            }
                            break;
                    }
                }
            };
        }
    }

}

