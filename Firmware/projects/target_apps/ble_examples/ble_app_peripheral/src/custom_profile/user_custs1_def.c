/**
 ****************************************************************************************
 *
 * @file user_custs1_def.c
 *
 * @brief Custom1 Server (CUSTS1) profile database definitions.
 *
 * Copyright (C) 2016. Dialog Semiconductor Ltd, unpublished work. This computer
 * program includes Confidential, Proprietary Information and is a Trade Secret of
 * Dialog Semiconductor Ltd.  All use, disclosure, and/or reproduction is prohibited
 * unless authorized in writing. All Rights Reserved.
 *
 * <bluetooth.support@diasemi.com> and contributors.
 *
 ****************************************************************************************
 */

/**
 ****************************************************************************************
 * @defgroup USER_CONFIG
 * @ingroup USER
 * @brief Custom1 Server (CUSTS1) profile database definitions.
 *
 * @{
 ****************************************************************************************
 */

/*
 * INCLUDE FILES
 ****************************************************************************************
 */

#include <stdint.h>
#include "prf_types.h"
#include "attm_db_128.h"
#include "user_custs1_def.h"

/*
 * LOCAL VARIABLE DEFINITIONS
 ****************************************************************************************
 */

static att_svc_desc128_t custs1_svc                              = DEF_CUST1_SVC_UUID_128;

static uint8_t CUST1_CTRL_POINT_UUID_128[ATT_UUID_128_LEN]       = DEF_CUST1_CTRL_POINT_UUID_128;
static uint8_t CUST1_LED_STATE_UUID_128[ATT_UUID_128_LEN]        = DEF_CUST1_LED_STATE_UUID_128;
static uint8_t CUST1_ADC_VAL_1_UUID_128[ATT_UUID_128_LEN]        = DEF_CUST1_ADC_VAL_1_UUID_128;
static uint8_t CUST1_ADC_VAL_2_UUID_128[ATT_UUID_128_LEN]        = DEF_CUST1_ADC_VAL_2_UUID_128;
static uint8_t CUST1_BUTTON_STATE_UUID_128[ATT_UUID_128_LEN]     = DEF_CUST1_BUTTON_STATE_UUID_128;
static uint8_t CUST1_INDICATEABLE_UUID_128[ATT_UUID_128_LEN]     = DEF_CUST1_INDICATEABLE_UUID_128;
static uint8_t CUST1_LONG_VALUE_UUID_128[ATT_UUID_128_LEN]       = DEF_CUST1_LONG_VALUE_UUID_128;

static struct att_char128_desc custs1_ctrl_point_char       = {ATT_CHAR_PROP_WR,
                                                              {0, 0},
                                                              DEF_CUST1_CTRL_POINT_UUID_128};

static struct att_char128_desc custs1_led_state_char        = {ATT_CHAR_PROP_WR | ATT_CHAR_PROP_RD,
                                                              {0, 0},
                                                              DEF_CUST1_LED_STATE_UUID_128};

static struct att_char128_desc custs1_adc_val_1_char        = {ATT_CHAR_PROP_RD | ATT_CHAR_PROP_NTF,
                                                              {0, 0},
                                                              DEF_CUST1_ADC_VAL_1_UUID_128};

static struct att_char128_desc custs1_adc_val_2_char        = {ATT_CHAR_PROP_RD,
                                                              {0, 0},
                                                              DEF_CUST1_ADC_VAL_2_UUID_128};

static struct att_char128_desc custs1_button_state_char     = {ATT_CHAR_PROP_RD | ATT_CHAR_PROP_NTF,
                                                              {0, 0},
                                                              DEF_CUST1_BUTTON_STATE_UUID_128};

static struct att_char128_desc custs1_indicateable_char     = {ATT_CHAR_PROP_RD | ATT_CHAR_PROP_IND,
                                                              {0, 0},
                                                              DEF_CUST1_INDICATEABLE_UUID_128};

static struct att_char128_desc custs1_long_value_char       = {ATT_CHAR_PROP_RD | ATT_CHAR_PROP_WR | ATT_CHAR_PROP_NTF,
                                                              {0, 0},
                                                              DEF_CUST1_LONG_VALUE_UUID_128};

static uint16_t att_decl_svc       = ATT_DECL_PRIMARY_SERVICE;
static uint16_t att_decl_char      = ATT_DECL_CHARACTERISTIC;
static uint16_t att_decl_cfg       = ATT_DESC_CLIENT_CHAR_CFG;
static uint16_t att_decl_user_desc = ATT_DESC_CHAR_USER_DESCRIPTION;

/*
 * GLOBAL VARIABLE DEFINITIONS
 ****************************************************************************************
 */

/// Full CUSTOM1 Database Description - Used to add attributes into the database
struct attm_desc_128 custs1_att_db[CUST1_IDX_NB] =
{
    // CUSTOM1 Service Declaration
    [CUST1_IDX_SVC]                     = {(uint8_t*)&att_decl_svc, ATT_UUID_16_LEN, PERM(RD, ENABLE),
                                            sizeof(custs1_svc), sizeof(custs1_svc), (uint8_t*)&custs1_svc},

    // Control Point Characteristic Declaration
    [CUST1_IDX_CONTROL_POINT_CHAR]      = {(uint8_t*)&att_decl_char, ATT_UUID_16_LEN, PERM(RD, ENABLE),
                                            sizeof(custs1_ctrl_point_char), sizeof(custs1_ctrl_point_char), (uint8_t*)&custs1_ctrl_point_char},

    // Control Point Characteristic Value
    [CUST1_IDX_CONTROL_POINT_VAL]       = {CUST1_CTRL_POINT_UUID_128, ATT_UUID_128_LEN, PERM(WR, ENABLE),
                                            DEF_CUST1_CTRL_POINT_CHAR_LEN, 0, NULL},

    
    // LED State Characteristic Declaration
    [CUST1_IDX_LED_STATE_CHAR]          = {(uint8_t*)&att_decl_char, ATT_UUID_16_LEN, PERM(RD, ENABLE),
                                            sizeof(custs1_led_state_char), sizeof(custs1_led_state_char), (uint8_t*)&custs1_led_state_char},

    // LED State Characteristic Value
    [CUST1_IDX_LED_STATE_VAL]           = {CUST1_LED_STATE_UUID_128, ATT_UUID_128_LEN, PERM(RD, ENABLE) | PERM(WR, ENABLE),
                                            DEF_CUST1_LED_STATE_CHAR_LEN, 0, NULL},

    
};

/// @} USER_CONFIG
