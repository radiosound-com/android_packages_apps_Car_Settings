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

package com.android.car.settings.applications.managedomainurls;

import static com.google.common.truth.Truth.assertThat;

import android.content.Intent;
import android.provider.Settings;

import org.junit.Test;

public class ManageDomainUrlsActivityTest {

    @Test
    public void getPackageNameFromIntent_appLinksIntent_returnsPackageName() {
        Intent intent = new Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS)
                .setData(android.net.Uri.parse("package:com.aurora.store"));

        assertThat(ManageDomainUrlsActivity.getPackageNameFromIntent(intent))
                .isEqualTo("com.aurora.store");
    }

    @Test
    public void getPackageNameFromIntent_listIntent_returnsNull() {
        Intent intent = new Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS);

        assertThat(ManageDomainUrlsActivity.getPackageNameFromIntent(intent)).isNull();
    }

    @Test
    public void getPackageNameFromIntent_otherAction_returnsNull() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_DOMAIN_URLS)
                .setData(android.net.Uri.parse("package:com.aurora.store"));

        assertThat(ManageDomainUrlsActivity.getPackageNameFromIntent(intent)).isNull();
    }
}
