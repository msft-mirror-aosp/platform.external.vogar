# Copyright (C) 2014 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

LOCAL_PATH := $(call my-dir)

# Run the tests using using the following target.
# ============================================================
.PHONY: run-vogar-tests
run-vogar-tests: vogar-tests
	ANDROID_BUILD_TOP=$$(pwd) \
	  java -cp ./out/host/linux-x86/framework/vogar-tests.jar \
	  org.junit.runner.JUnitCore vogar.AllTests

# copy vogar script
# ============================================================
include $(CLEAR_VARS)
LOCAL_IS_HOST_MODULE := true
LOCAL_MODULE_CLASS := EXECUTABLES
LOCAL_MODULE := vogar
LOCAL_LICENSE_KINDS := SPDX-license-identifier-Apache-2.0 SPDX-license-identifier-BSD
LOCAL_LICENSE_CONDITIONS := notice
LOCAL_NOTICE_FILE := $(LOCAL_PATH)/LICENSE
LOCAL_SRC_FILES := bin/vogar-android
include $(BUILD_PREBUILT)
