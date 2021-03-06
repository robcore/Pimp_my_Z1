/**   Copyright (C) 2013  Louis Teboul (a.k.a Androguide)
 *
 *    admin@pimpmyrom.org  || louisteboul@gmail.com
 *    http://pimpmyrom.org || http://androguide.fr
 *    71 quai Clémenceau, 69300 Caluire-et-Cuire, FRANCE.
 *
 *     This program is free software; you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation; either version 2 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License along
 *      with this program; if not, write to the Free Software Foundation, Inc.,
 *      51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 **/

package com.androguide.honamicontrol.kernel.memory;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.MenuItem;
import android.widget.CompoundButton;

import com.androguide.honamicontrol.R;
import com.androguide.honamicontrol.cards.CardSeekBarGeneric;
import com.androguide.honamicontrol.cards.CardSeekBarSysctl;
import com.androguide.honamicontrol.cards.CardSwitchDisabled;
import com.androguide.honamicontrol.cards.CardSwitchPlugin;
import com.androguide.honamicontrol.helpers.CMDProcessor.CMDProcessor;
import com.androguide.honamicontrol.helpers.CPUHelper;
import com.androguide.honamicontrol.helpers.Helpers;
import com.fima.cardsui.objects.CardStack;
import com.fima.cardsui.views.CardUI;

public class MemoryManagementActivity extends ActionBarActivity implements MemoryManagementInterface {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setIcon(getResources().getDrawable(R.drawable.ic_tools_mm));
        setContentView(R.layout.cardsui);

        final SharedPreferences bootPrefs = getSharedPreferences("BOOT_PREFS", 0);
        CardUI cardsUI = (CardUI) findViewById(R.id.cardsui);
        cardsUI.addStack(new CardStack(""));
        cardsUI.addStack(new CardStack(getString(R.string.ksm_header)));

