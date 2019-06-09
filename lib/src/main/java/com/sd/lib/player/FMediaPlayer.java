package com.sd.lib.player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.SurfaceHolder;

import java.lang.ref.WeakReference;

public class FMediaPlayer
{
    private static FMediaPlayer sInstance;

    private MediaPlayer mPlayer;
    private State mState = State.Idle;

    private String mDataPath;
    private int mDataRawResId;

    private boolean mIsDataInitialized;

    private WeakReference<SurfaceHolder> mSurfaceHolder;
    private boolean mIsLooping;

    private CountDownTimer mProgressTimer;

    private ObserverHolder<OnStateChangeCallback> mOnStateChangeCallbackHolder;
    private OnExceptionCallback mOnExceptionCallback;
    private OnProgressCallback mOnProgressCallback;

    private OnVideoSizeChangedListener mOnVideoSizeChangedListener;
    private OnCompletionListener mOnCompletionListener;
    private OnPreparedListener mOnPreparedListener;

    public FMediaPlayer()
    {
        //构造方法为public权限，可以单独new对象而不用全局单例对象
        init();
    }

    public static FMediaPlayer getInstance()
    {
        if (sInstance == null)
        {
            synchronized (FMediaPlayer.class)
            {
                if (sInstance == null)
                    sInstance = new FMediaPlayer();
            }
        }
        return sInstance;
    }

    /**
     * 初始化播放器，调用release()后如果想要继续使用，要调用此方法初始化
     */
    public void init()
    {
        if (mPlayer != null)
            release();

        mPlayer = new MediaPlayer();
        mPlayer.setOnErrorListener(mInternalOnErrorListener);
        mPlayer.setOnPreparedListener(mInternalOnPreparedListener);
        mPlayer.setOnCompletionListener(mInternalOnCompletionListener);
        mPlayer.setOnVideoSizeChangedListener(mInternalOnVideoSizeChangedListener);
    }

    /**
     * 添加状态变化回调
     *
     * @param callback
     */
    public void addOnStateChangeCallback(OnStateChangeCallback callback)
    {
        if (mOnStateChangeCallbackHolder == null)
            mOnStateChangeCallbackHolder = new ObserverHolder<>();

        mOnStateChangeCallbackHolder.add(callback);
    }

    /**
     * 移除状态变化回调
     *
     * @param callback
     */
    public void removeOnStateChangeCallback(OnStateChangeCallback callback)
    {
        if (mOnStateChangeCallbackHolder != null)
        {
            mOnStateChangeCallbackHolder.remove(callback);
            if (mOnStateChangeCallbackHolder.isEmpty())
                mOnStateChangeCallbackHolder = null;
        }
    }

    /**
     * 设置异常回调
     *
     * @param onExceptionCallback
     */
    public void setOnExceptionCallback(OnExceptionCallback onExceptionCallback)
    {
        mOnExceptionCallback = onExceptionCallback;
    }

    /**
     * 设置播放进度回调
     *
     * @param onProgressCallback
     */
    public void setOnProgressCallback(OnProgressCallback onProgressCallback)
    {
        mOnProgressCallback = onProgressCallback;
        startProgressTimerIfNeed();
    }

    /**
     * 设置视频宽高发生变化回调
     *
     * @param onVideoSizeChangedListener
     */
    public void setOnVideoSizeChangedListener(OnVideoSizeChangedListener onVideoSizeChangedListener)
    {
        mOnVideoSizeChangedListener = onVideoSizeChangedListener;
    }

    /**
     * 设置播放完毕回调
     *
     * @param onCompletionListener
     */
    public void setOnCompletionListener(OnCompletionListener onCompletionListener)
    {
        mOnCompletionListener = onCompletionListener;
    }

    /**
     * 设置准备完毕回调
     *
     * @param onPreparedListener
     */
    public void setOnPreparedListener(OnPreparedListener onPreparedListener)
    {
        mOnPreparedListener = onPreparedListener;
    }

    //----------proxy method start----------

    /**
     * 返回总时长（毫秒）
     *
     * @return
     */
    public int getDuration()
    {
        switch (mState)
        {
            case Playing:
            case Paused:
            case Stopped:
            case Completed:
                return mPlayer.getDuration();
            default:
                return 0;
        }
    }

