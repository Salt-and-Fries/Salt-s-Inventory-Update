package com.salts_inventory_update.protocol;

/** Connection-scoped negotiation state. Instances must never be retained by player UUID. */
public final class DesktopConnectionState {
    public enum Phase {
        UNNEGOTIATED,
        ACTIVE,
        UI_DISABLED,
        INCOMPATIBLE
    }

    private Phase phase = Phase.UNNEGOTIATED;
    private long clientNonce;
    private long connectionNonce;
    private long capabilities;
    private long lastModeSequence = -1L;
    private long deadlineTick = Long.MAX_VALUE;

    public synchronized void begin(long clientNonce, long currentTick) {
        reset();
        if (clientNonce == 0L) {
            throw new IllegalArgumentException("client nonce must be nonzero");
        }
        long deadlineTick = Math.addExact(currentTick, DesktopProtocol.HELLO_TIMEOUT_TICKS);
        this.clientNonce = clientNonce;
        this.deadlineTick = deadlineTick;
    }

    public synchronized boolean acknowledge(
        int protocol,
        long echoedClientNonce,
        long connectionNonce,
        long selectedCapabilities,
        boolean uiEnabled
    ) {
        return acknowledge(
            protocol,
            echoedClientNonce,
            connectionNonce,
            selectedCapabilities,
            uiEnabled,
            DesktopProtocol.KNOWN_CAPABILITIES
        );
    }

    /** Accepts a version-local capability mask while retaining the shared protocol default. */
    public synchronized boolean acknowledge(
        int protocol,
        long echoedClientNonce,
        long connectionNonce,
        long selectedCapabilities,
        boolean uiEnabled,
        long supportedCapabilities
    ) {
        if (phase != Phase.UNNEGOTIATED ||
            clientNonce == 0L ||
            protocol != DesktopProtocol.VERSION ||
            echoedClientNonce != clientNonce ||
            connectionNonce == 0L ||
            (selectedCapabilities & ~supportedCapabilities) != 0L) {
            markIncompatible();
            return false;
        }
        this.connectionNonce = connectionNonce;
        this.capabilities = selectedCapabilities;
        this.phase = uiEnabled ? Phase.ACTIVE : Phase.UI_DISABLED;
        this.deadlineTick = Long.MAX_VALUE;
        return true;
    }

    public synchronized boolean updateMode(long connectionNonce, long sequence, boolean uiEnabled) {
        if (!isNegotiated() || this.connectionNonce != connectionNonce || sequence <= lastModeSequence) {
            return false;
        }
        lastModeSequence = sequence;
        phase = uiEnabled ? Phase.ACTIVE : Phase.UI_DISABLED;
        return true;
    }

    public synchronized boolean expireIfNecessary(long currentTick, boolean channelPresent) {
        if (phase != Phase.UNNEGOTIATED || currentTick < deadlineTick) {
            return false;
        }
        if (channelPresent) {
            markIncompatible();
        } else {
            // An absent peer is a valid vanilla connection. Retire the probe
            // deadline without manufacturing negotiated state or repeatedly
            // reporting the same timeout on every subsequent tick.
            clientNonce = 0L;
            deadlineTick = Long.MAX_VALUE;
        }
        return true;
    }

    public synchronized boolean authorizes(long connectionNonce, long requiredCapabilities, boolean requireUi) {
        if (!isNegotiated() || this.connectionNonce != connectionNonce) {
            return false;
        }
        if (requireUi && phase != Phase.ACTIVE) {
            return false;
        }
        return (capabilities & requiredCapabilities) == requiredCapabilities;
    }

    public synchronized void markIncompatible() {
        phase = Phase.INCOMPATIBLE;
        clientNonce = 0L;
        connectionNonce = 0L;
        capabilities = 0L;
        lastModeSequence = -1L;
        deadlineTick = Long.MAX_VALUE;
    }

    public synchronized void reset() {
        phase = Phase.UNNEGOTIATED;
        clientNonce = 0L;
        connectionNonce = 0L;
        capabilities = 0L;
        lastModeSequence = -1L;
        deadlineTick = Long.MAX_VALUE;
    }

    public synchronized Phase phase() {
        return phase;
    }

    public synchronized long connectionNonce() {
        return connectionNonce;
    }

    public synchronized long capabilities() {
        return capabilities;
    }

    public synchronized boolean isNegotiated() {
        return phase == Phase.ACTIVE || phase == Phase.UI_DISABLED;
    }

    public synchronized boolean isUiEnabled() {
        return phase == Phase.ACTIVE;
    }
}
