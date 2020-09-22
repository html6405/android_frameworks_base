/*
 * Copyright (C) 2018 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.statusbar.phone;

import android.util.Log;
import android.util.DisplayMetrics;
import android.util.Pair;
import android.view.View;
import android.view.WindowInsets;
import android.view.DisplayCutout;

import android.graphics.Rect;
import java.util.List;

import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.statusbar.phone.StatusBarIconController;
import com.android.systemui.statusbar.policy.Clock;
import com.android.systemui.tuner.TunerService;
import com.android.systemui.util.leak.RotationUtils;

public class ClockController implements TunerService.Tunable {

    private static final String TAG = "ClockController";
    
    private int mCutoutLocation = -1;
    private static final int CLOCK_POSITION_RIGHT = 0;
    private static final int CLOCK_POSITION_CENTER = 1;
    private static final int CLOCK_POSITION_LEFT = 2;

    public static final String CLOCK_POSITION = "lineagesystem:status_bar_clock";

    private Clock mActiveClock, mCenterClock, mLeftClock, mRightClock;
    private View mCenterClockLayout, mRightClockLayout;

    private int mClockPosition = CLOCK_POSITION_CENTER;
    private boolean mBlackListed = false;
    private DisplayCutout mDisplayCutout;
    private View mStatusBar;

    public ClockController(View statusBar) {

        mCenterClock = statusBar.findViewById(R.id.clock_center);
        mLeftClock = statusBar.findViewById(R.id.clock);
        mRightClock = statusBar.findViewById(R.id.clock_right);
        mCenterClockLayout = statusBar.findViewById(R.id.center_clock_layout);
        mRightClockLayout = statusBar.findViewById(R.id.right_clock_layout);
	mStatusBar = statusBar;
        mActiveClock = mLeftClock;
        statusBar.setOnApplyWindowInsetsListener(new android.view.View.OnApplyWindowInsetsListener(){
            public WindowInsets onApplyWindowInsets (View v, WindowInsets insets){
                updateActiveClock();
                return insets;
            }
        });
        Dependency.get(TunerService.class).addTunable(this,
                StatusBarIconController.ICON_BLACKLIST, CLOCK_POSITION);
    }

    public Clock getClock() {
        switch (mClockPosition) {
            case CLOCK_POSITION_RIGHT:
                return mRightClock;
            case CLOCK_POSITION_LEFT:
                return mLeftClock;
            case CLOCK_POSITION_CENTER:
            default:
                if(mCutoutLocation == 1)
                     return mRightClock;
                return mCenterClock;
        }
    }

    private void updateActiveClock() {
        if(mStatusBar instanceof PhoneStatusBarView)
            mDisplayCutout = ((PhoneStatusBarView)mStatusBar).getRootWindowInsets().getDisplayCutout();
        else {
            mDisplayCutout = ((StatusBarWindowView)mStatusBar).getRootWindowInsets().getDisplayCutout();
        }
        DisplayMetrics metrics = new DisplayMetrics();
        mStatusBar.getDisplay().getMetrics(metrics);
        int middle = metrics.widthPixels/2;
        Pair<Integer, Integer> margins = null;
	if (mDisplayCutout != null) {
            List<Rect> bounding = mDisplayCutout.getBoundingRects();
            for (int i=0; i<bounding.size(); i++) {
                int left = bounding.get(i).left;
                int right = bounding.get(i).right;
                int top = bounding.get(i).top;
                int bottom = bounding.get(i).bottom;
                if(left >0 && right >0){
	           margins = new Pair<Integer, Integer>(left, right);
                   break;
	       }
            }
        }
	if(margins == null)
            mCutoutLocation = -1;
	else if(margins.first < middle && margins.second > middle)
	    mCutoutLocation = 1;
        else if(margins.first < middle)
            mCutoutLocation = 0;
        else
            mCutoutLocation = 2;


        mActiveClock.setClockVisibleByUser(false);
        mActiveClock = getClock();
        mActiveClock.setClockVisibleByUser(true);

        // Override any previous setting
        mActiveClock.setClockVisibleByUser(!mBlackListed);
    }

    @Override
    public void onTuningChanged(String key, String newValue) {
        Log.d(TAG, "onTuningChanged key=" + key + " value=" + newValue);

        if (CLOCK_POSITION.equals(key)) {
            mClockPosition = CLOCK_POSITION_LEFT;
            try {
                mClockPosition = Integer.valueOf(newValue);
            } catch (NumberFormatException ex) {}
        } else {
            mBlackListed = StatusBarIconController.getIconBlacklist(newValue).contains("clock");
        }
        updateActiveClock();
    }

    public View getClockLayout() {
        // We default to center, but it has no effect as long the clock itself is invisible
        return mClockPosition == CLOCK_POSITION_RIGHT ? mRightClockLayout : mCenterClockLayout;
    }
}
