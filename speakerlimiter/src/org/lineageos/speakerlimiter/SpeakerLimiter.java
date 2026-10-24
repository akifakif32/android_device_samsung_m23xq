/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.speakerlimiter;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.AudioPlaybackConfiguration;
import android.media.audiofx.DynamicsProcessing;
import android.media.audiofx.DynamicsProcessing.Config;
import android.media.audiofx.DynamicsProcessing.Limiter;
import android.media.audiofx.DynamicsProcessing.Mbc;
import android.media.audiofx.DynamicsProcessing.MbcBand;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemProperties;
import android.util.Log;

import java.util.List;

/**
 * The speaker distorts at the top volume steps because it can't handle the bass energy.
 * Stock firmware avoids this with Samsung SoundBooster, which needs Samsung's framework.
 * Instead, attach AOSP DynamicsProcessing to the output mix while the built-in speaker is in
 * use: a strong compressor on the low band plus a soft limiter. It acts on the signal after
 * the volume is applied, so low volumes stay untouched.
 *
 * Tuning without a rebuild:
 *   setprop persist.sys.spklim.config "<limiterDb> <bassHz> <bassThresholdDb> <bassRatio>"
 *   setprop persist.sys.spklim.enable false
 * New values are picked up on the next playback or routing change.
 */
public class SpeakerLimiter {
    private static final String TAG = "SpeakerLimiter";

    private static final String PROP_ENABLE = "persist.sys.spklim.enable";
    private static final String PROP_CONFIG = "persist.sys.spklim.config";
    private static final String DEFAULT_CONFIG = "-6 400 -40 10";

    private final AudioManager mAudioManager;
    private final Handler mHandler = new Handler(Looper.getMainLooper());
    private final Runnable mUpdate = this::update;

    private DynamicsProcessing mDp;
    private String mAppliedConfig;

    public SpeakerLimiter(Context context) {
        mAudioManager = context.getSystemService(AudioManager.class);
    }

    public void start() {
        mAudioManager.addOnModeChangedListener(mHandler::post, mode -> scheduleUpdate());
        mAudioManager.registerAudioPlaybackCallback(
                new AudioManager.AudioPlaybackCallback() {
                    @Override
                    public void onPlaybackConfigChanged(List<AudioPlaybackConfiguration> c) {
                        scheduleUpdate();
                    }
                },
                mHandler);
        mAudioManager.registerAudioDeviceCallback(
                new AudioDeviceCallback() {
                    @Override
                    public void onAudioDevicesAdded(AudioDeviceInfo[] added) {
                        scheduleUpdate();
                    }

                    @Override
                    public void onAudioDevicesRemoved(AudioDeviceInfo[] removed) {
                        scheduleUpdate();
                    }
                },
                mHandler);
        mHandler.postDelayed(mUpdate, 2000);
    }

    private void scheduleUpdate() {
        mHandler.removeCallbacks(mUpdate);
        mHandler.postDelayed(mUpdate, 300);
    }

    private void update() {
        if (SystemProperties.getBoolean(PROP_ENABLE, true) && speakerOnly()) {
            apply(SystemProperties.get(PROP_CONFIG, DEFAULT_CONFIG));
        } else {
            release();
        }
    }

    private boolean speakerOnly() {
        int mode = mAudioManager.getMode();
        if (mode == AudioManager.MODE_IN_CALL
                || mode == AudioManager.MODE_IN_COMMUNICATION
                || mode == AudioManager.MODE_CALL_SCREENING) {
            return false;
        }
        for (AudioDeviceInfo dev : mAudioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
            if (isExternal(dev.getType())) {
                return false;
            }
        }
        for (AudioPlaybackConfiguration cfg : mAudioManager.getActivePlaybackConfigurations()) {
            AudioDeviceInfo dev = cfg.getAudioDeviceInfo();
            if (dev != null && isExternal(dev.getType())) {
                return false;
            }
        }
        return true;
    }

