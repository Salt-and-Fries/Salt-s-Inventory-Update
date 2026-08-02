package com.salts_inventory_update.api.server.desktop;

import java.lang.reflect.Method;
import java.util.Objects;

/** Introspection and null-safety helpers for optional transfer validators. */
public final class DesktopTransferValidators {
    private DesktopTransferValidators() {
    }

    public static boolean supports(DesktopServerWindowHandler<?, ?> handler) {
        Objects.requireNonNull(handler, "handler");
        try {
            Method method = handler.getClass().getMethod("validateTransfer", DesktopTransferRequest.class);
            return method.getDeclaringClass() != DesktopServerWindowHandler.class;
        } catch (NoSuchMethodException exception) {
            throw new IllegalStateException("DesktopServerWindowHandler contract is incomplete", exception);
        }
    }

    public static DesktopTransferDecision validate(
        DesktopServerWindowHandler<?, ?> handler,
        DesktopTransferRequest<?, ?> request
    ) {
        Objects.requireNonNull(handler, "handler");
        Objects.requireNonNull(request, "request");
        @SuppressWarnings({"rawtypes", "unchecked"})
        DesktopTransferDecision decision = ((DesktopServerWindowHandler) handler).validateTransfer((DesktopTransferRequest) request);
        return Objects.requireNonNull(decision, "validateTransfer returned null");
    }
}

