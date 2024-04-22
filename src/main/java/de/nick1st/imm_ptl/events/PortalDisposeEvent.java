package de.nick1st.imm_ptl.events;

import qouteall.imm_ptl.core.portal.Portal;

public class PortalDisposeEvent extends PortalEvent {

    public PortalDisposeEvent(Portal portal) {
        super(portal);
    }
}
