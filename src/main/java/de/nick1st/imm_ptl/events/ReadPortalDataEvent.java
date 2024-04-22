package de.nick1st.imm_ptl.events;

import net.minecraft.nbt.CompoundTag;
import qouteall.imm_ptl.core.portal.Portal;

public class ReadPortalDataEvent extends PortalEvent{
    public final CompoundTag tag;

    public ReadPortalDataEvent(Portal portal, CompoundTag tag) {
        super(portal);
        this.tag = tag;
    }
}
