package com.thedeveloperworldisyours.view;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.net.Uri;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import java.io.IOException;

/**
 * Created by javierg on 06/03/2017.
 */

public class FitXYVideoView extends TextureView implements TextureView.SurfaceTextureListener, MediaPlayer.OnErrorListener {

    // Indicate if logging is on
    public static final boolean LOG_ON = true;

    // Log tag
    private static final String TAG = FitXYVideoView.class.getName();

    private MediaPlayer mMediaPlayer;

    private float mVideoHeight;
    private float mVideoWidth;

    private boolean mIsDataSourceSet;
    private boolean mIsViewAvailable;
    private boolean mIsVideoPrepared;
    private boolean mIsPlayCalled;

    private ScaleType mScaleType;
    private State mState;
    private Context mContext;

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        try {
            setDataSource(getContext());
            play();
        } catch (Exception e) {

        }
        return false;
    }


    public enum ScaleType {
        CENTER_CROP, TOP, BOTTOM
    }

    public enum State {
        UNINITIALIZED, PLAY, STOP, PAUSE, END
    }

    public FitXYVideoView(Context context) {
        super(context);
        mContext = context;
        initView();
    }

    public FitXYVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
        initView();
    }

    public FitXYVideoView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        mContext = context;
        initView();
    }

    private void initView() {
        initPlayer();
        setScaleType(ScaleType.CENTER_CROP);
        setSurfaceTextureListener(this);
    }

    public void setScaleType(ScaleType scaleType) {
        mScaleType = scaleType;
    }

    private void updateTextureViewSize() {
        float viewWidth = getWidth();
        float viewHeight = getHeight();

        float scaleX = 1.0f;
        float scaleY = 1.0f;

        if (mVideoWidth > viewWidth && mVideoHeight > viewHeight) {
            scaleX = mVideoWidth / viewWidth;
            scaleY = mVideoHeight / viewHeight;
        } else if (mVideoWidth < viewWidth && mVideoHeight < viewHeight) {
            scaleY = viewWidth / mVideoWidth;
            scaleX = viewHeight / mVideoHeight;
        } else if (viewWidth > mVideoWidth) {
            scaleY = (viewWidth / mVideoWidth) / (viewHeight / mVideoHeight);
        } else if (viewHeight > mVideoHeight) {
            scaleX = (viewHeight / mVideoHeight) / (viewWidth / mVideoWidth);


        }

        // Calculate pivot points, in our case crop from center
        int pivotPointX;//pivot point is the centre point
        int pivotPointY;
        if (mVideoWidth < viewWidth && mVideoHeight < viewHeight) {
            pivotPointX = (int) (viewWidth / 2);
            pivotPointY = (int) (mVideoHeight / 2);
        } else {
            pivotPointX = (int) viewWidth - ((int) (viewWidth / (mVideoWidth / viewWidth)));
            pivotPointY = (int) viewHeight - ((int) (viewHeight / (mVideoHeight / viewHeight)));
        }

        Matrix matrix = new Matrix();
        matrix.setScale(scaleX, scaleY, pivotPointX, pivotPointY);
        setTransform(matrix);
    }

    private void initPlayer() {
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(this);
        } else {
            mMediaPlayer.reset();
        }
        mIsVideoPrepared = false;
        mIsPlayCalled = false;
        mState = State.UNINITIALIZED;
    }

    /**
     * @see android.media.MediaPlayer#setDataSource(String)
     */
    public void setDataSource(String path) {
        initPlayer();

        try {
            mMediaPlayer.setDataSource(path);
            mIsDataSourceSet = true;
            prepare();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * @see android.media.MediaPlayer#setDataSource(android.content.Context, android.net.Uri)
     */
    public void setDataSource(Context context, Uri uri) {
        initPlayer();

        try {
            mMediaPlayer.setDataSource(context, uri);
            mIsDataSourceSet = true;
            prepare();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * @see android.media.MediaPlayer#setDataSource(java.io.FileDescriptor)
     */
    public void setDataSource(Context context) {
        initPlayer();
        Uri uri = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.delight_mov);
        try {
            mMediaPlayer.setDataSource(context, uri);
            mIsDataSourceSet = true;
            prepare();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    /**
     * @see android.media.MediaPlayer#setDataSource(java.io.FileDescriptor)
     */
    public void setDataSource(AssetFileDescriptor afd) {
        initPlayer();
        try {
            long startOffset = afd.getStartOffset();
            long length = afd.getLength();
            mMediaPlayer.setDataSource(afd.getFileDescriptor(), startOffset, length);
            mIsDataSourceSet = true;
            prepare();
        } catch (IOException e) {
            Log.d(TAG, e.getMessage());
        }
    }

    public void prepare() {
        try {
            mMediaPlayer.setOnVideoSizeChangedListener(
                    new MediaPlayer.OnVideoSizeChangedListener() {
                        @Override
                        public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                            mVideoWidth = width;
                            mVideoHeight = height;
                            updateTextureViewSize();
                        }
                    }
            );
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mState = State.END;
                    log("Video has ended.");

                    if (mListener != null) {
                        mListener.onVideoEnd();
                    }
                    try {
                        setDataSource(getContext());
                        play();
                    } catch (Exception e) {

                    }
                }
            });

            // don't forget to call MediaPlayer.prepareAsync() method when you use constructor for
            // creating MediaPlayer
            mMediaPlayer.prepareAsync();

            // Play contact_video when the media source is ready for playback.
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    mIsVideoPrepared = true;
                    if (mIsPlayCalled && mIsViewAvailable) {
                        log("Player is prepared and play() was called.");
                        play();
                    }

                    if (mListener != null) {
                        mListener.onVideoPrepared();
                    }
                }
            });

        } catch (IllegalArgumentException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (SecurityException e) {
            Log.d(TAG, e.getMessage());
            e.printStackTrace();
        } catch (IllegalStateException e) {
            Log.d(TAG, e.toString());
            e.printStackTrace();
        }
    }

    /**
     * Play or resume contact_video. Video will be played as soon as view is available and media player is
     * prepared.
     * <p/>
     * If contact_video is stopped or ended and play() method was called, contact_video will start over.
     */
    public void play() {
        if (!mIsDataSourceSet) {
            log("play() was called but data source was not set.");
            return;
        }

        mIsPlayCalled = true;

        if (!mIsVideoPrepared) {
            log("play() was called but contact_video is not prepared yet, waiting.");
            return;
        }

        if (!mIsViewAvailable) {
            log("play() was called but view is not available yet, waiting.");
            return;
        }

        if (mState == State.PLAY) {
            log("play() was called but contact_video is already playing.");
            return;
        }

        if (mState == State.PAUSE) {
            log("play() was called but contact_video is paused, resuming.");
            mState = State.PLAY;
            mMediaPlayer.start();
            return;
        }

        if (mState == State.END || mState == State.STOP) {
            log("play() was called but contact_video already ended, starting over.");
            mState = State.PLAY;
            mMediaPlayer.seekTo(0);
            mMediaPlayer.start();
            return;
        }

        mState = State.PLAY;
        mMediaPlayer.start();
    }

    /**
     * Pause contact_video. If contact_video is already paused, stopped or ended nothing will happen.
     */
    public void pause() {
        if (mState == State.PAUSE) {
            log("pause() was called but contact_video already paused.");
            return;
        }

        if (mState == State.STOP) {
            log("pause() was called but contact_video already stopped.");
            return;
        }

        if (mState == State.END) {
            log("pause() was called but contact_video already ended.");
            return;
        }

        mState = State.PAUSE;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
        }
    }

    public boolean isPlaying() {
        return mMediaPlayer.isPlaying();
    }


    /**
     * Stop contact_video (pause and seek to beginning). If contact_video is already stopped or ended nothing will
     * happen.
     */
    public void stop() {
        if (mState == State.STOP) {
            log("stop() was called but contact_video already stopped.");
            return;
        }

        if (mState == State.END) {
            log("stop() was called but contact_video already ended.");
            return;
        }

        mState = State.STOP;
        if (mMediaPlayer.isPlaying()) {
            mMediaPlayer.pause();
            mMediaPlayer.seekTo(0);
        }
    }

    /**
     * @see android.media.MediaPlayer#setLooping(boolean)
     */
    public void setLooping(boolean looping) {
        mMediaPlayer.setLooping(looping);
    }

    /**
     * @see android.media.MediaPlayer#seekTo(int)
     */
    public void seekTo(int milliseconds) {
        mMediaPlayer.seekTo(milliseconds);
    }

    /**
     * @see android.media.MediaPlayer#getDuration()
     */
    public int getDuration() {
        return mMediaPlayer.getDuration();
    }

    static void log(String message) {
        if (LOG_ON) {
            Log.d(TAG, message);
        }
    }

    private MediaPlayerListener mListener;

    /**
     * Listener trigger 'onVideoPrepared' and `onVideoEnd` events
     */
    public void setListener(MediaPlayerListener listener) {
        mListener = listener;
    }

    public interface MediaPlayerListener {

        public void onVideoPrepared();

        public void onVideoEnd();
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        Surface surface = new Surface(surfaceTexture);
        mMediaPlayer.setSurface(surface);
        mIsViewAvailable = true;
        if (mIsDataSourceSet && mIsPlayCalled && mIsVideoPrepared) {
            log("View is available and play() was called.");
            play();
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }
}