        // KERNEL SAME-PAGE MERGING
        if (Helpers.doesFileExist(KSM_TOGGLE)) {
            cardsUI.addCard(new CardSwitchPlugin(
                    getString(R.string.ksm),
                    getString(R.string.ksm_desc),
                    "#1abc9c",
                    KSM_TOGGLE,
                    this,
                    new CompoundButton.OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton compoundButton, boolean isOn) {
                            bootPrefs.edit().putBoolean("KSM_ENABLED", isOn).commit();
                            if (isOn)
                                Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 1 > " + KSM_TOGGLE);
                            else
                                Helpers.CMDProcessorWrapper.runSuCommand("busybox echo 0 > " + KSM_TOGGLE);
                        }
                    }
            ));

        } else {
            cardsUI.addCard(new CardSwitchDisabled(
                            getString(R.string.ksm),
                            getString(R.string.ksm_unsupported),
                            "#c74b46",
                            "",
                            this,
                            null)
            );
        }

        // KSM PAGES-TO-SCAN
        if (Helpers.doesFileExist(KSM_PAGES_TO_SCAN)) {
            int currPagesToScan = 100;
            try {
                currPagesToScan = Integer.valueOf(CPUHelper.readOneLineNotRoot(KSM_PAGES_TO_SCAN));
            } catch (Exception e) {
                Log.e("KSM_PAGES_TO_SCAN", e.getMessage());
            }

            final CardSeekBarGeneric cardKSMPages = new CardSeekBarGeneric(
                    getString(R.string.ksm_pages_to_scan),
                    getString(R.string.ksm_pages_to_scan_desc),
                    "#1abc9c", "",
                    KSM_PAGES_TO_SCAN,
                    512,
                    currPagesToScan,
                    this,
                    null
            );
            cardsUI.addCard(cardKSMPages);
        }

        // KSM SLEEP TIMER
        if (Helpers.doesFileExist(KSM_SLEEP_TIMER)) {
            int currTimer = 500;
            try {
                currTimer = Integer.valueOf(CPUHelper.readOneLineNotRoot(KSM_SLEEP_TIMER));
            } catch (Exception e) {
                Log.e("KSM_SLEEP_TIMER", e.getMessage());
            }

            cardsUI.addCard(new CardSeekBarGeneric(
                    getString(R.string.ksm_timer),
                    getString(R.string.ksm_timer_desc),
                    "#1abc9c", "ms",
                    KSM_SLEEP_TIMER,
                    2000,
                    currTimer,
                    this,
                    null
            ));
        }

        cardsUI.addStack(new CardStack("VM PARAMETERS"));

        // VFS CACHE PRESSURE
        String currVfs = CMDProcessor.runShellCommand("busybox sysctl " + VFS_CACHE_PRESSURE).getStdout();
        currVfs = currVfs.replaceAll("[\\D]", "");
        int vfs = 50;
        try {
            vfs = Integer.valueOf(currVfs);
        } catch (Exception e) {
            Log.e("VFS_CACHE_PRESSURE", e.getMessage());
        }
        cardsUI.addCard(new CardSeekBarSysctl(
                getString(R.string.vfs_cache_pressure),
                getString(R.string.vfs_cache_pressure_text),
                "#1abc9c",
                "%",
                VFS_CACHE_PRESSURE,
                100,
                vfs,
                this,
                null
        ));

        // VM SWAPPINESS
        String currSwappiness = CMDProcessor.runShellCommand("busybox sysctl " + SWAPPINESS).getStdout();
        currSwappiness = currSwappiness.replaceAll("[\\D]", "");
        int swappiness = 60;
        try {
            swappiness = Integer.valueOf(currSwappiness);
        } catch (Exception e) {
            Log.e("SWAPPINESS", e.getMessage());
        }
        cardsUI.addCard(new CardSeekBarSysctl(
                getString(R.string.swappiness),
                getString(R.string.swappiness_text),
                "#1abc9c",
                "%",
                SWAPPINESS,
                100,
                swappiness,
                this,
                null
        ));

        // VM DIRTY RATIO
        String currDirtyRatio = CMDProcessor.runShellCommand("busybox sysctl " + DIRTY_RATIO).getStdout();
        currDirtyRatio = currDirtyRatio.replaceAll("[\\D]", "");
        int dirtyRatio = 30;
        try {
            dirtyRatio = Integer.valueOf(currDirtyRatio);
        } catch (Exception e) {
            Log.e("DIRTY_RATIO", e.getMessage());
        }
        cardsUI.addCard(new CardSeekBarSysctl(
                getString(R.string.dirty_ratio),
                getString(R.string.dirty_ratio_text),
                "#1abc9c",
                "%",
                DIRTY_RATIO,
                100,
                dirtyRatio,
                this,
                null
        ));

        // VM DIRTY BACKGROUND RATIO
        String currDirtyBgRatio = CMDProcessor.runShellCommand("busybox sysctl " + DIRTY_BG_RATIO).getStdout();
        currDirtyBgRatio = currDirtyBgRatio.replaceAll("[\\D]", "");
        int dirtyBgRatio = 15;
        try {
            dirtyBgRatio = Integer.valueOf(currDirtyBgRatio);
        } catch (Exception e) {
            Log.e("DIRTY_BACKGROUND_RATIO", e.getMessage());
        }

        cardsUI.addCard(new CardSeekBarSysctl(
                getString(R.string.dirty_bg_ratio),
                getString(R.string.dirty_bg_ratio_text),
                "#1abc9c",
                "%",
                DIRTY_BG_RATIO,
                100,
                dirtyBgRatio,
                this,
                null
        ));

        // VM DIRTY WRITEBACK
        String currDirtyWriteback = CMDProcessor.runShellCommand("busybox sysctl " + DIRTY_WRITEBACK_CENTISECS).getStdout();
        currDirtyWriteback = currDirtyWriteback.replaceAll("[\\D]", "");
        int dirtyWriteback = 500;
        try {
            dirtyWriteback = Integer.valueOf(currDirtyWriteback);
        } catch (Exception e) {
            Log.e("DIRTY_WRITEBACK_CENTISECS", e.getMessage());
        }

        cardsUI.addCard(new CardSeekBarSysctl(
                getString(R.string.dirty_writeback),
                getString(R.string.dirty_writeback_text),
                "#1abc9c",
                "cs",
                DIRTY_WRITEBACK_CENTISECS,
                2000,
                dirtyWriteback,
                this,
                null
        ));

        // VM DIRTY EXPIRE
        String currDirtyExpire = CMDProcessor.runShellCommand("busybox sysctl " + DIRTY_EXPIRE_CENTISECS).getStdout();
        currDirtyExpire = currDirtyExpire.replaceAll("[\\D]", "");
        int dirtyExpire = 200;
        try {
            dirtyExpire = Integer.valueOf(currDirtyExpire);
        } catch (Exception e) {
            Log.e("DIRTY_EXPIRE_CENTISECS", e.getMessage());
        }

        cardsUI.addCard(new CardSeekBarSysctl(
                getString(R.string.dirty_expire),
                getString(R.string.dirty_expire_text),
                "#1abc9c",
                "cs",
                DIRTY_EXPIRE_CENTISECS,
                2000,
                dirtyExpire,
                this,
                null
        ));

        cardsUI.refresh();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}
