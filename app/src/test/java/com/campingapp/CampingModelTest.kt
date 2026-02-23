package com.campingapp

import com.campingapp.model.Camping
import com.campingapp.model.Comment
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CampingModelTest {

    @Test
    fun camping_defaultValues_areEmpty() {
        val camping = Camping()
        assertEquals("", camping.id)
        assertEquals("", camping.name)
        assertEquals("", camping.municipality)
    }

    @Test
    fun camping_propertiesSetCorrectly() {
        val camping = Camping(
            id = "1V",
            name = "CAMPING EL SALER",
            municipality = "VALENCIA",
            province = "VALENCIA",
            category = "2ª",
            capacity = "500"
        )
        assertEquals("1V", camping.id)
        assertEquals("CAMPING EL SALER", camping.name)
        assertEquals("VALENCIA", camping.municipality)
        assertEquals("2ª", camping.category)
        assertEquals("500", camping.capacity)
    }

    @Test
    fun comment_defaultId_isZero() {
        val comment = Comment(campingId = "1V", text = "Great camping!")
        assertEquals(0L, comment.id)
        assertEquals("1V", comment.campingId)
        assertEquals("Great camping!", comment.text)
        assertTrue(comment.createdAt > 0)
    }
}
