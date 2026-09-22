/*
 * Copyright (C) 2026 The Android Open Source Project
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

package com.android.car.settings.qc;

import static android.car.media.CarAudioManager.AUDIO_FEATURE_DYNAMIC_ROUTING;
import static android.car.media.CarAudioManager.INVALID_AUDIO_ZONE;
import static android.car.media.CarAudioManager.INVALID_VOLUME_GROUP_ID;
import static android.car.media.CarAudioManager.PRIMARY_AUDIO_ZONE;
import static android.media.AudioAttributes.USAGE_MEDIA;

import static com.android.car.qc.QCItem.QC_ACTION_SLIDER_VALUE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.app.admin.DevicePolicyManager;
import android.car.media.CarAudioManager;
import android.content.Context;
import android.content.Intent;
import android.os.UserManager;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.car.qc.QCList;
import com.android.car.qc.QCSlider;
import com.android.car.settings.CarSettingsApplication;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.util.ReflectionHelpers;

@RunWith(AndroidJUnit4.class)
public class LegacyVolumeSliderTest {
    private static final int MEDIA_GROUP = 0;
    private final CarAudioManager mCarAudioManager = mock(CarAudioManager.class);
    private Context mContext;
    private CarSettingsApplication mApplication;
    private MediaVolumeSlider mSlider;

    @Before
    public void setUp() {
        mContext = spy(InstrumentationRegistry.getInstrumentation().getContext());
        mApplication = new CarSettingsApplication();
        ReflectionHelpers.setField(mApplication, "mCarAudioManager", mCarAudioManager);
        ReflectionHelpers.setField(mApplication, "mAudioZoneId", INVALID_AUDIO_ZONE);
        when(mContext.getApplicationContext()).thenReturn(mApplication);
        when(mContext.getSystemService(UserManager.class)).thenReturn(mock(UserManager.class));
        when(mContext.getSystemService(DevicePolicyManager.class))
                .thenReturn(mock(DevicePolicyManager.class));
        when(mCarAudioManager.getVolumeGroupIdForUsage(anyInt(), anyInt()))
                .thenReturn(INVALID_VOLUME_GROUP_ID);
        when(mCarAudioManager.getVolumeGroupIdForUsage(anyInt(), eq(USAGE_MEDIA)))
                .thenReturn(MEDIA_GROUP);
        when(mCarAudioManager.getGroupVolume(anyInt(), eq(MEDIA_GROUP))).thenReturn(8);
        when(mCarAudioManager.getGroupMaxVolume(anyInt(), eq(MEDIA_GROUP))).thenReturn(25);
        when(mCarAudioManager.getGroupMinVolume(anyInt(), eq(INVALID_VOLUME_GROUP_ID)))
                .thenThrow(new IllegalArgumentException("Invalid volume group"));
        mSlider = new MediaVolumeSlider(mContext);
    }

    @Test
    public void getQCItem_legacyWithoutZone_showsMediaVolume() {
        QCList item = (QCList) mSlider.getQCItem();

        assertThat(item).isNotNull();
        assertThat(item.getRows()).hasSize(1);
        QCSlider slider = item.getRows().get(0).getSlider();
        assertThat(slider.getMin()).isEqualTo(0);
        assertThat(slider.getMax()).isEqualTo(25);
        assertThat(slider.getValue()).isEqualTo(8);
        assertThat(slider.isEnabled()).isTrue();
        verify(mCarAudioManager).getGroupVolume(PRIMARY_AUDIO_ZONE, MEDIA_GROUP);
    }

    @Test
    public void onNotifyChange_legacyWithoutZone_setsPrimaryVolume() {
        mSlider.onNotifyChange(volumeIntent());

        verify(mCarAudioManager).setGroupVolume(eq(PRIMARY_AUDIO_ZONE), eq(MEDIA_GROUP),
                eq(12), anyInt());
    }

    @Test
    public void volumeCallback_legacyWithoutZone_handlesPrimaryVolumeChange() {
        MediaVolumeSliderWorker worker = new MediaVolumeSliderWorker(mContext,
                SettingsQCRegistry.MEDIA_VOLUME_SLIDER_URI);

        worker.getVolumeChangeCallback().onGroupVolumeChanged(PRIMARY_AUDIO_ZONE, MEDIA_GROUP, 0);

        verify(mCarAudioManager).getVolumeGroupIdForUsage(PRIMARY_AUDIO_ZONE, USAGE_MEDIA);
    }

    @Test
    public void getQCItem_unsupportedGroups_hidesCallAndNavigation() {
        ReflectionHelpers.setField(mApplication, "mAudioZoneId", PRIMARY_AUDIO_ZONE);

        assertThat(new CallVolumeSlider(mContext).getQCItem()).isNull();
        assertThat(new NavigationVolumeSlider(mContext).getQCItem()).isNull();
        verify(mCarAudioManager, never()).getGroupMinVolume(anyInt(), eq(INVALID_VOLUME_GROUP_ID));
    }

    @Test
    public void dynamicRouting_assignedZone_preservesZoneForReadAndWrite() {
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING)).thenReturn(true);
        ReflectionHelpers.setField(mApplication, "mAudioZoneId", 3);

        assertThat(mSlider.getQCItem()).isNotNull();
        mSlider.onNotifyChange(volumeIntent());

        verify(mCarAudioManager).getGroupVolume(3, MEDIA_GROUP);
        verify(mCarAudioManager).setGroupVolume(eq(3), eq(MEDIA_GROUP), eq(12), anyInt());
    }

    @Test
    public void dynamicRouting_withoutZone_doesNotUsePrimaryZone() {
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING)).thenReturn(true);

        assertThat(mSlider.getQCItem()).isNull();
        mSlider.onNotifyChange(volumeIntent());

        verify(mCarAudioManager, never()).setGroupVolume(anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    public void getQCItem_carDisconnected_hidesSlider() {
        ReflectionHelpers.setField(mApplication, "mCarAudioManager", null);

        assertThat(mSlider.getQCItem()).isNull();
    }

    private static Intent volumeIntent() {
        return new Intent().putExtra(QC_ACTION_SLIDER_VALUE, 12)
                .putExtra(BaseVolumeSlider.EXTRA_GROUP_ID, MEDIA_GROUP);
    }
}
