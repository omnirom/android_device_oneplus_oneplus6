LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES := bootable/recovery \
		    generated_kernel_headers
LOCAL_SRC_FILES := gpt-utils.cpp sparse_crc32.cpp
LOCAL_HEADER_LIBRARIES := generated_kernel_headers
LOCAL_MODULE := libgptutils
include $(BUILD_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE_TAGS := optional
LOCAL_C_INCLUDES := bootable/recovery \
		    generated_kernel_headers
LOCAL_SRC_FILES := gpt-utils.cpp sparse_crc32.cpp
LOCAL_HEADER_LIBRARIES := generated_kernel_headers
LOCAL_SHARED_LIBRARIES += liblog libcutils
LOCAL_MODULE := libgptutils
LOCAL_COPY_HEADERS_TO := gpt-utils/inc
LOCAL_COPY_HEADERS := gpt-utils.h
LOCAL_VENDOR_MODULE := true
include $(BUILD_SHARED_LIBRARY)
