package ru.ifmo.rain.rasho.bank.tests;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.ifmo.rain.rasho.bank.client.Client;

import static org.junit.jupiter.api.Assertions.assertThrows;

@DisplayName("Client tests")
class ClientTests {

    private static final String NAME = "Name";
    private static final String SURNAME = "Surname";
    private static final String PASSPORT = "4514514";
    private static final String ACCOUNT_ID = "1074823";
    private static final String AMOUNT = "100000000";

    @DisplayName("Less that 5 main arguments")
    @Test
    void testFewArguments() {
        assertThrows(IllegalArgumentException.class, () -> Client.main(NAME));
    }

    @DisplayName("More that 5 main arguments")
    @Test
    void testManyArguments() {
        assertThrows(IllegalArgumentException.class, () -> Client.main(NAME, SURNAME, PASSPORT, ACCOUNT_ID, AMOUNT, "unnecessary argument"));
    }

    @DisplayName("Non-integer passport")
    @Test
    void testNonIntegerPassport() {
        assertThrows(RuntimeException.class, () -> Client.main(NAME, SURNAME, "passport", ACCOUNT_ID, AMOUNT));
    }

    @DisplayName("Non-integer account id")
    @Test
    void testNonIntegerAccountId() {
        assertThrows(RuntimeException.class, () -> Client.main(NAME, SURNAME, PASSPORT, "account id", AMOUNT));
    }

    @DisplayName("Non-integer amount of change")
    @Test
    void testNonIntegerAmount() {
        assertThrows(RuntimeException.class, () -> Client.main(NAME, SURNAME, PASSPORT, ACCOUNT_ID, "amount"));
    }

    @DisplayName("Integer overflow of passport")
    @Test
    void testPassportOverflow() {
        assertThrows(RuntimeException.class, () -> Client.main(NAME, SURNAME, Long.toString(Long.MAX_VALUE), ACCOUNT_ID, AMOUNT));
    }

    @DisplayName("Integer overflow of account id")
    @Test
    void testAccountIdOverflow() {
        assertThrows(RuntimeException.class, () -> Client.main(NAME, SURNAME, PASSPORT, Long.toString(Long.MAX_VALUE), AMOUNT));
    }

    @DisplayName("Integer overflow of amount")
    @Test
    void testAmountOverflow() {
        assertThrows(RuntimeException.class, () -> Client.main(NAME, SURNAME, PASSPORT, ACCOUNT_ID, Long.toString(Long.MAX_VALUE)));
    }

    @DisplayName("Null argument")
    @Test
    void testNullArgument() {
        assertThrows(IllegalArgumentException.class, () -> Client.main(NAME, SURNAME, null, ACCOUNT_ID, AMOUNT));
    }
}
