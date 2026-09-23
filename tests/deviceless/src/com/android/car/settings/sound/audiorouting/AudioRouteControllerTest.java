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

package com.android.car.settings.sound.audiorouting;

import static android.car.media.CarAudioManager.AUDIO_FEATURE_DYNAMIC_ROUTING;

import static com.android.car.settings.common.PreferenceController.AVAILABLE;
import static com.android.car.settings.common.PreferenceController.UNSUPPORTED_ON_DEVICE;

import static com.google.common.truth.Truth.assertThat;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.car.media.CarAudioManager;
import android.content.Context;

import androidx.lifecycle.Lifecycle;
import androidx.preference.Preference;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.runner.AndroidJUnit4;

import com.android.car.settings.CarSettingsApplication;
import com.android.car.settings.R;
import com.android.car.settings.common.PreferenceControllerTestHelper;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.util.ReflectionHelpers;

@RunWith(AndroidJUnit4.class)
public class AudioRouteControllerTest {
    private static final int AUDIO_ZONE = 0;

    private final CarAudioManager mCarAudioManager = mock(CarAudioManager.class);
    private PreferenceControllerTestHelper<AudioRouteController> mControllerHelper;
    private Preference mPreference;
    private int mMediaUsage;

    @Before
    public void setUp() {
        Context context = spy(InstrumentationRegistry.getInstrumentation().getContext());
        CarSettingsApplication application = new CarSettingsApplication();
        ReflectionHelpers.setField(application, "mCarAudioManager", mCarAudioManager);
        ReflectionHelpers.setField(application, "mAudioZoneId", AUDIO_ZONE);
        when(context.getApplicationContext()).thenReturn(application);
        mMediaUsage = context.getResources().getInteger(R.integer.audio_route_selector_usage);
        mPreference = new Preference(context);
        mControllerHelper = new PreferenceControllerTestHelper<>(context,
                AudioRouteController.class, mPreference);
    }

    @Test
    public void getAvailabilityStatus_legacyRouting_unsupported() {
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING))
                .thenReturn(false);

        assertThat(mControllerHelper.getController().getAvailabilityStatus())
                .isEqualTo(UNSUPPORTED_ON_DEVICE);
    }

    @Test
    public void lifecycle_legacyRouting_hidesPreferenceWithoutQueryingOutput() {
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING))
                .thenReturn(false);
        when(mCarAudioManager.getOutputDeviceForUsage(anyInt(), anyInt()))
                .thenThrow(new IllegalStateException("Non legacy routing is required"));

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_START);
        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_RESUME);
        mControllerHelper.getController().refreshUi();

        assertThat(mPreference.isVisible()).isFalse();
        verify(mCarAudioManager, never()).getOutputDeviceForUsage(anyInt(), anyInt());
    }

    @Test
    public void onCreate_dynamicRouting_showsPreferenceAndQueriesOutput() {
        when(mCarAudioManager.isAudioFeatureEnabled(AUDIO_FEATURE_DYNAMIC_ROUTING))
                .thenReturn(true);

        mControllerHelper.sendLifecycleEvent(Lifecycle.Event.ON_CREATE);

        assertThat(mControllerHelper.getController().getAvailabilityStatus()).isEqualTo(AVAILABLE);
        assertThat(mPreference.isVisible()).isTrue();
        verify(mCarAudioManager).getOutputDeviceForUsage(AUDIO_ZONE, mMediaUsage);
    }
}
