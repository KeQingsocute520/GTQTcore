package keqing.gtqtcore.common.metatileentities.multi.multiblock.standard;

import gregicality.science.common.block.GCYSMetaBlocks;
import gregicality.science.common.block.blocks.BlockGCYSMultiblockCasing;
import gregicality.science.common.block.blocks.BlockTransparentCasing;
import gregtech.api.block.IHeatingCoilBlockStats;
import gregtech.api.capability.IMultipleTankHandler;
import gregtech.api.capability.impl.MultiblockRecipeLogic;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.recipes.RecipeMap;
import gregtech.api.recipes.RecipeMaps;
import gregtech.api.util.TextFormattingUtil;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.client.renderer.texture.cube.OrientedOverlayRenderer;
import gregtech.client.utils.TooltipHelper;
import gregtech.common.blocks.BlockBoilerCasing;
import gregtech.common.blocks.BlockMetalCasing;
import gregtech.common.blocks.BlockWireCoil;
import gregtech.common.blocks.MetaBlocks;
import gregtech.core.sound.GTSoundEvents;
import keqing.gtqtcore.api.metaileentity.GTQTRecipeMapMultiblockController;
import keqing.gtqtcore.api.recipes.GTQTcoreRecipeMaps;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;

import static gregtech.api.unification.material.Materials.Lubricant;

public class MetaTileEntityIntegratedMiningDivision extends GTQTRecipeMapMultiblockController {
    protected int heatingCoilLevel;
    protected int heatingCoilDiscount;
    public MetaTileEntityIntegratedMiningDivision(ResourceLocation metaTileEntityId) {
        super(metaTileEntityId, new RecipeMap[] {
                GTQTcoreRecipeMaps.INTEGRATED_MINING_DIVISION,
                RecipeMaps.ORE_WASHER_RECIPES,
                RecipeMaps.THERMAL_CENTRIFUGE_RECIPES,
                RecipeMaps.SIFTER_RECIPES,
                RecipeMaps.MACERATOR_RECIPES
        });
        this.recipeMapWorkable = new MetaTileEntityIntegratedMiningDivisionrWorkable(this);
    }
    public MetaTileEntity createMetaTileEntity(IGregTechTileEntity metaTileEntityHolder) {
        return new MetaTileEntityIntegratedMiningDivision(this.metaTileEntityId);
    }




    @Override
    protected void addDisplayText(List<ITextComponent> textList) {
        if (isStructureFormed()) {
            if (getInputFluidInventory() != null) {
                FluidStack LubricantStack = getInputFluidInventory().drain(Lubricant.getFluid(Integer.MAX_VALUE), false);
                int liquidOxygenAmount = LubricantStack == null ? 0 : LubricantStack.amount;
                textList.add(new TextComponentTranslation("gtqtcore.multiblock.ab.amount", TextFormattingUtil.formatNumbers((liquidOxygenAmount))));
                textList.add(new TextComponentTranslation("gtqtcore.multiblock.ab.heat"));
            }
        }
        if (isStructureFormed()) {
            textList.add(new TextComponentTranslation("gtqtcore.multiblock.md.level", heatingCoilLevel));
        }
        super.addDisplayText(textList);
    }

    @Override
    protected void addWarningText(List<ITextComponent> textList) {
        super.addWarningText(textList);
        if (isStructureFormed()) {
            FluidStack lubricantStack = getInputFluidInventory().drain(Lubricant.getFluid(Integer.MAX_VALUE), false);
            if (lubricantStack == null || lubricantStack.amount == 0) {
                textList.add(new TextComponentTranslation("gtqtcore.multiblock.md.no"));
            }
        }
    }
    @Override
    public void addInformation(ItemStack stack, @Nullable World player, @Nonnull List<String> tooltip, boolean advanced) {
        super.addInformation(stack, player, tooltip, advanced);
        tooltip.add(I18n.format("gtqtcore.multiblock.md.tooltip.1"));
        tooltip.add(I18n.format("gtqtcore.multiblock.md.tooltip.2"));
        tooltip.add(I18n.format("gtqtcore.multiblock.md.tooltip.3"));
        tooltip.add(I18n.format("gtqtcore.multiblock.md.tooltip.4"));
        tooltip.add(I18n.format("gtqtcore.multiblock.md.tooltip.5"));
        tooltip.add(TooltipHelper.RAINBOW_SLOW + I18n.format("矿石所需要的唯一", new Object[0]));
    }

    @Override
    protected void formStructure(PatternMatchContext context) {
        super.formStructure(context);
        Object coilType = context.get("CoilType");
        if (coilType instanceof IHeatingCoilBlockStats) {
            this.heatingCoilLevel = ((IHeatingCoilBlockStats) coilType).getLevel();
            this.heatingCoilDiscount = ((IHeatingCoilBlockStats) coilType).getEnergyDiscount();
        } else {
            this.heatingCoilLevel = BlockWireCoil.CoilType.CUPRONICKEL.getLevel();
            this.heatingCoilDiscount = BlockWireCoil.CoilType.CUPRONICKEL.getEnergyDiscount();
        }
    }

