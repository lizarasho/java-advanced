package ru.ifmo.rain.rasho.bank.server.person;

import ru.ifmo.rain.rasho.bank.common.Account;
import ru.ifmo.rain.rasho.bank.common.Person;

import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class AbstractPerson implements Person {

    private final String name;
    private final String surname;
    private final int passport;
    private final ConcurrentMap<Integer, Account> accountById;

    public AbstractPerson(String name, String surname, int passport, ConcurrentMap<Integer, Account> accountById) {
        this.name = name;
        this.surname = surname;
        this.passport = passport;
        this.accountById = accountById;
    }

    public AbstractPerson(String name, String surname, int passport) {
        this(name, surname, passport, new ConcurrentHashMap<>());
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getSurname() {
        return surname;
    }

    @Override
    public int getPassport() {
        return passport;
    }

    @Override
    public synchronized Account getAccount(int accountId) {
        return accountById.getOrDefault(accountId, null);
    }

    @Override
    public synchronized ConcurrentMap<Integer, Account> getAccountByIdMap() {
        return accountById;
    }

    @Override
    public synchronized void addAccount(Account account) throws RemoteException {
        accountById.put(account.getId(), account);
    }
}
