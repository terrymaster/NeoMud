package com.neomud.shared

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VersionComparisonTest {

    @Test
    fun equal_versions_return_zero() {
        assertEquals(0, NeoMudVersion.compareVersions("0.1.0", "0.1.0"))
        assertEquals(0, NeoMudVersion.compareVersions("1.0.0", "1.0.0"))
    }

    @Test
    fun three_part_equals_four_part_with_zero_padding() {
        assertEquals(0, NeoMudVersion.compareVersions("0.1.0", "0.1.0.0"))
        assertEquals(0, NeoMudVersion.compareVersions("1.0.0.0", "1.0.0"))
    }

    @Test
    fun higher_major_version_is_greater() {
        assertTrue(NeoMudVersion.compareVersions("1.0.0", "0.99.99") > 0)
        assertTrue(NeoMudVersion.compareVersions("2.0.0", "1.9.9") > 0)
    }

    @Test
    fun higher_minor_version_is_greater() {
        assertTrue(NeoMudVersion.compareVersions("0.2.0", "0.1.0") > 0)
        assertTrue(NeoMudVersion.compareVersions("0.10.0", "0.9.0") > 0)
    }

    @Test
    fun higher_patch_version_is_greater() {
        assertTrue(NeoMudVersion.compareVersions("0.1.1", "0.1.0") > 0)
    }

    @Test
    fun lower_version_is_negative() {
        assertTrue(NeoMudVersion.compareVersions("0.1.0", "0.2.0") < 0)
        assertTrue(NeoMudVersion.compareVersions("0.1.0.0", "0.1.0.1") < 0)
    }

    @Test
    fun four_part_build_number_comparison() {
        assertTrue(NeoMudVersion.compareVersions("0.1.0.5", "0.1.0.3") > 0)
        assertEquals(0, NeoMudVersion.compareVersions("0.1.0.0", "0.1.0.0"))
    }
}
