mkdir input
cp -f ../projects/target_apps/ble_examples/ble_app_peripheral/Keil_5/out_583/ble_app_peripheral_583.hex input/fw_1.hex
cp -f ../projects/target_apps/ble_examples/ble_app_peripheral/Keil_5/out_583/ble_app_peripheral_583.hex input/fw_2.hex
cp -f ../sdk/platform/include/ble_580_sw_version.h input/fw_version_1.h
cp -f ../sdk/platform/include/ble_580_sw_version.h input/fw_version_2.h

mkdir output
hex2bin input/fw_1.hex
hex2bin input/fw_2.hex
mkimage single input/fw_1.bin input/fw_version_1.h output/fw_image_1.img
mkimage single input/fw_2.bin input/fw_version_2.h output/fw_image_2.img
mkimage multi spi secondary_bootloader.bin output/fw_image_1.img 0x8000 output/fw_image_2.img 0x13000 0x1F000 output/fw_multi_part_spi.bin
