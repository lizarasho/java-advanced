package ru.ifmo.rain.rasho.bank.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ru.ifmo.rain.rasho.bank.common.Account;
import ru.ifmo.rain.rasho.bank.common.Bank;
import ru.ifmo.rain.rasho.bank.common.Person;
import ru.ifmo.rain.rasho.bank.server.bank.RemoteBank;

import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.ExportException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Remote bank tests")
public class BankTests {

    private static Bank bank;
    private static final String URL = "//localhost/bank";

    private static final int THREAD_COUNT = 15;
    private static final int PERSON_COUNT = 100;
    private static final String[] NAMES = new String[PERSON_COUNT];
    private static final String[] SURNAMES = new String[PERSON_COUNT];
    private static final int[] PASSPORTS = new int[PERSON_COUNT];
    private static final int[] ACCOUNT_IDS = new int[PERSON_COUNT];

    @BeforeAll
    public static void init() throws RemoteException, NotBoundException {
        Registry registry;
        try {
            registry = LocateRegistry.createRegistry(Registry.REGISTRY_PORT);
        } catch (ExportException ignored) {
            registry = LocateRegistry.getRegistry(Registry.REGISTRY_PORT);
        }
        registry.rebind(URL, new RemoteBank(Registry.REGISTRY_PORT));
        bank = (Bank) registry.lookup(URL);
        for (int i = 0; i < PERSON_COUNT; i++) {
            NAMES[i] = "Name" + i;
            SURNAMES[i] = "Surname" + i;
            PASSPORTS[i] = ACCOUNT_IDS[i] = i;
        }
    }

    @DisplayName("One account creation")
    @Test
    public void createOneAccount() throws RemoteException {
        bank.createAccount(NAMES[0], SURNAMES[0], PASSPORTS[0], ACCOUNT_IDS[0]);
        assertTrue(bank.containsPerson(PASSPORTS[0]));
    }

    @DisplayName("Check availability of non-existing person")
    @Test
    public void checkNonExistingPerson() throws RemoteException {
        assertFalse(bank.containsPerson(-1));
        assertNull(bank.getRemotePerson(-1));
    }

    @DisplayName("Check availability of non-existing account")
    @Test
    public void checkNonExistingAccount() throws RemoteException {
        assertFalse(bank.containsAccount(PASSPORTS[0], -1));
        assertNull(bank.getAccount(PASSPORTS[0], -1));
    }

    @DisplayName("Test null name")
    @Test
    public void testNullName() {
        assertThrows(IllegalArgumentException.class, () -> bank.createAccount(null, SURNAMES[0], -1, ACCOUNT_IDS[0]));
    }

    @DisplayName("Test null surname")
    @Test
    public void testNullSurname() {
        assertThrows(IllegalArgumentException.class, () -> bank.createAccount(NAMES[0], null, -1, ACCOUNT_IDS[0]));
    }

    @DisplayName("Test creation of people with different names and equal passports")
    @Test
    public void testEqualPassports() {
        assertThrows(IllegalArgumentException.class, () -> {
            bank.createAccount(NAMES[0], SURNAMES[0], PASSPORTS[0], ACCOUNT_IDS[0]);
            bank.createAccount(NAMES[1], SURNAMES[1], PASSPORTS[0], ACCOUNT_IDS[0]);
        });
    }

    @DisplayName("Get local person and verify his properties")
    @Test
    public void getOneLocalPerson() throws RemoteException {
        bank.createAccount(NAMES[0], SURNAMES[0], PASSPORTS[0], ACCOUNT_IDS[0]);
        Person localPerson = bank.getLocalPerson(PASSPORTS[0]);
        checkCreatedPerson(localPerson, NAMES[0], SURNAMES[0], PASSPORTS[0], ACCOUNT_IDS[0]);
    }

    @DisplayName("Get remote person and verify his properties")
    @Test
    public void getOneRemotePerson() throws RemoteException {
        bank.createAccount(NAMES[0], SURNAMES[0], PASSPORTS[0], ACCOUNT_IDS[0]);
        Person remotePerson = bank.getRemotePerson(PASSPORTS[0]);
        checkCreatedPerson(remotePerson, NAMES[0], SURNAMES[0], PASSPORTS[0], ACCOUNT_IDS[0]);
    }

