#
# SPDX-FileCopyrightText: The LineageOS Project
# SPDX-License-Identifier: Apache-2.0
#

# Common
$(call inherit-product, $(LOCAL_PATH)/common.mk)

# Init
PRODUCT_PACKAGES += \
    init.m23xq.rc

$(call soong_config_set,libinit,vendor_init_lib,//$(LOCAL_PATH):libinit_samsung_m23xq)

# NFC
PRODUCT_PACKAGES += \
    android.hardware.nfc-service.nxp

PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/nfc/libnfc-nxp_RF.conf:$(TARGET_COPY_OUT_VENDOR)/etc/libnfc-nxp_RF.conf

# Sensors
PRODUCT_COPY_FILES += \
    $(LOCAL_PATH)/configs/sensors/hals.conf:$(TARGET_COPY_OUT_VENDOR)/etc/sensors/hals.conf
