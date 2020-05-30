LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := init.qcom.usb.sh
LOCAL_MODULE_CLASS := ETC
LOCAL_MODULE_TAGS  := optional
LOCAL_MODULE_PATH := $(TARGET_OUT_PRODUCT)/vendor_overlay/29/bin
LOCAL_SRC_FILES := init.qcom.usb.sh
include $(BUILD_PREBUILT)
