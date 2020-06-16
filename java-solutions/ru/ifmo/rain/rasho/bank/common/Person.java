package ru.ifmo.rain.rasho.bank.common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.concurrent.ConcurrentMap;
import java.util.Map;

import ru.ifmo.rain.rasho.bank.server.person.*;

/**
 * Interface of classes representing a person who owns bank account.
 *
 * @see RemotePerson
 * @see LocalPerson
 */
public interface Person extends Remote {

    /**
     * Getter for the first name of the person.
     *
     * @return {@link String} representing person's first name.
     */
    String getName() throws RemoteException;

    /**
     * Getter for the surname of the person.
     *
     * @return {@link String} representing person's surname.
     */
    String getSurname() throws RemoteException;

    /**
     * Getter for the passport identifier of the person.
     *
     * @return passport identifier.
     */
    int getPassport() throws RemoteException;

    /**
     * Getter for account of the person specified by {@code accountId}.
     *
     * @param accountId account identifier
     * @return {@link Account} if the required account exists and {@code null} otherwise.
     * @throws RemoteException
     */
    Account getAccount(int accountId) throws RemoteException;

    /**
     * Getter for the {@link Map} representing person's {@link Account} by its account identifier.
     *
     * @return {@link ConcurrentMap}.
     */
    ConcurrentMap<Integer, Account> getAccountByIdMap() throws RemoteException;

    /**
     * Adds the new bank account {@link Account} for the person.
     *
     * @param account {@link Account} representing account to add.
     */
    void addAccount(Account account) throws RemoteException;
}
