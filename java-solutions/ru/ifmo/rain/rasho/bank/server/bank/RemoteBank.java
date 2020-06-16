package ru.ifmo.rain.rasho.bank.server.bank;

import ru.ifmo.rain.rasho.bank.common.Account;
import ru.ifmo.rain.rasho.bank.common.Bank;
import ru.ifmo.rain.rasho.bank.common.Person;
import ru.ifmo.rain.rasho.bank.common.Utils;
import ru.ifmo.rain.rasho.bank.server.account.RemoteAccount;
import ru.ifmo.rain.rasho.bank.server.person.LocalPerson;
import ru.ifmo.rain.rasho.bank.server.person.RemotePerson;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RemoteBank implements Bank {
    private final int port;

    private final ConcurrentMap<Integer, Person> personByPassport = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Account> accountById = new ConcurrentHashMap<>();

    public RemoteBank(final int port) {
        this.port = port;
    }

    @Override
    public synchronized Account createAccount(final String name, final String surname,
                                              final int passport, final int accountId) throws RemoteException {
        validate(name, surname, passport);
        if (!containsAccount(passport, accountId)) {
            Utils.log("Creating account " + Utils.getBankAccountId(passport, accountId)
                    + " of " + name + ' ' + surname);
            final Account account = new RemoteAccount(accountId);
            accountById.put(Utils.getBankAccountId(passport, accountId), account);
            UnicastRemoteObject.exportObject(account, port);
            Person person = getRemotePerson(passport);
            if (person == null) {
                person = createPerson(name, surname, passport);
            }
            person.addAccount(account);
            return account;
        }
        return getAccount(passport, accountId);
    }

    @Override
    public Account getAccount(final int passport, final int accountId) {
        Utils.log("Retrieving account " + Utils.getBankAccountId(passport, accountId));
        return accountById.getOrDefault(Utils.getBankAccountId(passport, accountId), null);
    }

    @Override
    public Person getRemotePerson(int passport) {
        Utils.log("Retrieving remote person " + passport);
        return personByPassport.getOrDefault(passport, null);
    }

    @Override
    public Person getLocalPerson(int passport) throws RemoteException {
        Utils.log("Retrieving local person " + passport);
        if (containsPerson(passport)) {
            return new LocalPerson(personByPassport.get(passport));
        }
        return null;
    }

    @Override
    public boolean containsAccount(int passport, int accountId) {
        return accountById.containsKey(Utils.getBankAccountId(passport, accountId));
    }

    @Override
    public boolean containsPerson(int passport) {
        return personByPassport.containsKey(passport);
    }

    private synchronized Person createPerson(final String name, final String surname,
                                             final int passport) throws RemoteException {
        if (!containsPerson(passport)) {
            RemotePerson person = new RemotePerson(name, surname, passport);
            personByPassport.put(passport, person);
            UnicastRemoteObject.exportObject(person, port);
            return person;
        }
        return getRemotePerson(passport);
    }

    private void validate(String name, String surname, int passport) throws RemoteException {
        if (name == null || surname == null) {
            throw new IllegalArgumentException("Non-null name and surname were expected.");
        }
        if (containsPerson(passport)) {
            Person expectedPerson = getRemotePerson(passport);
            String expectedName = expectedPerson.getName();
            String expectedSurname = expectedPerson.getSurname();
            if (!name.equals(expectedName) && !surname.equals(expectedSurname)) {
                throw new IllegalArgumentException(
                        expectedName + ' ' + expectedSurname + "is already registered using this passport number. "
                                + name + ' ' + surname + " can not be registered."
                );
            }
        }
    }
}