    @DisplayName("Parallel accounts creation")
    @Test
    public void parallelCreateManyAccounts() {
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < PERSON_COUNT; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    checkPersonByIndex(finalI);
                } catch (RemoteException ignored) {
                }
            });
        }
    }

    @DisplayName("One addition to bank account balance")
    @Test
    public void oneAddAmountToAccount() throws RemoteException {
        final int amountAddition = 100;
        bank.createAccount(NAMES[0], SURNAMES[0], PASSPORTS[0], ACCOUNT_IDS[0] + 1);
        Account account = bank.getAccount(PASSPORTS[0], ACCOUNT_IDS[0] + 1);
        int initialBalance = account.getAmount();
        account.addAmount(amountAddition);
        assertEquals(initialBalance + amountAddition, account.getAmount());
    }

    @DisplayName("Parallel additions to bank accounts balances")
    @Test
    void parallelAddAmountToAccount() {
        final int amountAddition = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < PERSON_COUNT; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    bank.createAccount(NAMES[finalI], SURNAMES[finalI], PASSPORTS[finalI], ACCOUNT_IDS[finalI]);
                    Account account = bank.getAccount(PASSPORTS[finalI], ACCOUNT_IDS[finalI]);
                    int initialBalance = account.getAmount();
                    account.addAmount(amountAddition);
                    assertEquals(initialBalance + amountAddition, account.getAmount());
                } catch (RemoteException ignored) {
                }
            });
        }
    }

    @DisplayName("One addition to remote person account balance")
    @Test
    void oneAddAmountToRemotePerson() throws RemoteException {
        final int amountAddition = 100;
        int accountId = ACCOUNT_IDS[0] + 1;
        bank.createAccount(NAMES[0], SURNAMES[0], PASSPORTS[0], accountId);
        Person remotePerson = bank.getRemotePerson(PASSPORTS[0]);
        checkAddToRemotePerson(remotePerson, accountId, amountAddition);
    }

    @DisplayName("Parallel additions to remote person accounts balances")
    @Test
    void parallelAddAmountToRemotePerson() {
        final int amountAddition = 150;
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < PERSON_COUNT; i++) {
            final int finalI = i;
            final int accountId = i + 1;
            executorService.submit(() -> {
                try {
                    bank.createAccount(NAMES[finalI], SURNAMES[finalI], PASSPORTS[finalI], PASSPORTS[accountId]);
                    Person remotePerson = bank.getRemotePerson(finalI);
                    checkAddToRemotePerson(remotePerson, accountId, amountAddition);
                } catch (RemoteException ignored) {
                }
            });
        }
    }

    @DisplayName("One addition to local person account balance")
    @Test
    void oneAddAmountToLocalPerson() throws RemoteException {
        final int amountAddition = -100;
        int accountId = ACCOUNT_IDS[0] + 1;
        bank.createAccount(NAMES[0], SURNAMES[0], PASSPORTS[0], accountId);
        Person localPerson = bank.getLocalPerson(PASSPORTS[0]);
        checkAddToLocalPerson(localPerson, accountId, amountAddition);
    }


    @DisplayName("Parallel additions to local person accounts balances")
    @Test
    void parallelAddAmountToLocalPerson() {
        final int amountAddition = 150;
        ExecutorService executorService = Executors.newFixedThreadPool(THREAD_COUNT);
        for (int i = 0; i < PERSON_COUNT; i++) {
            int finalI = i;
            executorService.submit(() -> {
                try {
                    bank.createAccount(NAMES[finalI], SURNAMES[finalI], PASSPORTS[finalI], ACCOUNT_IDS[finalI] + 1);
                    Person localPerson = bank.getLocalPerson(finalI);
                    checkAddToLocalPerson(localPerson, ACCOUNT_IDS[finalI] + 1, amountAddition);
                } catch (RemoteException ignored) {
                }
            });
        }
    }

    private void checkAddToRemotePerson(Person remotePerson, int accountId, int amountAddition) throws RemoteException {
        int expectedBalance = addAmountToPerson(remotePerson, accountId, amountAddition);
        assertEquals(bank.getAccount(remotePerson.getPassport(), accountId).getAmount(), expectedBalance);
        assertEquals(remotePerson.getAccount(accountId).getAmount(), expectedBalance);
    }

    private void checkAddToLocalPerson(Person localPerson, int accountId, int amountAddition) throws RemoteException {
        int expectedBalance = addAmountToPerson(localPerson, accountId, amountAddition);
        assertNotEquals(bank.getAccount(localPerson.getPassport(), accountId).getAmount(), expectedBalance);
        assertEquals(localPerson.getAccount(accountId).getAmount(), expectedBalance);
    }

    private int addAmountToPerson(Person person, int accountId, int amountAddition) throws RemoteException {
        int initialBalance = person.getAccount(accountId).getAmount();
        person.getAccount(accountId).addAmount(amountAddition);
        return initialBalance + amountAddition;
    }

    private void checkPersonByIndex(int index) throws RemoteException {
        bank.createAccount(NAMES[index], SURNAMES[index], PASSPORTS[index], ACCOUNT_IDS[index]);
        assertTrue(bank.containsPerson(PASSPORTS[index]));
        Person remotePerson = bank.getRemotePerson(index);
        Person localPerson = bank.getLocalPerson(index);
        checkCreatedPerson(remotePerson, NAMES[index], SURNAMES[index], PASSPORTS[index], ACCOUNT_IDS[index]);
        checkCreatedPerson(localPerson, NAMES[index], SURNAMES[index], PASSPORTS[index], ACCOUNT_IDS[index]);
    }

    private void checkCreatedPerson(Person person, String name, String surname, int passport, int accountId)
            throws RemoteException {
        assertTrue(bank.containsPerson(passport));
        assertEquals(name, person.getName());
        assertEquals(surname, person.getSurname());
        assertEquals(passport, person.getPassport());
        assertEquals(0, person.getAccount(accountId).getAmount());
    }
}