    /**
     * 返回当前播放的进度位置（毫秒）
     *
     * @return
     */
    public int getCurrentPosition()
    {
        switch (mState)
        {
            case Playing:
            case Paused:
            case Completed:
                return mPlayer.getCurrentPosition();
            default:
                return 0;
        }
    }

    /**
     * 设置SurfaceHolder
     *
     * @param holder
     */
    public void setDisplay(SurfaceHolder holder)
    {
        setSurfaceHolder(holder);

        if (mState == State.Initialized)
            mPlayer.setDisplay(holder);
    }

    /**
     * 设置左右声道音量
     *
     * @param leftVolume  [0-1]
     * @param rightVolume [0-1]
     */
    public void setVolume(float leftVolume, float rightVolume)
    {
        mPlayer.setVolume(leftVolume, rightVolume);
    }

    /**
     * 设置是否循环播放
     *
     * @param looping
     */
    public void setLooping(boolean looping)
    {
        mIsLooping = looping;

        if (isDataInitialized())
            mPlayer.setLooping(looping);
    }

    /**
     * 当前是否循环播放模式
     *
     * @return
     */
    public boolean isLooping()
    {
        return mIsLooping;
    }

    public int getVideoWidth()
    {
        return mPlayer.getVideoWidth();
    }

    public int getVideoHeight()
    {
        return mPlayer.getVideoHeight();
    }

    //----------proxy method end----------

    private void setSurfaceHolder(SurfaceHolder holder)
    {
        final SurfaceHolder old = getSurfaceHolder();
        if (old != holder)
        {
            mSurfaceHolder = old == null ? null : new WeakReference<>(holder);
        }
    }

    /**
     * 返回设置的SurfaceHolder
     *
     * @return
     */
    private SurfaceHolder getSurfaceHolder()
    {
        return mSurfaceHolder == null ? null : mSurfaceHolder.get();
    }

    //----------data start----------

    public String getDataPath()
    {
        return mDataPath;
    }

    public int getDataRawResId()
    {
        return mDataRawResId;
    }

    /**
     * 设置数据源
     *
     * @param path 本地文件路径或者链接地址
     * @return
     */
    public boolean setDataPath(String path)
    {
        if (!TextUtils.isEmpty(mDataPath) && mDataPath.equals(path))
            return true;

        try
        {
            reset();
            mPlayer.setDataSource(path);
            mDataPath = path;
            setState(State.Initialized);
            return true;
        } catch (Exception e)
        {
            notifyException(e);
            return false;
        }
    }

    /**
     * 设置文件rawResId
     *
     * @param rawResId
     * @param context
     */
    public boolean setDataRawResId(int rawResId, Context context)
    {
        if (mDataRawResId == rawResId)
            return true;

        try
        {
            reset();
            final AssetFileDescriptor afd = context.getResources().openRawResourceFd(rawResId);
            mPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
            mDataRawResId = rawResId;
            setState(State.Initialized);
            return true;
        } catch (Exception e)
        {
            notifyException(e);
            return false;
        }
    }

    //----------data end----------

    /**
     * 是否播放状态
     *
     * @return
     */
    public boolean isPlaying()
    {
        return State.Playing == mState;
    }

    /**
     * 是否暂停状态
     *
     * @return
     */
    public boolean isPaused()
    {
        return State.Paused == mState;
    }

    /**
     * 播放数据是否已经初始化
     *
     * @return
     */
    public boolean isDataInitialized()
    {
        return mIsDataInitialized;
    }

    /**
     * 播放进度移动到某个位置
     *
     * @param position 某个时间点（毫秒）
     * @return true-发起seek成功
     */
    public boolean seekTo(int position)
    {
        if (isDataInitialized())
        {
            mPlayer.seekTo(position);
            return true;
        } else
        {
            return false;
        }
    }

    /**
     * 播放和暂停
     */
    public void performPlayPause()
    {
        togglePlayInside(false);
    }

    /**
     * 重新播放和停止播放
     */
    public void performPlayStop()
    {
        togglePlayInside(true);
    }

