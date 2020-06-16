package ru.ifmo.rain.rasho.bank.common;

import ru.ifmo.rain.rasho.bank.server.bank.RemoteBank;

import java.rmi.Remote;
import java.rmi.RemoteException;

/**
 * @see RemoteBank
 */
public interface Bank extends Remote {

    /**
     * Creates a new account with identifier {@code accountId} of specified person if it doesn't exist.
     *
     * @param name      name of the account owner
     * @param surname   surname of the account owner
     * @param passport  passport identifier of the account owner
     * @param accountId account identifier
     * @return created or existing account.
     */
    Account createAccount(final String name, final String surname, final int passport,
                          final int accountId) throws RemoteException;

    /**
     * Getter for account with identifier {@code accountId} ot person specified by {@code passport}.
     *
     * @param passport  passport identifier of the account owner
     * @param accountId account identifier
     * @return {@link Account} if it exists and {@code null} otherwise.
     */
    Account getAccount(final int passport, final int accountId) throws RemoteException;

    /**
     * Getter for remote version of bank customer specified by {@code passport}.
     *
     * @param passport passport identifier
     * @return {@link Person} if the required person is a client of the bank and {@code null} otherwise.
     */
    Person getRemotePerson(final int passport) throws RemoteException;

    /**
     * Getter for local version of bank customer specified by {@code passport}.
     *
     * @param passport passport identifier
     * @return {@link Person} if the required person is a client of the bank and {@code null} otherwise.
     */
    Person getLocalPerson(final int passport) throws RemoteException;

    /**
     * Checks if the account specified by {@code passport} and {@code accountId} exists.
     *
     * @param passport  passport identifier of the account owner
     * @param accountId account identifier
     * @return {@code true} if the required account exists and {@code false} otherwise.
     */
    boolean containsAccount(final int passport, final int accountId) throws RemoteException;

    /**
     * Checks if the person specified by {@code passport} is the account owner.
     *
     * @param passport passport identifier
     * @return {@code true} if the required person owns the account and {@code false} otherwise.
     */
    boolean containsPerson(final int passport) throws RemoteException;
}
