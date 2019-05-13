LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_PACKAGE_NAME := OneplusNetworkSettings
LOCAL_CERTIFICATE := platform
LOCAL_MODULE_TAGS := optional
LOCAL_DEX_PREOPT := true
LOCAL_STATIC_ANDROID_LIBRARIES := \
    android-support-v14-preference \
    android-support-v13 \
    android-support-v7-appcompat \
    android-support-v7-preference \
    android-support-v7-recyclerview \
    android-support-v4
LOCAL_SRC_FILES := $(call all-java-files-under, src)
LOCAL_PRIVILEGED_MODULE := true
LOCAL_RESOURCE_DIR := $(LOCAL_PATH)/res
LOCAL_USE_AAPT2 := true

LOCAL_JAVA_LIBRARIES := telephony-common


#LOCAL_PROGUARD_ENABLED := disabled

include $(BUILD_PACKAGE)
