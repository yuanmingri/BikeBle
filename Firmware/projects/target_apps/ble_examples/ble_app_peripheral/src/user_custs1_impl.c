/**
 ****************************************************************************************
 *
 * @file user_custs1_impl.c
 *
 * @brief Peripheral project Custom1 Server implementation source code.
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

/*
 * INCLUDE FILES
 ****************************************************************************************
 */

#include "gpio.h"
#include "app_api.h"
#include "app.h"
#include "user_custs1_def.h"
#include "user_custs1_impl.h"
#include "user_peripheral.h"
#include "user_periph_setup.h"
#include "battery.h"
#include "arch_console.h"
#include "spi_flash.h"
/*
 * GLOBAL VARIABLE DEFINITIONS
 ****************************************************************************************
 */
timer_hnd led_tmr_hndl;
ke_msg_id_t timer_used;

GPIO_PORT LED_GPIO_PORTS[4] = { LED1_PORT, LED2_PORT, LED3_PORT, LED4_PORT };
GPIO_PIN LED_GPIO_PINS[4] = { LED1_PIN, LED2_PIN, LED3_PIN, LED4_PIN };

app_state_t gstate __attribute__((section("retention_mem_area0"), zero_init));

void user_custs1_led_wr_ind_handler(ke_msg_id_t const msgid,
                                     struct custs1_val_write_ind const *param,
                                     ke_task_id_t const dest_id,
                                     ke_task_id_t const src_id)
{
    uint8_t val = 0;
    memcpy(&val, &param->value[0], param->length);

    if (val == CUSTS1_LED_ON)
        gstate.led_state = true;
    else if (val == CUSTS1_LED_OFF)
        gstate.led_state = false;
		set_led_state();
}

void user_custs1_control_wr_ind_handler(ke_msg_id_t const msgid,
                                     struct custs1_val_write_ind const *param,
                                     ke_task_id_t const dest_id,
                                     ke_task_id_t const src_id)
{
	
}

void set_led_state(void)
{
	int i;
	for(i = 0; i < 4; i++)
	{
		GPIO_ConfigurePin(LED_GPIO_PORTS[i],LED_GPIO_PINS[i],OUTPUT,PID_GPIO,gstate.led_state);
	}
}

void user_led_timer_handler(void)
{
#if 0
		if(!gstate.connected)
		{
				app_easy_timer_cancel( led_tmr_hndl);
		}
		else
		{
				led_tmr_hndl = app_easy_timer( gstate.led_delay, user_led_timer_handler );  
		}
#endif
}   

void user_cust1_on_connect_handler(void)
{
	gstate.connected = true;
	arch_puts("connected\r\n");
	//led_tmr_hndl = app_easy_timer( gstate.led_delay, user_led_timer_handler );
}

void user_cust1_on_disconnect_handler(void)
{
	gstate.connected = false;
	gstate.led_state = false;
	set_led_state();
}

void user_cust1_init(void)
{
		gstate.led_state = false;
		set_led_state();
	
#ifdef USE_SPI_FLASH_FOR_CONFIG
		load_config();
#endif
		arch_puts("cust1_init\r\n");
}

#ifdef USE_SPI_FLASH_FOR_CONFIG

static void config_spi_flash_init(void)
{
    static int8_t dev_id;

    dev_id = spi_flash_enable(SPI_EN_GPIO_PORT, SPI_EN_GPIO_PIN);
    if (dev_id == SPI_FLASH_AUTO_DETECT_NOT_DETECTED)
    {
        // The device was not identified. The default parameters are used.
        // Alternatively, an error can be asserted here.
        spi_flash_init(SPI_FLASH_DEFAULT_SIZE, SPI_FLASH_DEFAULT_PAGE);
    }
}


void load_config()
{
		uint8_t *pdata = (uint8_t*)&gstate.cfg;
		int i;
	
		config_spi_flash_init();

    spi_flash_read_data((uint8_t *)&gstate.cfg, CONFIG_SPI_FLASH_ADDR, sizeof(config_led_t));

    spi_release();
		
		for(i = 0; i < sizeof(config_led_t); i++)
		{
				if(pdata[i] == 0xFF)
					pdata[i] = 0;
		}
}

void save_config(void)
{
		uint32_t sector_nb;
    uint32_t offset;
    int8_t ret;
    int i;
	
		config_spi_flash_init();
	
    // Calculate the starting sector offset
    offset = (CONFIG_SPI_FLASH_ADDR / SPI_SECTOR_SIZE) * SPI_SECTOR_SIZE;

    // Calculate the numbers of sectors to erase
    sector_nb = (sizeof(config_led_t) / SPI_SECTOR_SIZE);
    if (sizeof(config_led_t) % SPI_SECTOR_SIZE)
        sector_nb++;

    // Erase flash sectors
    for (i = 0; i < sector_nb; i++)
    {
        ret = spi_flash_block_erase(offset, SECTOR_ERASE);
        offset += SPI_SECTOR_SIZE;
        if (ret != ERR_OK)
            break;
		}
		
		if (ret == ERR_OK)
    {
        spi_flash_write_data((uint8_t *)&gstate.cfg, CONFIG_SPI_FLASH_ADDR, sizeof(config_led_t));
    }

    spi_release();
}
#endif
