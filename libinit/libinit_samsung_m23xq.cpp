/*
 * Copyright (C) 2021-2024 The LineageOS Project
 *
 * SPDX-License-Identifier: Apache-2.0
 */

#include <libinit_variant.h>

#include "vendor_init.h"

static const variant_info_t m23xqxx_info = {
    .device = "m23xq",
    .model = "SM-M236B",
    .name = "m23xqxx",
    .build_fingerprint = "samsung/m23xqxx/m23xq:11/RP1A.200720.012/M236BXXSEEZB2:user/release-keys",
    .build_desc = "m23xqxx-user 14 UP1A.231005.007 M236BXXSEEZB2 release-keys"
};

static const variant_info_t m23xqdd_info = {
    .device = "m23xq",
    .model = "SM-E236B",
    .name = "m23xqdd",
    .build_fingerprint = "samsung/m23xqdd/m23xq:11/RP1A.200720.012/E236BXXSEEZB1:user/release-keys",
    .build_desc = "m23xqdd-user 14 UP1A.231005.007 E236BXXSEEZB1 release-keys"
};

static const variant_info_t m23xqjpn_info = {
    .device = "m23xq",
    .model = "SM-M236Q",
    .name = "m23xqjpn",
    .build_fingerprint = "samsung/m23xqjpn/m23xq:11/RP1A.200720.012/M236QOPS7CZB2:user/release-keys",
    .build_desc = "m23xqjpn-user 14 UP1A.231005.007 M236QOPS7CZB2 release-keys"
};

static const variant_info_t m23xqlgt_info = {
    .device = "m23xq",
    .model = "SM-M236L",
    .name = "m23xqlgt",
    .build_fingerprint = "samsung/m23xqlgt/m23xq:11/RP1A.200720.012/M236LKSSDEZB2:user/release-keys",
    .build_desc = "m23xqlgt-user 14 UP1A.231005.007 M236LKSSDEZB2 release-keys"
};

static const std::vector<variant_info_t> variants = {
    m23xqxx_info,
    m23xqdd_info,
    m23xqjpn_info,
    m23xqlgt_info,
};

void vendor_load_properties() {
    search_variant(variants);
}
