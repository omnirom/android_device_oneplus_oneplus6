# Copyright (C) 2016 The CyanogenMod Project
# Copyright (C) 2017 The LineageOS Project
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

#
# This file is the build configuration for a full Android
# build for grouper hardware. This cleanly combines a set of
# device-specific aspects (drivers) with a device-agnostic
# product configuration (apps).
#
$(call inherit-product-if-exists, vendor/gapps/arm64/arm64-vendor.mk)
$(call inherit-product, vendor/omni/config/phone-xxhdpi-4096-dalvik-heap.mk)
$(call inherit-product, vendor/omni/config/phone-xxhdpi-2048-hwui-memory.mk)
# Enable updating of APEXes
$(call inherit-product, $(SRC_TARGET_DIR)/product/updatable_apex.mk)

# Binaries
PRODUCT_PACKAGES += llkd

# For ringtones that rely on forward lock encryption
PRODUCT_PACKAGES += libfwdlockengine

# Camera service uses 'libdepthphoto' for adding dynamic depth
# metadata inside depth jpegs.
PRODUCT_PACKAGES += \
    libdepthphoto \

# Enable stats logging in LMKD
TARGET_LMKD_STATS_LOG := true

# Enable dynamic partition size
#PRODUCT_USE_DYNAMIC_PARTITION_SIZE := true

#from build treble includes
PRODUCT_COPY_FILES += \
    system/core/rootdir/init.zygote64_32.rc:root/init.zygote64_32.rc \
    system/core/rootdir/init.zygote32_64.rc:root/init.zygote32_64.rc

TARGET_SUPPORTS_32_BIT_APPS := true
TARGET_SUPPORTS_64_BIT_APPS := true

# SP-NDK:
PRODUCT_PACKAGES += \
    libvulkan \

PRODUCT_SYSTEM_DEFAULT_PROPERTIES += \
    ro.build.version.all_codenames=$(PLATFORM_VERSION_ALL_CODENAMES) \
    ro.build.version.codename=$(PLATFORM_VERSION_CODENAME) \
    ro.build.version.release=$(PLATFORM_VERSION) \
    ro.build.version.sdk=$(PLATFORM_SDK_VERSION)

AB_OTA_PARTITIONS += \
    boot \
    dtbo \
    system \
    vbmeta

AB_OTA_POSTINSTALL_CONFIG += \
    RUN_POSTINSTALL_system=true \
    POSTINSTALL_PATH_system=system/bin/otapreopt_script \
    FILESYSTEM_TYPE_system=ext4 \
    POSTINSTALL_OPTIONAL_system=true

PRODUCT_PACKAGES += \
    otapreopt_script \
    update_engine \
    update_engine_sideload \
    update_verifier

PRODUCT_HOST_PACKAGES += \
    brillo_update_payload

# Boot control
PRODUCT_PACKAGES_DEBUG += \
    bootctl

PRODUCT_STATIC_BOOT_CONTROL_HAL := \
    bootctrl.sdm845 \
    libcutils \
    libgptutils \
    libz \

PRODUCT_PACKAGES_DEBUG += \
    update_engine_client

PRODUCT_PACKAGES += \
    omni_charger_res_images \
    animation.txt \
    font_charger.png

PRODUCT_PACKAGES += \
    com.android.future.usb.accessory

# Live Wallpapers
PRODUCT_PACKAGES += \
    LiveWallpapers \
    LiveWallpapersPicker \
    VisualizationWallpapers \
    librs_jni

# Prebuilt
PRODUCT_COPY_FILES += \
    $(call find-copy-subdir-files,*,device/oneplus/oneplus6/prebuilt/system,system) \
    $(call find-copy-subdir-files,*,device/oneplus/oneplus6/prebuilt/root,root)


PRODUCT_AAPT_CONFIG := xxhdpi
PRODUCT_AAPT_PREF_CONFIG := xxhdpi
PRODUCT_CHARACTERISTICS := nosdcard

# audio
PRODUCT_PACKAGES += \
    audio.a2dp.default \

# Lights
PRODUCT_PACKAGES += \
    lights.oneplus6

PRODUCT_PACKAGES += \
    android.hardware.light-V2.0-java \
    android.hardware.light@2.0-impl

PRODUCT_COPY_FILES += \
    frameworks/av/media/libstagefright/data/media_codecs_google_audio.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/media_codecs_google_audio.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_telephony.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/media_codecs_google_telephony.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_video.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/media_codecs_google_video.xml

