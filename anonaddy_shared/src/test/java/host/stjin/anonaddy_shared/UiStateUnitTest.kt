package host.stjin.anonaddy_shared

import host.stjin.anonaddy_shared.models.UiState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UiStateUnitTest {

    @Test
    fun testUiStateLoading() {
        val state: UiState<String> = UiState.Loading
        assertTrue(state.isLoading)
        assertFalse(state.isSuccess)
        assertFalse(state.isError)
    }

    @Test
    fun testUiStateSuccess() {
        val state: UiState<String> = UiState.Success("test_data")
        assertFalse(state.isLoading)
        assertTrue(state.isSuccess)
        assertFalse(state.isError)
        assertEquals("test_data", state.getOrNull())
    }

    @Test
    fun testUiStateErrorWithStatusCode() {
        val state: UiState<String> = UiState.Error("404", 404)
        assertFalse(state.isLoading)
        assertFalse(state.isSuccess)
        assertTrue(state.isError)
        val errorState = state as UiState.Error
        assertEquals("404", errorState.message)
        assertEquals(404, errorState.statusCode)
    }
}
