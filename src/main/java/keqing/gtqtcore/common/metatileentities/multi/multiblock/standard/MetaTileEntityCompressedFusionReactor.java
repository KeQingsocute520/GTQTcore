package keqing.gtqtcore.common.metatileentities.multi.multiblock.standard;

import gregtech.api.GTValues;
import gregtech.api.block.IHeatingCoilBlockStats;
import gregtech.api.capability.GregtechDataCodes;
import gregtech.api.capability.IEnergyContainer;
import gregtech.api.capability.impl.EnergyContainerHandler;
import gregtech.api.capability.impl.EnergyContainerList;
import gregtech.api.capability.impl.FluidTankList;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.gui.GuiTextures;
import gregtech.api.gui.ModularUI;
import gregtech.api.gui.resources.TextureArea;
import gregtech.api.gui.widgets.ImageCycleButtonWidget;
import gregtech.api.gui.widgets.ImageWidget;
import gregtech.api.gui.widgets.IndicatorImageWidget;
import gregtech.api.gui.widgets.ProgressWidget;
import gregtech.api.metatileentity.IFastRenderMetaTileEntity;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.MultiblockDisplayText;
import gregtech.api.metatileentity.multiblock.MultiblockWithDisplayBase;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.MultiblockShapeInfo;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.recipes.Recipe;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.recipes.recipeproperties.FusionEUToStartProperty;
import gregtech.api.recipes.recipeproperties.IRecipePropertyStorage;
import gregtech.api.unification.material.Materials;
import gregtech.api.util.RelativeDirection;
import gregtech.api.util.TextComponentUtil;
import gregtech.api.util.TextFormattingUtil;
import gregtech.api.util.interpolate.Eases;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.IRenderSetup;
import gregtech.client.renderer.texture.Textures;
import gregtech.client.shader.postprocessing.BloomEffect;
import gregtech.client.shader.postprocessing.BloomType;
import gregtech.client.utils.*;
import gregtech.common.ConfigHolder;
import gregtech.common.blocks.BlockFusionCasing;
import gregtech.common.blocks.BlockGlassCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.common.metatileentities.MetaTileEntities;

import keqing.gtqtcore.api.blocks.impl.WrappedIntTired;
import keqing.gtqtcore.api.predicate.TiredTraceabilityPredicate;
import keqing.gtqtcore.api.utils.GTQTUtil;
import keqing.gtqtcore.client.textures.GTQTTextures;
import keqing.gtqtcore.common.block.GTQTMetaBlocks;
import keqing.gtqtcore.common.block.blocks.GTQTMultiblockCasing;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.AtomicDouble;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.DoubleSupplier;

