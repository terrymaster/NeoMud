package com.neomud.client.ui.components

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.neomud.client.testutil.TestData
import com.neomud.client.testutil.TestThemeWrapper
import com.neomud.shared.model.EffectType
import org.junit.Rule
import org.junit.Test

class PlayerStatusPanelScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(
        deviceConfig = DeviceConfig.PIXEL_6
    )

    @Test
    fun fullHp() {
        paparazzi.snapshot {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(currentHp = 100, maxHp = 100, currentMp = 50, maxMp = 50),
                    activeEffects = emptyList(),
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun lowHp() {
        paparazzi.snapshot {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(currentHp = 15, maxHp = 100, currentMp = 5, maxMp = 50),
                    activeEffects = emptyList(),
                    onClick = {}
                )
            }
        }
    }

    @Test
    fun withEffects() {
        paparazzi.snapshot {
            TestThemeWrapper {
                PlayerStatusPanel(
                    player = TestData.player(),
                    activeEffects = listOf(
                        TestData.activeEffect(name = "Poison", type = EffectType.POISON),
                        TestData.activeEffect(name = "Haste", type = EffectType.HASTE),
                        TestData.activeEffect(name = "Strength", type = EffectType.BUFF_STRENGTH)
                    ),
                    onClick = {}
                )
            }
        }
    }
}
