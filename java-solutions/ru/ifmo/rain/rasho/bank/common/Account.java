package ru.ifmo.rain.rasho.bank.common;

import java.rmi.Remote;
import java.rmi.RemoteException;

import ru.ifmo.rain.rasho.bank.server.account.*;

/**
 * Interface of classes representing bank accounts.
 *
 * @see RemoteAccount
 * @see LocalAccount
 */
public interface Account extends Remote {

    /**
     * @return account identifier
     */
    int getId() throws RemoteException;

    /**
     * @return amount of money on the account
     */
    int getAmount() throws RemoteException;

    /**
     * Changes current amount of money on the account to the value specified by {@code amount}
     *
     * @param amount a new amount of the account
     */
    void setAmount(int amount) throws RemoteException;

    /**
     * Adds value specified by {@code amount} to the current amount of money on the account
     *
     * @param amount a value to add
     */
    void addAmount(int amount) throws RemoteException;

}