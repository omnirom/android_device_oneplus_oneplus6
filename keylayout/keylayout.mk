ifeq ($(TARGET_DEVICE),$(filter $(TARGET_DEVICE),oneplus6))
PRODUCT_COPY_FILES += \
    device/oneplus/oneplus6/keylayout/gf_input.kl:system/usr/keylayout/gf_input.kl
else
PRODUCT_COPY_FILES += \
    device/oneplus/oneplus6/keylayout/gf_input_op6t.kl:system/usr/keylayout/gf_input.kl
endif
