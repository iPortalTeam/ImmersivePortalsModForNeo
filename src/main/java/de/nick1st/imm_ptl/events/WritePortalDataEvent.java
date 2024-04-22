package de.nick1st.imm_ptl.events;

import net.minecraft.nbt.CompoundTag;
import qouteall.imm_ptl.core.portal.Portal;

public class WritePortalDataEvent extends PortalEvent{
    public final CompoundTag tag;

    public WritePortalDataEvent(Portal portal, CompoundTag tag) {
        super(portal);
        this.tag = tag;
    }
}