    @Override
    public void invalidateStructure() {
        super.invalidateStructure();
        this.heatingCoilLevel = 0;
        this.heatingCoilDiscount = 0;
    }

    @Nonnull
    protected BlockPattern createStructurePattern() {
        return FactoryBlockPattern.start()
                .aisle("CCMCC", "CGGGC", "CGCGC", "CGGGC", "CCCCC")
                .aisle("CBCBC", "BGGGB", "CGAGC", "BGGGB", "CBCBC")
                .aisle("CBCBC", "BXXXB", "CXAXC", "BXXXB", "CBCBC")
                .aisle("CBCBC", "BGGGB", "CGAGC", "BGGGB", "CBCBC")
                .aisle("CBCBC", "BGGGB", "CGAGC", "BGGGB", "CBCBC")
                .aisle("CBCBC", "BXXXB", "CXAXC", "BXXXB", "CBCBC")
                .aisle("CBCBC", "BGGGB", "CGAGC", "BGGGB", "CBCBC")
                .aisle("CCCCC", "CGGGC", "CGOGC", "CGGGC", "CCCCC")
                .where('O', this.selfPredicate())
                .where('C', states(this.getCasingState())
                        .or(abilities(MultiblockAbility.MAINTENANCE_HATCH).setExactLimit(1))
                        .or(abilities(MultiblockAbility.IMPORT_ITEMS).setMaxGlobalLimited(4).setPreviewCount(1))
                        .or(abilities(MultiblockAbility.EXPORT_ITEMS).setMaxGlobalLimited(4).setPreviewCount(1))
                        .or(abilities(MultiblockAbility.IMPORT_FLUIDS).setMaxGlobalLimited(8).setPreviewCount(1))
                        .or(abilities(MultiblockAbility.EXPORT_FLUIDS).setMaxGlobalLimited(8).setPreviewCount(1))
                        .or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1).setMaxGlobalLimited(3))
                )
                .where('X', heatingCoils())
                .where('M', abilities(MultiblockAbility.MUFFLER_HATCH))
                .where('A', states(this.getSecondCasingState()))
                .where('B', states(this.getPipeCasingState()))
                .where('G', states(this.getGlassState()))
                .build();
    }
    private IBlockState getCasingState() {
        return MetaBlocks.METAL_CASING.getState(BlockMetalCasing.MetalCasingType.PTFE_INERT_CASING);
    }
    private IBlockState getSecondCasingState() {
        return GCYSMetaBlocks.MULTIBLOCK_CASING.getState(BlockGCYSMultiblockCasing.CasingType.ADVANCED_SUBSTRATE);
    }
    private IBlockState getPipeCasingState() {
        return MetaBlocks.BOILER_CASING.getState(BlockBoilerCasing.BoilerCasingType.POLYTETRAFLUOROETHYLENE_PIPE);
    }
    private IBlockState getGlassState() {
        return GCYSMetaBlocks.TRANSPARENT_CASING.getState(BlockTransparentCasing.CasingType.PMMA);
    }

    public ICubeRenderer getBaseTexture(IMultiblockPart iMultiblockPart) {
        return Textures.INERT_PTFE_CASING;
    }

    @Override
    public SoundEvent getBreakdownSound() {
        return GTSoundEvents.BREAKDOWN_ELECTRICAL;
    }

    @SideOnly(Side.CLIENT)
    @Nonnull
    @Override
    protected OrientedOverlayRenderer getFrontOverlay() {
        return Textures.LARGE_CHEMICAL_REACTOR_OVERLAY;
    }

    @Override
    public boolean hasMufflerMechanics() {
        return true;
    }

    public static int getMaxParallel(int heatingCoilLevel) {
        return  64 * heatingCoilLevel;
    }
    private final FluidStack LUBRICANT_STACK = Lubricant.getFluid(1);
    protected class MetaTileEntityIntegratedMiningDivisionrWorkable extends MultiblockRecipeLogic {
        private final MetaTileEntityIntegratedMiningDivision combustionEngine;
        public MetaTileEntityIntegratedMiningDivisionrWorkable(RecipeMapMultiblockController tileEntity) {
            super(tileEntity);
            this.combustionEngine = (MetaTileEntityIntegratedMiningDivision) tileEntity;
        }
        @Override
        public int getParallelLimit() {
            return getMaxParallel(heatingCoilLevel);
        }
        protected void updateRecipeProgress() {
            if (canRecipeProgress && drawEnergy(recipeEUt, true)) {
                IMultipleTankHandler inputTank = combustionEngine.getInputFluidInventory();
                if (LUBRICANT_STACK.isFluidStackIdentical(inputTank.drain(LUBRICANT_STACK, false))) {
                    inputTank.drain(LUBRICANT_STACK, true);
                    if (++progressTime > maxProgressTime) {
                        completeRecipe();
                    }
                }
                else return;
                drawEnergy(recipeEUt, false);

            }
        }
    }
}