/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.speakerlimiter;

import android.app.Application;

public class SpeakerLimiterApp extends Application {
    private SpeakerLimiter mLimiter;

    @Override
    public void onCreate() {
        super.onCreate();
        mLimiter = new SpeakerLimiter(this);
        mLimiter.start();
    }
}
