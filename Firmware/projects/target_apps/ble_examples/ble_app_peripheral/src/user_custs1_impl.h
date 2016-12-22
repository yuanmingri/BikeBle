/**
 ****************************************************************************************
 *
 * @file user_custs1_impl.h
 *
 * @brief Peripheral project Custom1 Server implementation header file.
 *
 * Copyright (C) 2015. Dialog Semiconductor Ltd, unpublished work. This computer
 * program includes Confidential, Proprietary Information and is a Trade Secret of
 * Dialog Semiconductor Ltd.  All use, disclosure, and/or reproduction is prohibited
 * unless authorized in writing. All Rights Reserved.
 *
 * <bluetooth.support@diasemi.com> and contributors.
 *
 ****************************************************************************************
 */

#ifndef _USER_CUSTS1_IMPL_H_
#define _USER_CUSTS1_IMPL_H_

/**
 ****************************************************************************************
 * @addtogroup APP
 * @ingroup RICOW
 *
 * @brief
 *
 * @{
 ****************************************************************************************
 */

/*
 * DEFINES
 ****************************************************************************************
 */

enum
{
    CUSTS1_CP_ADC_VAL1_DISABLE = 0,
    CUSTS1_CP_ADC_VAL1_ENABLE,
};

enum
{
    CUSTS1_LED_OFF = 0,
    CUSTS1_LED_ON,
};


/*
 * INCLUDE FILES
 ****************************************************************************************
 */

#include "gapc_task.h"                 // gap functions and messages
#include "gapm_task.h"                 // gap functions and messages
#include "custs1_task.h"

#define USE_SPI_FLASH_FOR_SETTING

#define LED1_PORT GPIO_PORT_0
#define LED1_PIN	GPIO_PIN_6

#define LED2_PORT GPIO_PORT_0
#define LED2_PIN	GPIO_PIN_7

#define LED3_PORT GPIO_PORT_1
#define LED3_PIN	GPIO_PIN_2

#define LED4_PORT GPIO_PORT_1
#define LED4_PIN	GPIO_PIN_0

typedef struct {
		bool led_on;
		bool led_state;
		bool connected;
		uint16_t delay;
}app_state_t;

extern app_state_t gstate;
/*
 * FUNCTION DECLARATIONS
 ****************************************************************************************
 */

/**
 ****************************************************************************************
 * @brief Led state value write indication handler.
 * @param[in] msgid   Id of the message received.
 * @param[in] param   Pointer to the parameters of the message.
 * @param[in] dest_id ID of the receiving task instance.
 * @param[in] src_id  ID of the sending task instance.
 * @return void
 ****************************************************************************************
*/
void user_custs1_led_wr_ind_handler(ke_msg_id_t const msgid,
                                     struct custs1_val_write_ind const *param,
                                     ke_task_id_t const dest_id,
                                     ke_task_id_t const src_id);

void user_custs1_control_wr_ind_handler(ke_msg_id_t const msgid,
																		struct custs1_val_write_ind const *param,
																		ke_task_id_t const dest_id,
																		ke_task_id_t const src_id);

void user_led_timer_handler(void);
void user_cust1_on_connect_handler(void);
void user_cust1_on_disconnect_handler(void);
void user_cust1_init(void);
void set_led_state(void);

//#define USE_SPI_FLASH_FOR_CONFIG
																		
#ifdef USE_SPI_FLASH_FOR_CONFIG
																		
#define CONFIG_SPI_FLASH_ADDR	0x1F000

void load_config(void);
void save_config(void);																		
#endif
																		
#endif // _USER_CUSTS1_IMPL_H_