# NFC - NCI
PRODUCT_PACKAGES += \
    NfcNci \
    libnfc_nci_jni \
    Tag \
    com.android.nfc_extras

PRODUCT_PACKAGES += \
    android.hardware.nfc@1.1 \
    android.hardware.nfc@1.0

# NFC - NQ (NXP)
PRODUCT_PACKAGES += \
    Tag \
    com.android.nfc_extras

PRODUCT_PACKAGES += \
    wificond \
    wifilogd \
    libwpa_client \
    hostapd \
    wpa_supplicant \
    wpa_supplicant.conf

PRODUCT_PACKAGES += \
    libcld80211 \
    lib_driver_cmd_qcwcn

# Camera
PRODUCT_PACKAGES += \
    SnapdragonCamera2

# power
PRODUCT_PACKAGES += \
    power.oneplus6

# ANT+
PRODUCT_PACKAGES += \
    AntHalService

# QMI
PRODUCT_PACKAGES += \
    libjson

PRODUCT_PACKAGES += \
    ims-ext-common \
    tcmiface

#PRODUCT_BOOT_JARS += \
    qtiNetworkLib \
    com.qualcomm.qti.camera \
    com.nxp.nfc \
    tcmiface \
    WfdCommon \
    com.qti.snapdragon.sdk.display \
    qcnvitems \
    qcrilhook

# Netutils
PRODUCT_PACKAGES += \
    netutils-wrapper-1.0 \
    libandroid_net

#PRODUCT_PACKAGES += \
    DeviceParts

#PRODUCT_PACKAGES += \
    vndk_package

PRODUCT_PACKAGES += \
    android.hidl.base@1.0

PRODUCT_PACKAGES += \
    vendor.display.config@1.7 \
    libdisplayconfig \
    libqdMetaData.system \
    vendor.nxp.nxpese@1.0 \
    vendor.nxp.nxpnfc@1.0 \
    vendor.oneplus.camera.CameraHIDL@1.0 \
    vendor.oneplus.fingerprint.extension@1.0 \
    vendor.qti.hardware.camera.device@1.0

# Display
PRODUCT_PACKAGES += \
    libion \
    libtinyxml2

PRODUCT_PACKAGES += \
    libtinyalsa

PRODUCT_PACKAGES += \
    ld.config.txt

# Support for the O-MR1 devices
#PRODUCT_COPY_FILES += \
    build/make/target/product/vndk/init.gsi.rc:system/etc/init/init.gsi.rc \
    build/make/target/product/vndk/init.vndk-27.rc:system/etc/init/gsi/init.vndk-27.rc

#PRODUCT_EXTRA_VNDK_VERSIONS := 27

# TODO(b/78308559): includes vr_hwc into GSI before vr_hwc move to vendor
PRODUCT_PACKAGES += \
    vr_hwc

PRODUCT_COPY_FILES += \
    frameworks/av/media/libstagefright/data/media_codecs_google_audio.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/media_codecs_google_audio.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_telephony.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/media_codecs_google_telephony.xml \
    frameworks/av/media/libstagefright/data/media_codecs_google_video.xml:$(TARGET_COPY_OUT_SYSTEM)/etc/media_codecs_google_video.xml

PRODUCT_PACKAGES += android.hardware.health@2.0-service.oneplus6
#DEVICE_FRAMEWORK_MANIFEST_FILE += \
    system/libhidl/vintfdata/manifest_healthd_exclude.xml

# Temporary handling
#
# Include config.fs get only if legacy device/qcom/<target>/android_filesystem_config.h
# does not exist as they are mutually exclusive.  Once all target's android_filesystem_config.h
# have been removed, TARGET_FS_CONFIG_GEN should be made unconditional.
DEVICE_CONFIG_DIR := $(dir $(firstword $(subst ]],, $(word 2, $(subst [[, ,$(_node_import_context))))))
ifeq ($(wildcard device/oneplus/oneplus6/android_filesystem_config.h),)
  TARGET_FS_CONFIG_GEN := device/oneplus/oneplus6/config.fs
else
  $(warning **********)
  $(warning TODO: Need to replace legacy $(DEVICE_CONFIG_DIR)android_filesystem_config.h with config.fs)
  $(warning **********)
endif

include device/oneplus/oneplus6/keylayout/keylayout.mk
