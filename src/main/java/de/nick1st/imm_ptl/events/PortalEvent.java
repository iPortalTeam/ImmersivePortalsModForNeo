package de.nick1st.imm_ptl.events;

import net.neoforged.bus.api.Event;
import qouteall.imm_ptl.core.portal.Portal;

public abstract class PortalEvent extends Event {
    public final Portal portal;

    protected PortalEvent(Portal portal) {
        this.portal = portal;
    }
}