    private static boolean isExternal(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_WIRED_HEADSET:
            case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
            case AudioDeviceInfo.TYPE_LINE_ANALOG:
            case AudioDeviceInfo.TYPE_LINE_DIGITAL:
            case AudioDeviceInfo.TYPE_AUX_LINE:
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO:
            case AudioDeviceInfo.TYPE_BLUETOOTH_A2DP:
            case AudioDeviceInfo.TYPE_BLE_HEADSET:
            case AudioDeviceInfo.TYPE_BLE_SPEAKER:
            case AudioDeviceInfo.TYPE_BLE_BROADCAST:
            case AudioDeviceInfo.TYPE_HEARING_AID:
            case AudioDeviceInfo.TYPE_USB_DEVICE:
            case AudioDeviceInfo.TYPE_USB_ACCESSORY:
            case AudioDeviceInfo.TYPE_USB_HEADSET:
            case AudioDeviceInfo.TYPE_HDMI:
            case AudioDeviceInfo.TYPE_HDMI_ARC:
            case AudioDeviceInfo.TYPE_HDMI_EARC:
            case AudioDeviceInfo.TYPE_DOCK:
            case AudioDeviceInfo.TYPE_DOCK_ANALOG:
                return true;
            default:
                return false;
        }
    }

    private void apply(String config) {
        float[] v = parse(config);
        if (mDp != null && config.equals(mAppliedConfig)) {
            return;
        }
        try {
            if (mDp == null) {
                Mbc mbc = new Mbc(true, true, 2);
                mbc.setBand(0, bassBand(v));
                mbc.setBand(1, restBand());
                Config cfg = new Config.Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION, 2,
                        false, 0, true, 2, false, 0, true)
                        .setMbcAllChannelsTo(mbc)
                        .setLimiterAllChannelsTo(limiter(v))
                        .build();
                mDp = new DynamicsProcessing(Integer.MAX_VALUE, 0, cfg);
            } else {
                mDp.setMbcBandAllChannelsTo(0, bassBand(v));
                mDp.setLimiterAllChannelsTo(limiter(v));
            }
            mDp.setEnabled(true);
            mAppliedConfig = config;
            Log.i(TAG, "enabled: " + config + " control=" + mDp.hasControl());
        } catch (RuntimeException e) {
            Log.e(TAG, "failed to apply " + config, e);
            release();
        }
    }

    private void release() {
        if (mDp == null) {
            return;
        }
        try {
            mDp.release();
        } catch (RuntimeException ignored) {
        }
        mDp = null;
        mAppliedConfig = null;
        Log.i(TAG, "released");
    }

    private static float[] parse(String config) {
        float[] v = {-6f, 400f, -40f, 10f};
        String[] parts = config.trim().split("\\s+");
        for (int i = 0; i < parts.length && i < v.length; i++) {
            try {
                v[i] = Float.parseFloat(parts[i]);
            } catch (NumberFormatException e) {
                Log.w(TAG, "bad value in " + PROP_CONFIG + ": " + parts[i]);
            }
        }
        return v;
    }

    private static MbcBand bassBand(float[] v) {
        // enabled, cutoff Hz, attack ms, release ms, ratio, threshold dB, knee dB,
        // noise gate dB, expander ratio, pre gain dB, post gain dB
        return new MbcBand(true, v[1], 5f, 120f, v[3], v[2], 6f, -90f, 1f, 0f, 0f);
    }

    private static MbcBand restBand() {
        return new MbcBand(true, 20000f, 5f, 120f, 1f, 0f, 0f, -90f, 1f, 0f, 0f);
    }

    private static Limiter limiter(float[] v) {
        // in use, enabled, link group, attack ms, release ms, ratio, threshold dB, post gain dB
        return new Limiter(true, true, 0, 1f, 60f, 10f, v[0], 0f);
    }
}