public class MetaTileEntityCompressedFusionReactor extends RecipeMapMultiblockController
        implements IFastRenderMetaTileEntity, IBloomEffect {

    protected int heatingCoilLevel;
    protected int heatingCoilDiscount;
    int beamTire;
    private Integer color;
    protected int glassTire;

    public int getMaxParallel(int heatingCoilLevel) {
        if (tier == GTValues.UHV)
            return   heatingCoilLevel;
        if (tier == GTValues.UEV)
            return  4 * heatingCoilLevel;
        return  16 * heatingCoilLevel;
    }
    protected static final int NO_COLOR = 0;

    private final int tier;
    private EnergyContainerList inputEnergyContainers;
    private long heat = 0; // defined in TileEntityFusionReactor but serialized in FusionRecipeLogic
    private int fusionRingColor = NO_COLOR;
    private final FusionProgressSupplier progressBarSupplier;

    @SideOnly(Side.CLIENT)
    private boolean registeredBloomRenderTicket;

    public MetaTileEntityCompressedFusionReactor(ResourceLocation metaTileEntityId, int tier) {
        super(metaTileEntityId, RecipeMaps.FUSION_RECIPES);
        this.recipeMapWorkable = new FusionRecipeLogic(this);
        this.tier = tier;
        this.energyContainer = new EnergyContainerHandler(this, 0, 0, 0, 0, 0) {
            
            @Override
            public String getName() {
                return GregtechDataCodes.FUSION_REACTOR_ENERGY_CONTAINER_TRAIT;
            }
        };
        this.progressBarSupplier = new FusionProgressSupplier();
    }

    @Override
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
        return new MetaTileEntityCompressedFusionReactor(metaTileEntityId, tier);
    }

    @Override
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle(
                        "                                               ",
                        "                                               ",
                        "                    FCICICF                    ",
                        "                    PCIBICP                    ",
                        "                    FCICICF                    ",
                        "                                               ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "                    FCIBICF                    ",
                        "                   CC     CC                   ",
                        "                   PC     CP                   ",
                        "                   CC     CC                   ",
                        "                    FCIBICF                    ",
                        "                                               ")
                .aisle(
                        "                    FCICICF                    ",
                        "                   CC     CC                   ",
                        "                CCCCC     CCCCC                ",
                        "                PPPHHHHHHHHHPPP                ",
                        "                CCCCC     CCCCC                ",
                        "                   CC     CC                   ",
                        "                    FCICICF                    ")
                .aisle(
                        "                    FCIBICF                    ",
                        "                CCCCC     CCCCC                ",
                        "              CCCCCHHHHHHHHHCCCCC              ",
                        "              PPHHHHHHHHHHHHHHHPP              ",
                        "              CCCCCHHHHHHHHHCCCCC              ",
                        "                CCCCC     CCCCC                ",
                        "                    FCIBICF                    ")
                .aisle(
                        "                    FCICICF                    ",
                        "              CCCCCCC     CCCCCCC              ",
                        "            CCCCHHHCC     CCHHHCCCC            ",
                        "            PCHHHHHHHHHHHHHHHHHHHCP            ",
                        "            CCCCHHHCC     CCHHHCCCC            ",
                        "              CCCCCCC     CCCCCCC              ",
                        "                    FCICICF                    ")
                .aisle(
                        "                                               ",
                        "            CCCCCCC FCIBICF CCCCCCC            ",
                        "           CCCHHCCCCC     CCCCCHHCCC           ",
                        "           PHHHHHHHPC     CPHHHHHHHP           ",
                        "           CCCHHCCCCC     CCCCCHHCCC           ",
                        "            CCCCCCC FCIBICF CCCCCCC            ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "           CCCCC               CCCCC           ",
                        "          ECHHCCCCC FCICICF CCCCCHHCE          ",
                        "          PHHHHHPPP FCIBICF PPPHHHHHP          ",
                        "          ECHHCCCCC FCICICF CCCCCHHCE          ",
                        "           CCCCC               CCCCC           ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "          CCCC                   CCCC          ",
                        "         CCHCCCC               CCCCHCC         ",
                        "         PHHHHPP               PPHHHHP         ",
                        "         CCHCCCC               CCCCHCC         ",
                        "          CCCC                   CCCC          ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "         CCC                       CCC         ",
                        "        CCHCCC                   CCCHCC        ",
                        "        PHHHPP                   PPHHHP        ",
                        "        CCHCCC                   CCCHCC        ",
                        "         CCC                       CCC         ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "        CCC                         CCC        ",
                        "       CCHCE                       ECHCC       ",
                        "       PHHHP                       PHHHP       ",
                        "       CCHCE                       ECHCC       ",
                        "        CCC                         CCC        ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "       CCC                           CCC       ",
                        "      ECHCC                         CCHCE      ",
                        "      PHHHP                         PHHHP      ",
                        "      ECHCC                         CCHCE      ",
                        "       CCC                           CCC       ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "      CCC                             CCC      ",
                        "     CCHCE                           ECHCC     ",
                        "     PHHHP                           PHHHP     ",
                        "     CCHCE                           ECHCC     ",
                        "      CCC                             CCC      ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "     CCC                               CCC     ",
                        "    CCHCC                             CCHCC    ",
                        "    PHHHP                             PHHHP    ",
                        "    CCHCC                             CCHCC    ",
                        "     CCC                               CCC     ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "     CCC                               CCC     ",
                        "    CCHCC                             CCHCC    ",
                        "    PHHHP                             PHHHP    ",
                        "    CCHCC                             CCHCC    ",
                        "     CCC                               CCC     ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "    CCC                                 CCC    ",
                        "   CCHCC                               CCHCC   ",
                        "   PHHHP                               PHHHP   ",
                        "   CCHCC                               CCHCC   ",
                        "    CCC                                 CCC    ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "    CCC                                 CCC    ",
                        "   CCHCC                               CCHCC   ",
                        "   PHHHP                               PHHHP   ",
                        "   CCHCC                               CCHCC   ",
                        "    CCC                                 CCC    ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "   CCC                                   CCC   ",
                        "  CCHCC                                 CCHCC  ",
                        "  PHHHP                                 PHHHP  ",
                        "  CCHCC                                 CCHCC  ",
                        "   CCC                                   CCC   ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "   CCC                                   CCC   ",
                        "  CCHCC                                 CCHCC  ",
                        "  PHHHP                                 PHHHP  ",
                        "  CCHCC                                 CCHCC  ",
                        "   CCC                                   CCC   ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "   CCC                                   CCC   ",
                        "  CCHCC                                 CCHCC  ",
                        "  PHHHP                                 PHHHP  ",
                        "  CCHCC                                 CCHCC  ",
                        "   CCC                                   CCC   ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "  CCC                                     CCC  ",
                        " CCHCC                                   CCHCC ",
                        " PHHHP                                   PHHHP ",
                        " CCHCC                                   CCHCC ",
                        "  CCC                                     CCC  ",
                        "                                               ")
                .aisle(
                        "  FFF                                     FFF  ",
                        " FCCCF                                   FCCCF ",
                        "FCCHCCF                                 FCCHCCF",
                        "FCHHHCF                                 FCHHHCF",
                        "FCCHCCF                                 FCCHCCF",
                        " FCCCF                                   FCCCF ",
                        "  FFF                                     FFF  ")
                .aisle(
                        "  CCC                                     CCC  ",
                        " C   C                                   C   C ",
                        "C  H  C                                 C  H  C",
                        "C HHH C                                 C HHH C",
                        "C  H  C                                 C  H  C",
                        " C   C                                   C   C ",
                        "  CCC                                     CCC  ")
                .aisle(
                        "  III                                     III  ",
                        " I   I                                   I   I ",
                        "I  H  I                                 I  H  I",
                        "I HHH I                                 I HHH I",
                        "I  H  I                                 I  H  I",
                        " I   I                                   I   I ",
                        "  III                                     III  ")
                .aisle(
                        "  CBC                                     CBC  ",
                        " B   B                                   B   B ",
                        "C  H  C                                 C  H  C",
                        "B HHH B                                 B HHH B",
                        "C  H  C                                 C  H  C",
                        " B   B                                   B   B ",
                        "  CBC                                     CBC  ")
                .aisle(
                        "  III                                     III  ",
                        " I   I                                   I   I ",
                        "I  H  I                                 I  H  I",
                        "I HHH I                                 I HHH I",
                        "I  H  I                                 I  H  I",
                        " I   I                                   I   I ",
                        "  III                                     III  ")
                .aisle(
                        "  CCC                                     CCC  ",
                        " C   C                                   C   C ",
                        "C  H  C                                 C  H  C",
                        "C HHH C                                 C HHH C",
                        "C  H  C                                 C  H  C",
                        " C   C                                   C   C ",
                        "  CCC                                     CCC  ")
                .aisle(
                        "  FFF                                     FFF  ",
                        " FCCCF                                   FCCCF ",
                        "FCCHCCF                                 FCCHCCF",
                        "FCHHHCF                                 FCHHHCF",
                        "FCCHCCF                                 FCCHCCF",
                        " FCCCF                                   FCCCF ",
                        "  FFF                                     FFF  ")
                .aisle(
                        "                                               ",
                        "  CCC                                     CCC  ",
                        " CCHCC                                   CCHCC ",
                        " PHHHP                                   PHHHP ",
                        " CCHCC                                   CCHCC ",
                        "  CCC                                     CCC  ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "   CCC                                   CCC   ",
                        "  CCHCC                                 CCHCC  ",
                        "  PHHHP                                 PHHHP  ",
                        "  CCHCC                                 CCHCC  ",
                        "   CCC                                   CCC   ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "   CCC                                   CCC   ",
                        "  CCHCC                                 CCHCC  ",
                        "  PHHHP                                 PHHHP  ",
                        "  CCHCC                                 CCHCC  ",
                        "   CCC                                   CCC   ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "   CCC                                   CCC   ",
                        "  CCHCC                                 CCHCC  ",
                        "  PHHHP                                 PHHHP  ",
                        "  CCHCC                                 CCHCC  ",
                        "   CCC                                   CCC   ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "    CCC                                 CCC    ",
                        "   CCHCC                               CCHCC   ",
                        "   PHHHP                               PHHHP   ",
                        "   CCHCC                               CCHCC   ",
                        "    CCC                                 CCC    ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "    CCC                                 CCC    ",
                        "   CCHCC                               CCHCC   ",
                        "   PHHHP                               PHHHP   ",
                        "   CCHCC                               CCHCC   ",
                        "    CCC                                 CCC    ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "     CCC                               CCC     ",
                        "    CCHCC                             CCHCC    ",
                        "    PHHHP                             PHHHP    ",
                        "    CCHCC                             CCHCC    ",
                        "     CCC                               CCC     ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "     CCC                               CCC     ",
                        "    CCHCC                             CCHCC    ",
                        "    PHHHP                             PHHHP    ",
                        "    CCHCC                             CCHCC    ",
                        "     CCC                               CCC     ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "      CCC                             CCC      ",
                        "     CCHCE                           ECHCC     ",
                        "     PHHHP                           PHHHP     ",
                        "     CCHCE                           ECHCC     ",
                        "      CCC                             CCC      ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "       CCC                           CCC       ",
                        "      ECHCC                         CCHCE      ",
                        "      PHHHP                         PHHHP      ",
                        "      ECHCC                         CCHCE      ",
                        "       CCC                           CCC       ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "        CCC                         CCC        ",
                        "       CCHCE                       ECHCC       ",
                        "       PHHHP                       PHHHP       ",
                        "       CCHCE                       ECHCC       ",
                        "        CCC                         CCC        ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "         CCC                       CCC         ",
                        "        CCHCCC                   CCCHCC        ",
                        "        PHHHPP                   PPHHHP        ",
                        "        CCHCCC                   CCCHCC        ",
                        "         CCC                       CCC         ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "          CCCC                   CCCC          ",
                        "         CCHCCCC               CCCCHCC         ",
                        "         PHHHHPP               PPHHHHP         ",
                        "         CCHCCCC               CCCCHCC         ",
                        "          CCCC                   CCCC          ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "           CCCCC               CCCCC           ",
                        "          ECHHCCCCC FCICICF CCCCCHHCE          ",
                        "          PHHHHHPPP FCIBICF PPPHHHHHP          ",
                        "          ECHHCCCCC FCICICF CCCCCHHCE          ",
                        "           CCCCC               CCCCC           ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "            CCCCCCC FCIBICF CCCCCCC            ",
                        "           CCCHHCCCCC     CCCCCHHCCC           ",
                        "           PHHHHHHHPC     CPHHHHHHHP           ",
                        "           CCCHHCCCCC     CCCCCHHCCC           ",
                        "            CCCCCCC FCIBICF CCCCCCC            ",
                        "                                               ")
                .aisle(
                        "                    FCICICF                    ",
                        "              CCCCCCC     CCCCCCC              ",
                        "            CCCCHHHCC     CCHHHCCCC            ",
                        "            PPHHHHHHHHHHHHHHHHHHHPP            ",
                        "            CCCCHHHCC     CCHHHCCCC            ",
                        "              CCCCCCC     CCCCCCC              ",
                        "                    FCICICF                    ")
                .aisle(
                        "                    FCIBICF                    ",
                        "                CCCCC     CCCCC                ",
                        "              CCCCCHHHHHHHHHCCCCC              ",
                        "              PPHHHHHHHHHHHHHHHPP              ",
                        "              CCCCCHHHHHHHHHCCCCC              ",
                        "                CCCCC     CCCCC                ",
                        "                    FCIBICF                    ")
                .aisle(
                        "                    FCICICF                    ",
                        "                   CC     CC                   ",
                        "                CCCCC     CCCCC                ",
                        "                PPPHHHHHHHHHPPP                ",
                        "                CCCCC     CCCCC                ",
                        "                   CC     CC                   ",
                        "                    FCICICF                    ")
                .aisle(
                        "                                               ",
                        "                    FCIBICF                    ",
                        "                   CC     CC                   ",
                        "                   PC     CP                   ",
                        "                   CC     CC                   ",
                        "                    FCIBICF                    ",
                        "                                               ")
                .aisle(
                        "                                               ",
                        "                                               ",
                        "                    FCICICF                    ",
                        "                    FCI~ICF                    ",
                        "                    FCICICF                    ",
                        "                                               ",
                        "                                               ")
                .where('~', selfPredicate())
                .where('C', states(getCasingState()))
                .where('E', states(getCasingState())
                        .or(abilities(MultiblockAbility.MAINTENANCE_HATCH).setExactLimit(1))
                        .or(abilities(MultiblockAbility.IMPORT_ITEMS).setMaxGlobalLimited(4).setPreviewCount(1))
                        .or(abilities(MultiblockAbility.EXPORT_ITEMS).setMaxGlobalLimited(4).setPreviewCount(1))
                        .or(abilities(MultiblockAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8).setPreviewCount(1))
                        .or(abilities(MultiblockAbility.EXPORT_FLUIDS).setMaxGlobalLimited(8).setPreviewCount(1))
                        .or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(3))
                )
                .where('F', states(MetaBlocks.FRAMES.get(Materials.NaquadahAlloy).getBlock(Materials.NaquadahAlloy)))
                .where('H', states(getCasingState1()))
                .where('P', TiredTraceabilityPredicate.CP_BEAM)
                .where('I', heatingCoils())
                .where('B', TiredTraceabilityPredicate.CP_GLASS)
                .where(' ', any())
                .build();
    }

    private static IBlockState getCasingState1() {
        return MetaBlocks.FUSION_CASING.getState(BlockFusionCasing.CasingType.FUSION_COIL);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        if (tier == GTValues.UHV)
            return  GTQTTextures.COMPRESSED_FUSION_REACTOR_MKI_CASING;
        if (tier == GTValues.UEV)
            return  GTQTTextures.COMPRESSED_FUSION_REACTOR_MKII_CASING;
        return  GTQTTextures.COMPRESSED_FUSION_REACTOR_MKIII_CASING;
    }

    private IBlockState getCasingState() {
        if (tier == GTValues.UHV)
            return  GTQTMetaBlocks.MULTI_CASING.getState(GTQTMultiblockCasing.CasingType.COMPRESSED_FUSION_REACTOR_MKI_CASING);
        if (tier == GTValues.UEV)
            return  GTQTMetaBlocks.MULTI_CASING.getState(GTQTMultiblockCasing.CasingType.COMPRESSED_FUSION_REACTOR_MKII_CASING);
        return  GTQTMetaBlocks.MULTI_CASING.getState(GTQTMultiblockCasing.CasingType.COMPRESSED_FUSION_REACTOR_MKIII_CASING);
    }


    @Override
    protected void formStructure(PatternMatchContext context) {
        long energyStored = this.energyContainer.getEnergyStored();
        super.formStructure(context);
        this.initializeAbilities();
        ((EnergyContainerHandler) this.energyContainer).setEnergyStored(energyStored);
        Object coilType = context.get("CoilType");
        Object beamTire = context.get("BeamTiredStats");
        Object glassTire = context.get("GlassTiredStats");
        this.glassTire = GTQTUtil.getOrDefault(() -> glassTire instanceof WrappedIntTired,
                () -> ((WrappedIntTired)glassTire).getIntTier(),
                0);
        this.beamTire = GTQTUtil.getOrDefault(() -> beamTire instanceof WrappedIntTired,
                () -> ((WrappedIntTired)beamTire).getIntTier(),
                0);
        if (coilType instanceof IHeatingCoilBlockStats) {
            this.heatingCoilLevel = ((IHeatingCoilBlockStats) coilType).getLevel();
            this.heatingCoilDiscount = ((IHeatingCoilBlockStats) coilType).getEnergyDiscount();
        } else {
            this.heatingCoilLevel = BlockWireCoil.CoilType.CUPRONICKEL.getLevel();
            this.heatingCoilDiscount = BlockWireCoil.CoilType.CUPRONICKEL.getEnergyDiscount();
        }
    }

    protected int getFusionRingColor() {
        return this.fusionRingColor;
    }

    protected boolean hasFusionRingColor() {
        return this.fusionRingColor != NO_COLOR;
    }

    protected void setFusionRingColor(int fusionRingColor) {
        if (this.fusionRingColor != fusionRingColor) {
            this.fusionRingColor = fusionRingColor;
            writeCustomData(GregtechDataCodes.UPDATE_COLOR, buf -> buf.writeVarInt(fusionRingColor));
        }
    }


    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.energyContainer = new EnergyContainerHandler(this, 0, 0, 0, 0, 0) {
            
            @Override
            public String getName() {
                return GregtechDataCodes.FUSION_REACTOR_ENERGY_CONTAINER_TRAIT;
            }
        };
        this.inputEnergyContainers = new EnergyContainerList(Lists.newArrayList());
        this.heat = 0;
        this.setFusionRingColor(NO_COLOR);
    }

    @Override
    protected void initializeAbilities() {
        this.inputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.IMPORT_ITEMS));
        this.inputFluidInventory = new FluidTankList(true, getAbilities(MultiblockAbility.IMPORT_FLUIDS));
        this.outputInventory = new ItemHandlerList(getAbilities(MultiblockAbility.EXPORT_ITEMS));
        this.outputFluidInventory = new FluidTankList(true, getAbilities(MultiblockAbility.EXPORT_FLUIDS));
        List<IEnergyContainer> energyInputs = getAbilities(MultiblockAbility.INPUT_ENERGY);
        this.inputEnergyContainers = new EnergyContainerList(energyInputs);
        long euCapacity = calculateEnergyStorageFactor(energyInputs.size());
        this.energyContainer = new EnergyContainerHandler(this, euCapacity, GTValues.V[tier], 0, 0, 0) {
            
            @Override
            public String getName() {
                return GregtechDataCodes.FUSION_REACTOR_ENERGY_CONTAINER_TRAIT;
            }
        };
    }

    private long calculateEnergyStorageFactor(int energyInputAmount) {
        return energyInputAmount * (long) Math.pow(2, tier - 6) * 10000000L;
    }

    @Override
    protected void updateFormedValid() {
        if (this.inputEnergyContainers.getEnergyStored() > 0) {
            long energyAdded = this.energyContainer.addEnergy(this.inputEnergyContainers.getEnergyStored());
            if (energyAdded > 0) this.inputEnergyContainers.removeEnergy(energyAdded);
        }
        super.updateFormedValid();
        if (recipeMapWorkable.isWorking() && fusionRingColor == NO_COLOR) {
            if (recipeMapWorkable.getPreviousRecipe() != null &&
                    !recipeMapWorkable.getPreviousRecipe().getFluidOutputs().isEmpty()) {
                setFusionRingColor(0xFF000000 |
                        recipeMapWorkable.getPreviousRecipe().getFluidOutputs().get(0).getFluid().getColor());
            }
        } else if (!recipeMapWorkable.isWorking() && isStructureFormed()) {
            setFusionRingColor(NO_COLOR);
        }
    }

    @Override
    public void writeInitialSyncData(PacketBuffer buf) {
        super.writeInitialSyncData(buf);
        buf.writeVarInt(this.fusionRingColor);
    }

    @Override
    public void receiveInitialSyncData(PacketBuffer buf) {
        super.receiveInitialSyncData(buf);
        this.fusionRingColor = buf.readVarInt();
    }

    @Override
    public void receiveCustomData(int dataId, PacketBuffer buf) {
        if (dataId == GregtechDataCodes.UPDATE_COLOR) {
            this.fusionRingColor = buf.readVarInt();
        } else {
            super.receiveCustomData(dataId, buf);
        }
    }


    @SideOnly(Side.CLIENT)
    
    @Override
    protected ICubeRenderer getFrontOverlay() {
        return Textures.FUSION_REACTOR_OVERLAY;
    }

    @Override
    public boolean hasMaintenanceMechanics() {
        return false;
    }

    public long getHeat() {
        return heat;
    }

    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        super.addDisplayText(textList);
        textList.add(new TextComponentTranslation("gtqtcore.multiblock.md.level", heatingCoilLevel));
        textList.add(new TextComponentTranslation("gtqtcore.multiblock.fu.level", 100-10*beamTire));
        textList.add(new TextComponentTranslation("gtqtcore.multiblock.md.glass", glassTire));
        textList.add(new TextComponentTranslation("gregtech.multiblock.cracking_unit.energy", 100 - 2.5 * this.glassTire));
        if (isStructureFormed()) {
            textList.add(new TextComponentTranslation("gregtech.multiblock.fusion_reactor.energy", this.energyContainer.getEnergyStored(), this.energyContainer.getEnergyCapacity()));
            textList.add(new TextComponentTranslation("gregtech.multiblock.fusion_reactor.heat", heat));
        }
    }

    @Override
    public void addInformation(ItemStack stack,  World player,  List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gregtech.machine.fusion_reactor.capacity", calculateEnergyStorageFactor(16) / 1000000L));
        tooltip.add(I18n.format("gregtech.machine.fusion_reactor.overclocking"));
        tooltip.add(I18n.format("gtqtcore.multiblock.fu.tooltip.1"));
        tooltip.add(I18n.format("gtqtcore.multiblock.hb.tooltip.4"));
        tooltip.add(I18n.format("gtqtcore.multiblock.hb.tooltip.3"));

        if (tier == GTValues.UHV){
            tooltip.add(I18n.format("gtqtcore.multiblock.ab.tooltip.2", 24));
            tooltip.add(TooltipHelper.RAINBOW_SLOW + I18n.format("惊 鸿 万 物", new Object[0]));}
        if (tier == GTValues.UEV){
            tooltip.add(I18n.format("gtqtcore.multiblock.ab.tooltip.2", 96));
            tooltip.add(TooltipHelper.RAINBOW_SLOW + I18n.format("破 碎 亘 古", new Object[0]));}
        if (tier == GTValues.UIV){
            tooltip.add(I18n.format("gtqtcore.multiblock.ab.tooltip.2", 384));
            tooltip.add(TooltipHelper.RAINBOW_SLOW + I18n.format("凌 驾 虚 无", new Object[0]));}
    }

    @Override
    protected ModularUI.Builder createUITemplate(EntityPlayer entityPlayer) {
        // Background
        ModularUI.Builder builder = ModularUI.builder(GuiTextures.BACKGROUND, 198, 236);

        // Display
        builder.image(4, 4, 190, 138, GuiTextures.DISPLAY);

        // Energy Bar
        builder.widget(new ProgressWidget(
                () -> energyContainer.getEnergyCapacity() > 0 ?
                        1.0 * energyContainer.getEnergyStored() / energyContainer.getEnergyCapacity() : 0,
                4, 144, 94, 7,
                GuiTextures.PROGRESS_BAR_FUSION_ENERGY, ProgressWidget.MoveType.HORIZONTAL)
                .setHoverTextConsumer(this::addEnergyBarHoverText));

        // Heat Bar
        builder.widget(new ProgressWidget(
                () -> energyContainer.getEnergyCapacity() > 0 ? 1.0 * heat / energyContainer.getEnergyCapacity() : 0,
                100, 144, 94, 7,
                GuiTextures.PROGRESS_BAR_FUSION_HEAT, ProgressWidget.MoveType.HORIZONTAL)
                .setHoverTextConsumer(this::addHeatBarHoverText));

        // Indicator Widget
        builder.widget(new IndicatorImageWidget(174, 122, 17, 17, getLogo())
                .setWarningStatus(getWarningLogo(), this::addWarningText)
                .setErrorStatus(getErrorLogo(), this::addErrorText));

        // Title
        if (tier == GTValues.UHV) {
            // MK1
            builder.widget(new ImageWidget(66, 9, 67, 12, GuiTextures.FUSION_REACTOR_MK1_TITLE).setIgnoreColor(true));
        } else if (tier == GTValues.UEV) {
            // MK2
            builder.widget(new ImageWidget(65, 9, 69, 12, GuiTextures.FUSION_REACTOR_MK2_TITLE).setIgnoreColor(true));
        } else {
            // MK3
            builder.widget(new ImageWidget(64, 9, 71, 12, GuiTextures.FUSION_REACTOR_MK3_TITLE).setIgnoreColor(true));
        }

        // Fusion Diagram + Progress Bar
        builder.widget(new ImageWidget(55, 24, 89, 101, GuiTextures.FUSION_REACTOR_DIAGRAM).setIgnoreColor(true));
        builder.widget(FusionProgressSupplier.Type.BOTTOM_LEFT.getWidget(this));
        builder.widget(FusionProgressSupplier.Type.TOP_LEFT.getWidget(this));
        builder.widget(FusionProgressSupplier.Type.TOP_RIGHT.getWidget(this));
        builder.widget(FusionProgressSupplier.Type.BOTTOM_RIGHT.getWidget(this));

        // Fusion Legend
        builder.widget(new ImageWidget(7, 98, 108, 41, GuiTextures.FUSION_REACTOR_LEGEND).setIgnoreColor(true));

        // Power Button + Detail
        builder.widget(new ImageCycleButtonWidget(173, 211, 18, 18, GuiTextures.BUTTON_POWER,
                recipeMapWorkable::isWorkingEnabled, recipeMapWorkable::setWorkingEnabled));
        builder.widget(new ImageWidget(173, 229, 18, 6, GuiTextures.BUTTON_POWER_DETAIL));

        // Voiding Mode Button
        builder.widget(new ImageCycleButtonWidget(173, 189, 18, 18, GuiTextures.BUTTON_VOID_MULTIBLOCK,
                4, this::getVoidingMode, this::setVoidingMode)
                .setTooltipHoverString(MultiblockWithDisplayBase::getVoidingModeTooltip));

        // Distinct Buses Unavailable Image
        builder.widget(new ImageWidget(173, 171, 18, 18, GuiTextures.BUTTON_NO_DISTINCT_BUSES)
                .setTooltip("gregtech.multiblock.universal.distinct_not_supported"));

        // Flex Unavailable Image
        builder.widget(getFlexButton(173, 153, 18, 18));

        // Player Inventory
        builder.bindPlayerInventory(entityPlayer.inventory, 153);
        return builder;
    }

    private void addEnergyBarHoverText(List<ITextComponent> hoverList) {
        ITextComponent energyInfo = TextComponentUtil.stringWithColor(
                TextFormatting.AQUA,
                TextFormattingUtil.formatNumbers(energyContainer.getEnergyStored()) + " / " +
                        TextFormattingUtil.formatNumbers(energyContainer.getEnergyCapacity()) + " EU");
        hoverList.add(TextComponentUtil.translationWithColor(
                TextFormatting.GRAY,
                "gregtech.multiblock.energy_stored",
                energyInfo));
    }

    private void addHeatBarHoverText(List<ITextComponent> hoverList) {
        ITextComponent heatInfo = TextComponentUtil.stringWithColor(
                TextFormatting.RED,
                TextFormattingUtil.formatNumbers(heat) + " / " +
                        TextFormattingUtil.formatNumbers(energyContainer.getEnergyCapacity()));
        hoverList.add(TextComponentUtil.translationWithColor(
                TextFormatting.GRAY,
                "gregtech.multiblock.fusion_reactor.heat",
                heatInfo));
    }

    private static class FusionProgressSupplier {

        private final AtomicDouble tracker = new AtomicDouble(0.0);
        private final ProgressWidget.TimedProgressSupplier bottomLeft;
        private final DoubleSupplier topLeft;
        private final DoubleSupplier topRight;
        private final DoubleSupplier bottomRight;

        public FusionProgressSupplier() {
            // Bottom Left, fill on [0, 0.25)
            bottomLeft = new ProgressWidget.TimedProgressSupplier(200, 164, false) {

                @Override
                public double getAsDouble() {
                    double val = super.getAsDouble();
                    tracker.set(val);
                    if (val >= 0.25) {
                        return 1;
                    }
                    return 4 * val;
                }

                @Override
                public void resetCountdown() {
                    super.resetCountdown();
                    tracker.set(0);
                }
            };

            // Top Left, fill on [0.25, 0.5)
            topLeft = () -> {
                double val = tracker.get();
                if (val < 0.25) {
                    return 0;
                } else if (val >= 0.5) {
                    return 1;
                }
                return 4 * (val - 0.25);
            };

            // Top Right, fill on [0.5, 0.75)
            topRight = () -> {
                double val = tracker.get();
                if (val < 0.5) {
                    return 0;
                } else if (val >= 0.75) {
                    return 1;
                }
                return 4 * (val - 0.5);
            };

            // Bottom Right, fill on [0.75, 1.0]
            bottomRight = () -> {
                double val = tracker.get();
                if (val < 0.75) {
                    return 0;
                } else if (val >= 1) {
                    return 1;
                }
                return 4 * (val - 0.75);
            };
        }

        public void resetCountdown() {
            bottomLeft.resetCountdown();
        }

        public DoubleSupplier getSupplier(Type type) {
            return switch (type) {
                case BOTTOM_LEFT -> bottomLeft;
                case TOP_LEFT -> topLeft;
                case TOP_RIGHT -> topRight;
                case BOTTOM_RIGHT -> bottomRight;
            };
        }

        private enum Type {

            BOTTOM_LEFT(
                    61, 66, 35, 41,
                    GuiTextures.PROGRESS_BAR_FUSION_REACTOR_DIAGRAM_BL, ProgressWidget.MoveType.VERTICAL),
            TOP_LEFT(
                    61, 30, 41, 35,
                    GuiTextures.PROGRESS_BAR_FUSION_REACTOR_DIAGRAM_TL, ProgressWidget.MoveType.HORIZONTAL),
            TOP_RIGHT(
                    103, 30, 35, 41,
                    GuiTextures.PROGRESS_BAR_FUSION_REACTOR_DIAGRAM_TR, ProgressWidget.MoveType.VERTICAL_DOWNWARDS),
            BOTTOM_RIGHT(
                    97, 72, 41, 35,
                    GuiTextures.PROGRESS_BAR_FUSION_REACTOR_DIAGRAM_BR, ProgressWidget.MoveType.HORIZONTAL_BACKWARDS);

            private final int x;
            private final int y;
            private final int width;
            private final int height;
            private final TextureArea texture;
            private final ProgressWidget.MoveType moveType;

            Type(int x, int y, int width, int height, TextureArea texture, ProgressWidget.MoveType moveType) {
                this.x = x;
                this.y = y;
                this.width = width;
                this.height = height;
                this.texture = texture;
                this.moveType = moveType;
            }

            public ProgressWidget getWidget(MetaTileEntityCompressedFusionReactor instance) {
                return new ProgressWidget(
                        () -> instance.recipeMapWorkable.isActive() ?
                                instance.progressBarSupplier.getSupplier(this).getAsDouble() : 0,
                        x, y, width, height, texture, moveType)
                        .setIgnoreColor(true)
                        .setHoverTextConsumer(
                                tl -> MultiblockDisplayText.builder(tl, instance.isStructureFormed())
                                        .setWorkingStatus(instance.recipeMapWorkable.isWorkingEnabled(),
                                                instance.recipeMapWorkable.isActive())
                                        .addWorkingStatusLine());
            }
        }
    }

    protected int getGlassTireTier() {
        return this.glassTire;
    }

    private class FusionRecipeLogic extends MultiblockRecipeLogic {

        public FusionRecipeLogic(MetaTileEntityCompressedFusionReactor tileEntity) {
            super(tileEntity);
        }

        protected void modifyOverclockPost(int[] resultOverclock,  IRecipePropertyStorage storage) {
            super.modifyOverclockPost(resultOverclock, storage);
            int coilTier = ((MetaTileEntityCompressedFusionReactor)this.metaTileEntity).getGlassTireTier();
            if (coilTier > 0) {
                resultOverclock[0] = (int)((double)resultOverclock[0] * (1.0 - (double)glassTire * 0.025));
                resultOverclock[0] = Math.max(1, resultOverclock[0]);
            }
        }
        public void setMaxProgress(int maxProgress) {
            this.maxProgressTime = maxProgress*(100-beamTire*10)/100;

        }

        @Override
        public int getParallelLimit() {
            return getMaxParallel(heatingCoilLevel);
        }


        @Override
        protected double getOverclockingDurationDivisor() {
            return 2.0D;
        }

        @Override
        protected double getOverclockingVoltageMultiplier() {
            return 2.0D;
        }

        @Override
        public long getMaxVoltage() {
            return Math.min(GTValues.V[tier], super.getMaxVoltage());
        }

        @Override
        public void updateWorkable() {
            super.updateWorkable();
            // Drain heat when the reactor is not active, is paused via soft mallet, or does not have enough energy and
            // has fully wiped recipe progress
            // Don't drain heat when there is not enough energy and there is still some recipe progress, as that makes
            // it doubly hard to complete the recipe
            // (Will have to recover heat and recipe progress)
            if (heat > 0) {
                if (!isActive || !workingEnabled || (hasNotEnoughEnergy && progressTime == 0)) {
                    heat = heat <= 10000 ? 0 : (heat - 10000);
                }
            }
        }

        @Override
        public boolean checkRecipe( Recipe recipe) {
            if (!super.checkRecipe(recipe))
                return false;

            // if the reactor is not able to hold enough energy for it, do not run the recipe
            if (recipe.getProperty(FusionEUToStartProperty.getInstance(), 0L) > energyContainer.getEnergyCapacity())
                return false;

            long heatDiff = recipe.getProperty(FusionEUToStartProperty.getInstance(), 0L) - heat;
            // if the stored heat is >= required energy, recipe is okay to run
            if (heatDiff <= 0)
                return true;

            // if the remaining energy needed is more than stored, do not run
            if (energyContainer.getEnergyStored() < heatDiff)
                return false;

            // remove the energy needed
            energyContainer.removeEnergy(heatDiff);
            // increase the stored heat
            heat += heatDiff;
            return true;
        }

        
        @Override
        public NBTTagCompound serializeNBT() {
            NBTTagCompound tag = super.serializeNBT();
            tag.setLong("Heat", heat);
            return tag;
        }

        @Override
        public void deserializeNBT( NBTTagCompound compound) {
            super.deserializeNBT(compound);
            heat = compound.getLong("Heat");
        }

        @Override
        protected void setActive(boolean active) {
            if (active != isActive) {
                MetaTileEntityCompressedFusionReactor.this.progressBarSupplier.resetCountdown();
            }
            super.setActive(active);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderMetaTileEntity(double x, double y, double z, float partialTicks) {
        if (this.hasFusionRingColor() && !this.registeredBloomRenderTicket) {
            this.registeredBloomRenderTicket = true;
            BloomEffectUtil.registerBloomRender(FusionBloomSetup.INSTANCE, getBloomType(), this, this);
        }
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void renderBloomEffect( BufferBuilder buffer,  EffectRenderContext context) {
        if (!this.hasFusionRingColor()) return;
        int color = RenderUtil.interpolateColor(this.getFusionRingColor(), -1, Eases.QUAD_IN.getInterpolation(
                Math.abs((Math.abs(getOffsetTimer() % 50) + context.partialTicks()) - 25) / 25));
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;
        EnumFacing relativeBack = RelativeDirection.BACK.getRelativeFacing(getFrontFacing(), getUpwardsFacing(),
                isFlipped());
        EnumFacing.Axis axis = RelativeDirection.UP.getRelativeFacing(getFrontFacing(), getUpwardsFacing(), isFlipped())
                .getAxis();

        RenderBufferHelper.renderRing(buffer,
                getPos().getX() - context.cameraX() + relativeBack.getXOffset() * 23 + 0.5,
                getPos().getY() - context.cameraY() + relativeBack.getYOffset() * 23 + 0.5,
                getPos().getZ() - context.cameraZ() + relativeBack.getZOffset() * 23 + 0.5,
                6, 0.2, 10, 20,
                r, g, b, a, EnumFacing.Axis.X);

        RenderBufferHelper.renderRing(buffer,
                getPos().getX() - context.cameraX() + relativeBack.getXOffset() * 23 + 0.5,
                getPos().getY() - context.cameraY() + relativeBack.getYOffset() * 23 + 0.5,
                getPos().getZ() - context.cameraZ() + relativeBack.getZOffset() * 23 + 0.5,
                6, 0.2, 10, 20,
                r, g, b, a, EnumFacing.Axis.Y);

        RenderBufferHelper.renderRing(buffer,
                getPos().getX() - context.cameraX() + relativeBack.getXOffset() * 23 + 0.5,
                getPos().getY() - context.cameraY() + relativeBack.getYOffset() * 23 + 0.5,
                getPos().getZ() - context.cameraZ() + relativeBack.getZOffset() * 23 + 0.5,
                6, 0.2, 10, 20,
                r, g, b, a, EnumFacing.Axis.Z);

        RenderBufferHelper.renderRing(buffer,
                getPos().getX() - context.cameraX() + relativeBack.getXOffset() * 23 + 0.5,
                getPos().getY() - context.cameraY() + relativeBack.getYOffset() * 23 + 0.5,
                getPos().getZ() - context.cameraZ() + relativeBack.getZOffset() * 23 + 0.5,
                12, 0.2, 10, 20,
                r, g, b, a, EnumFacing.Axis.Y);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean shouldRenderBloomEffect( EffectRenderContext context) {
        return this.hasFusionRingColor();
    }

    @Override
    public AxisAlignedBB getRenderBoundingBox() {
        EnumFacing relativeRight = RelativeDirection.RIGHT.getRelativeFacing(getFrontFacing(), getUpwardsFacing(),
                isFlipped());
        EnumFacing relativeBack = RelativeDirection.BACK.getRelativeFacing(getFrontFacing(), getUpwardsFacing(),
                isFlipped());

        return new AxisAlignedBB(
                this.getPos().offset(relativeBack).offset(relativeRight, 6),
                this.getPos().offset(relativeBack, 13).offset(relativeRight.getOpposite(), 6));
    }

    @Override
    public boolean shouldRenderInPass(int pass) {
        return pass == 0;
    }

    @Override
    public boolean isGlobalRenderer() {
        return true;
    }

    private static BloomType getBloomType() {
        ConfigHolder.FusionBloom fusionBloom = ConfigHolder.client.shader.fusionBloom;
        return BloomType.fromValue(fusionBloom.useShader ? fusionBloom.bloomStyle : -1);
    }

    @SideOnly(Side.CLIENT)
    private static final class FusionBloomSetup implements IRenderSetup {

        private static final FusionBloomSetup INSTANCE = new FusionBloomSetup();

        float lastBrightnessX;
        float lastBrightnessY;

        @Override
        public void preDraw( BufferBuilder buffer) {
            BloomEffect.strength = (float) ConfigHolder.client.shader.fusionBloom.strength;
            BloomEffect.baseBrightness = (float) ConfigHolder.client.shader.fusionBloom.baseBrightness;
            BloomEffect.highBrightnessThreshold = (float) ConfigHolder.client.shader.fusionBloom.highBrightnessThreshold;
            BloomEffect.lowBrightnessThreshold = (float) ConfigHolder.client.shader.fusionBloom.lowBrightnessThreshold;
            BloomEffect.step = 1;

            lastBrightnessX = OpenGlHelper.lastBrightnessX;
            lastBrightnessY = OpenGlHelper.lastBrightnessY;
            GlStateManager.color(1, 1, 1, 1);
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240.0F, 240.0F);
            GlStateManager.disableTexture2D();

            buffer.begin(GL11.GL_QUAD_STRIP, DefaultVertexFormats.POSITION_COLOR);
        }

        @Override
        public void postDraw( BufferBuilder buffer) {
            Tessellator.getInstance().draw();

            GlStateManager.enableTexture2D();
            OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, lastBrightnessX, lastBrightnessY);
        }
    }
}