package keqing.gtqtcore.api.metaileentity.multiblock;

import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import keqing.gtqtcore.api.capability.IBall;
import keqing.gtqtcore.api.capability.IBuffer;

public class GTQTMultiblockAbility {

    public static final MultiblockAbility<IBuffer> BUFFER_MULTIBLOCK_ABILITY = new MultiblockAbility<>("buffer");
    public static final MultiblockAbility<IBall> GRINDBALL_MULTIBLOCK_ABILITY = new MultiblockAbility<>("ball");

}
