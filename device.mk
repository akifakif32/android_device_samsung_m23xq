#
# SPDX-FileCopyrightText: The Infinity-X Project
# SPDX-License-Identifier: Apache-2.0
#
#
LOCAL_PATH := device/samsung/m23xq

DEVICE_PACKAGE_OVERLAYS += \
    $(LOCAL_PATH)/overlay \
    WifiResDevice

BOARD_BLUETOOTH_BDROID_BUILDCFG_INCLUDE_DIR := $(COMMON_PATH)/bluetooth/include

# call the common setup
$(call inherit-product, device/samsung/sm7225-common/common.mk)

# call the proprietary setup
$(call inherit-product, vendor/samsung/m23xq/m23xq-vendor.mk)

# Init files
PRODUCT_PACKAGES += \
    init.m23xq.rc

# Audio
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/audio/mixer_paths.xml:$(TARGET_COPY_OUT_VENDOR)/etc/mixer_paths.xml

TARGET_VENDOR_PROP += $(LOCAL_PATH)/vendor.prop
TARGET_SYSTEM_PROP += $(LOCAL_PATH)/system.prop

# Soong namespaces
PRODUCT_SOONG_NAMESPACES += \
    $(LOCAL_PATH)