    /**
     * 暂停恢复播放
     *
     * @param restart true-恢复的时候重头播放
     */
    private void togglePlayInside(boolean restart)
    {
        if (!isDataInitialized())
            return;

        try
        {
            if (isPlaying())
            {
                if (restart)
                {
                    stop();
                } else
                {
                    pause();
                }
            } else
            {
                start();
            }
        } catch (Exception e)
        {
            notifyException(e);
        }
    }

    /**
     * 开始播放
     */
    public void start()
    {
        switch (mState)
        {
            case Initialized:
                prepareAsyncPlayer();
                break;
            case Prepared:
                startPlayer();
                break;
            case Paused:
                startPlayer();
                break;
            case Completed:
                startPlayer();
                break;
            case Stopped:
                prepareAsyncPlayer();
                break;
            default:
                break;
        }
    }

    /**
     * 暂停播放
     */
    public void pause()
    {
        switch (mState)
        {
            case Playing:
                pausePlayer();
                break;
            default:
                break;
        }
    }

    /**
     * 停止播放
     */
    public void stop()
    {
        switch (mState)
        {
            case Prepared:
                stopPlayer();
                break;
            case Playing:
                stopPlayer();
                break;
            case Paused:
                stopPlayer();
                break;
            case Completed:
                stopPlayer();
                break;
            default:
                break;
        }
    }

    /**
     * 重置播放器，一般用于关闭播放界面的时候调用
     */
    public void reset()
    {
        if (mState == State.Released)
            return;

        stop();
        resetPlayer();
    }

    /**
     * 释放播放器，用于不再需要播放器的时候调用，调用此方法后，需要手动调用init()方法初始化后才可以使用
     */
    public void release()
    {
        stop();
        releasePlayer();
    }

    /**
     * 返回当前状态
     *
     * @return
     */
    public State getState()
    {
        return mState;
    }

    private void setState(State state)
    {
        if (mState != state)
        {
            final State oldState = mState;

            mState = state;

            switch (mState)
            {
                case Initialized:
                    setDataInitialized(true);
                    break;
                case Playing:
                    startProgressTimerIfNeed();
                    break;
                case Paused:
                case Stopped:
                    notifyProgressCallback();
                    stopProgressTimer();
                    break;
            }

            if (mOnStateChangeCallbackHolder != null)
            {
                mOnStateChangeCallbackHolder.foreach(new ObserverHolder.ForeachCallback<OnStateChangeCallback>()
                {
                    @Override
                    public void onNext(OnStateChangeCallback observer)
                    {
                        observer.onStateChanged(FMediaPlayer.this, oldState, mState);
                    }
                });
            }
        }
    }

    /**
     * 设置播放数据是否已经初始化
     *
     * @param dataInitialized
     */
    private void setDataInitialized(boolean dataInitialized)
    {
        mIsDataInitialized = dataInitialized;

        if (mIsDataInitialized)
        {
            setDisplay(getSurfaceHolder());
            setLooping(mIsLooping);
        }
    }

    private void prepareAsyncPlayer()
    {
        mPlayer.prepareAsync();
        setState(State.Preparing);
    }

    private void startPlayer()
    {
        mPlayer.start();
        setState(State.Playing);
    }

    private void pausePlayer()
    {
        mPlayer.pause();
        setState(State.Paused);
    }

    private void stopPlayer()
    {
        mPlayer.stop();
        setState(State.Stopped);
    }

    private void resetPlayer()
    {
        resetDataInternal();

        mPlayer.reset();
        setState(State.Idle);
    }

    private void releasePlayer()
    {
        resetDataInternal();
        setSurfaceHolder(null);

        mPlayer.release();
        setState(State.Released);
    }

    private void resetDataInternal()
    {
        mDataPath = null;
        mDataRawResId = 0;
        setDataInitialized(false);
        if (mState != State.Released)
            mPlayer.setDisplay(null);

        stopProgressTimer();
    }

    /**
     * 通知异常
     *
     * @param e
     */
    private void notifyException(Exception e)
    {
        if (mOnExceptionCallback != null)
            mOnExceptionCallback.onException(e);
    }

