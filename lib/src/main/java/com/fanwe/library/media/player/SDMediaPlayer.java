package com.fanwe.library.media.player;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.text.TextUtils;

public class SDMediaPlayer
{
    private static SDMediaPlayer sInstance;

    private MediaPlayer mPlayer;
    private State mState = State.Idle;

    private String mDataPath;
    private int mDataRawResId;

    private boolean mHasInitialized;

    private OnStateChangeCallback mOnStateChangeCallback;
    private OnExceptionCallback mOnExceptionCallback;

    public SDMediaPlayer()
    {
        init();
    }

    public static SDMediaPlayer getInstance()
    {
        if (sInstance == null)
        {
            sInstance = new SDMediaPlayer();
        }
        return sInstance;
    }

    /**
     * 初始化播放器，调用release()后如果想要继续使用，要调用此方法初始化
     */
    public void init()
    {
        if (mPlayer != null)
        {
            release();
        }
        mPlayer = new MediaPlayer();
        mPlayer.setOnErrorListener(mOnErrorListener);
        mPlayer.setOnPreparedListener(mOnPreparedListener);
        mPlayer.setOnCompletionListener(mOnCompletionListener);
    }

    /**
     * 设置状态变化回调
     *
     * @param onStateChangeCallback
     */
    public void setOnStateChangeCallback(OnStateChangeCallback onStateChangeCallback)
    {
        mOnStateChangeCallback = onStateChangeCallback;
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
    public boolean setDataSource(String path)
    {
        try
        {
            if (!TextUtils.isEmpty(mDataPath) && mDataPath.equals(path))
            {
                return true;
            }

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
        try
        {
            if (mDataRawResId == rawResId)
            {
                return true;
            }

            reset();
            AssetFileDescriptor afd = context.getResources().openRawResourceFd(rawResId);
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
     * 是否已经初始化
     *
     * @return
     */
    public boolean hasInitialized()
    {
        return mHasInitialized;
    }

    /**
     * 播放进度移动到某个位置
     *
     * @param position 某个时间点（毫秒）
     */
    public void seekTo(int position)
    {
        try
        {
            if (hasInitialized())
            {
                mPlayer.seekTo(position);
            }
        } catch (Exception e)
        {
            notifyException(e);
        }
    }

    /**
     * 播放和暂停
     */
    public void performPlayPause()
    {
        performPlayInside(false);
    }

    /**
     * 重新播放和停止播放
     */
    public void performRestartPlayStop()
    {
        performPlayInside(true);
    }

    /**
     * 模拟暂停恢复播放
     *
     * @param restart true-恢复的时候重头播放
     */
    private void performPlayInside(boolean restart)
    {
        try
        {
            if (hasInitialized())
            {
                if (isPlaying())
                {
                    if (restart)
                    {
                        mPlayer.seekTo(0);
                    }
                    pause();
                } else
                {
                    start();
                }
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

            if (mState == State.Initialized)
            {
                mHasInitialized = true;
            }

            if (mOnStateChangeCallback != null)
            {
                mOnStateChangeCallback.onStateChanged(oldState, mState, this);
            }
        }
    }

    private void prepareAsyncPlayer()
    {
        setState(State.Preparing);
        mPlayer.prepareAsync();
    }

    private void startPlayer()
    {
        setState(State.Playing);
        mPlayer.start();
    }

    private void pausePlayer()
    {
        setState(State.Paused);
        mPlayer.pause();
    }

    private void stopPlayer()
    {
        setState(State.Stopped);
        mPlayer.stop();
    }

    private void resetPlayer()
    {
        setState(State.Idle);
        mPlayer.reset();

        resetDataInternal();
    }

    private void releasePlayer()
    {
        setState(State.Released);
        mPlayer.release();

        resetDataInternal();
    }

    private void resetDataInternal()
    {
        mDataPath = null;
        mDataRawResId = 0;
        mHasInitialized = false;
    }

    /**
     * 通知异常
     *
     * @param e
     */
    private void notifyException(Exception e)
    {
        if (mOnExceptionCallback != null)
        {
            mOnExceptionCallback.onException(e);
        }
    }

    //----------listener start----------
    /**
     * 错误监听
     */
    private OnErrorListener mOnErrorListener = new OnErrorListener()
    {
        @Override
        public boolean onError(MediaPlayer mp, int what, int extra)
        {
            resetPlayer();
            notifyException(new RuntimeException(mp + ":" + String.valueOf(what) + "," + extra));
            return true;
        }
    };

    /**
     * 准备监听
     */
    private OnPreparedListener mOnPreparedListener = new OnPreparedListener()
    {
        @Override
        public void onPrepared(MediaPlayer mp)
        {
            setState(State.Prepared);
            start();
        }
    };

    /**
     * 播放结束监听
     */
    private OnCompletionListener mOnCompletionListener = new OnCompletionListener()
    {
        @Override
        public void onCompletion(MediaPlayer mp)
        {
            setState(State.Completed);
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
        void onStateChanged(State oldState, State newState, SDMediaPlayer player);
    }

    public interface OnExceptionCallback
    {
        void onException(Exception e);
    }
}
