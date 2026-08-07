/*
 * Copyright (C) 2019 The Android Open Source Project
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

package com.android.car.settings.applications.managedomainurls;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;

import com.android.car.settings.Flags;
import com.android.car.settings.common.BaseCarSettingsActivity;

/**
 * Starts {@link ManageDomainUrlsFragment} in a separate activity to help with the back navigation
 * flow. This setting differs from the other settings in that the user arrives here from the
 * PermissionController rather than from within the Settings app itself.
 */
public class ManageDomainUrlsActivity extends BaseCarSettingsActivity {

    @Nullable
    @Override
    protected Fragment getInitialFragment() {
        String packageName = getPackageNameFromIntent(getIntent());
        if (packageName != null && isInstalledPackage(packageName)) {
            return Flags.newFragmentForIntents()
                    ? ApplicationLaunchSettingsFragmentUpdated.newInstance(packageName)
                    : ApplicationLaunchSettingsFragment.newInstance(packageName);
        }
        return new ManageDomainUrlsFragment();
    }

    @Nullable
    @VisibleForTesting
    static String getPackageNameFromIntent(Intent intent) {
        if (intent == null || !Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS.equals(
                intent.getAction())) {
            return null;
        }

        Uri data = intent.getData();
        if (data == null || !"package".equals(data.getScheme())) {
            return null;
        }

        String packageName = data.getSchemeSpecificPart();
        return packageName == null || packageName.isEmpty() ? null : packageName;
    }

    private boolean isInstalledPackage(String packageName) {
        try {
            getPackageManager().getApplicationInfo(packageName, /* flags= */ 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}