    private void startProgressTimerIfNeed()
    {
        stopProgressTimer();

        if (mOnProgressCallback == null)
        {
            //未设置回调不需要启动Timer
            return;
        }

        if (mState == State.Playing)
        {
            if (mProgressTimer == null)
            {
                mProgressTimer = new CountDownTimer(Long.MAX_VALUE, 250)
                {
                    @Override
                    public void onTick(long millisUntilFinished)
                    {
                        notifyProgressCallback();
                    }

                    @Override
                    public void onFinish()
                    {
                    }
                };
                mProgressTimer.start();
            }
        }
    }

    private void stopProgressTimer()
    {
        if (mProgressTimer != null)
        {
            mProgressTimer.cancel();
            mProgressTimer = null;
        }
    }

    private void notifyProgressCallback()
    {
        if (mOnProgressCallback != null)
            mOnProgressCallback.onProgress(FMediaPlayer.this, getCurrentPosition(), getDuration());
    }

    //----------listener start----------
    /**
     * 错误监听
     */
    private final OnErrorListener mInternalOnErrorListener = new OnErrorListener()
    {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra)
        {
            resetPlayer();
            notifyException(new RuntimeException(mp + ":" + what + "," + extra));
            return true;
        }
    };

    /**
     * 准备监听
     */
    private final MediaPlayer.OnPreparedListener mInternalOnPreparedListener = new MediaPlayer.OnPreparedListener()
    {
        @Override
        public void onPrepared(MediaPlayer mp)
        {
            setState(State.Prepared);
            start();

            if (mOnPreparedListener != null)
                mOnPreparedListener.onPrepared(FMediaPlayer.this);
        }
    };

    /**
     * 播放结束监听
     */
    private final MediaPlayer.OnCompletionListener mInternalOnCompletionListener = new MediaPlayer.OnCompletionListener()
    {
        @Override
        public void onCompletion(MediaPlayer mp)
        {
            setState(State.Completed);

            if (mOnCompletionListener != null)
                mOnCompletionListener.onCompletion(FMediaPlayer.this);
        }
    };

    /**
     * 视频宽高发生变化
     */
    private final MediaPlayer.OnVideoSizeChangedListener mInternalOnVideoSizeChangedListener = new MediaPlayer.OnVideoSizeChangedListener()
    {
        @Override
        public void onVideoSizeChanged(MediaPlayer mp, int width, int height)
        {
            if (mOnVideoSizeChangedListener != null)
                mOnVideoSizeChangedListener.onVideoSizeChanged(FMediaPlayer.this, width, height);
        }
    };

    //----------listener end----------

    public enum State
    {
        /**
         * 已经释放资源
         */
        Released,
        /**
         * 空闲，还没设置dataSource
         */
        Idle,
        /**
         * 已经设置dataSource，还未播放
         */
        Initialized,
        /**
         * 准备中
         */
        Preparing,
        /**
         * 准备完毕
         */
        Prepared,
        /**
         * 已经启动播放
         */
        Playing,
        /**
         * 已经暂停播放
         */
        Paused,
        /**
         * 已经播放完毕
         */
        Completed,
        /**
         * 调用stop方法后的状态
         */
        Stopped;
    }

    public interface OnStateChangeCallback
    {
        /**
         * 播放器状态发生变化回调
         *
         * @param player
         * @param oldState
         * @param newState
         */
        void onStateChanged(FMediaPlayer player, State oldState, State newState);
    }

    public interface OnExceptionCallback
    {
        /**
         * 异常回调
         *
         * @param e
         */
        void onException(Exception e);
    }

    public interface OnVideoSizeChangedListener
    {
        /**
         * 视频宽高发生变化回调
         *
         * @param player
         * @param width
         * @param height
         */
        void onVideoSizeChanged(FMediaPlayer player, int width, int height);
    }

    public interface OnCompletionListener
    {
        /**
         * 播放完毕回调
         *
         * @param player
         */
        void onCompletion(FMediaPlayer player);
    }

    public interface OnPreparedListener
    {
        /**
         * 准备完毕回调
         *
         * @param player
         */
        void onPrepared(FMediaPlayer player);
    }

    public interface OnProgressCallback
    {
        /**
         * 播放进度回调
         *
         * @param player
         * @param currentPosition 当前播放进度（毫秒）
         * @param totalDuration   总时长（毫秒）
         */
        void onProgress(FMediaPlayer player, int currentPosition, int totalDuration);
    }
}
