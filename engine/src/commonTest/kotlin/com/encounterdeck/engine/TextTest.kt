package com.encounterdeck.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class TextTest {

    @Test
    fun `count of one is singular`() {
        assertEquals("1 Ogre", countLabel("Ogre", 1))
    }

    @Test
    fun `regular plural adds s`() {
        assertEquals("2 Vine Blights", countLabel("Vine Blight", 2))
    }

    @Test
    fun `f becomes ves`() {
        assertEquals("3 Dire Wolves", countLabel("Dire Wolf", 3))
    }

    @Test
    fun `consonant plus y becomes ies`() {
        assertEquals("2 Harpies", countLabel("Harpy", 2))
    }
}
