package com.salts_inventory_update.integration;

import java.util.ArrayList;
import java.util.List;

import com.salts_inventory_update.api.integration.DesktopDiagnostics;
import com.salts_inventory_update.api.integration.DesktopDiagnostics.Level;

/** A provider's public registrations commit together or roll back when registration fails. */
public final class DesktopRegistrationScope {
    private record Transaction(String owner, List<Runnable> undo) { }
    private static final ThreadLocal<Transaction> CURRENT = new ThreadLocal<>();

    private DesktopRegistrationScope() { }

    public static void run(String owner, Runnable callback) {
        Transaction previous = CURRENT.get();
        Transaction transaction = new Transaction(owner, new ArrayList<>());
        CURRENT.set(transaction);
        try {
            callback.run();
        } catch (RuntimeException | Error failure) {
            for (int i = transaction.undo().size() - 1; i >= 0; i--) {
                try {
                    transaction.undo().get(i).run();
                } catch (RuntimeException | Error rollbackFailure) {
                    failure.addSuppressed(rollbackFailure);
                }
            }
            throw failure;
        } finally {
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }

    public static void onRollback(Runnable undo) {
        Transaction transaction = CURRENT.get();
        if (transaction != null) transaction.undo().add(undo);
    }

    public static IllegalStateException conflict(String description) {
        Transaction transaction = CURRENT.get();
        DesktopDiagnostics.report(Level.ERROR, transaction == null ? "unscoped" : transaction.owner(),
            "registration-conflict", description, null);
        return new IllegalStateException(description);
    }
}
