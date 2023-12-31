package keqing.gtqtcore.common.metatileentities.multi.multiblock.standard;

import gregtech.api.capability.IObjectHolder;
import gregtech.api.capability.IOpticalComputationHatch;
import gregtech.api.capability.IOpticalComputationProvider;
import gregtech.api.capability.IOpticalComputationReceiver;
import gregtech.api.capability.impl.ComputationRecipeLogic;
import gregtech.api.capability.impl.ItemHandlerList;
import gregtech.api.metatileentity.MetaTileEntity;
import gregtech.api.metatileentity.interfaces.IGregTechTileEntity;
import gregtech.api.metatileentity.multiblock.IMultiblockPart;
import gregtech.api.metatileentity.multiblock.MultiblockAbility;
import gregtech.api.metatileentity.multiblock.RecipeMapMultiblockController;
import gregtech.api.pattern.BlockPattern;
import gregtech.api.pattern.FactoryBlockPattern;
import gregtech.api.pattern.PatternMatchContext;
import gregtech.api.recipes.Recipe;
import gregtech.client.renderer.ICubeRenderer;
import gregtech.client.renderer.texture.Textures;
import gregtech.common.blocks.BlockComputerCasing;
import gregtech.common.blocks.MetaBlocks;
import keqing.gtqtcore.api.recipes.GTQTcoreRecipeMaps;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import net.minecraftforge.items.IItemHandlerModifiable;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Collections;


public class MetaTileEntitykeQingNet extends RecipeMapMultiblockController implements IOpticalComputationReceiver {
        private IOpticalComputationProvider computationProvider;
        private IObjectHolder objectHolder;

        public MetaTileEntitykeQingNet(ResourceLocation metaTileEntityId) {
            super(metaTileEntityId, GTQTcoreRecipeMaps.KEQING_NET_RECIES);
            this.recipeMapWorkable = new ResearchStationRecipeLogic(this);
        }

        public MetaTileEntity createMetaTileEntity(IGregTechTileEntity tileEntity) {
            return new MetaTileEntitykeQingNet(this.metaTileEntityId);
        }

        protected void formStructure(PatternMatchContext context) {
            super.formStructure(context);
            List<IOpticalComputationHatch> providers = this.getAbilities(MultiblockAbility.COMPUTATION_DATA_RECEPTION);
            if (providers != null && providers.size() >= 1) {
                this.computationProvider = (IOpticalComputationProvider)providers.get(0);
            }

            List<IObjectHolder> holders = this.getAbilities(MultiblockAbility.OBJECT_HOLDER);
            if (holders != null && holders.size() >= 1) {
                this.objectHolder = (IObjectHolder)holders.get(0);
                this.inputInventory = new ItemHandlerList(Collections.singletonList(this.objectHolder.getAsHandler()));
            }

            if (this.computationProvider == null || this.objectHolder == null) {
                this.invalidateStructure();
            }

        }


        public ComputationRecipeLogic getRecipeMapWorkable() {
            return (ComputationRecipeLogic)this.recipeMapWorkable;
        }

        public void invalidateStructure() {
            this.computationProvider = null;
            List<IObjectHolder> holders = this.getAbilities(MultiblockAbility.OBJECT_HOLDER);
            if (holders != null && holders.size() >= 1 && holders.get(0) == this.objectHolder) {
                this.objectHolder.setLocked(false);
            }

            this.objectHolder = null;
            super.invalidateStructure();
        }

        public IOpticalComputationProvider getComputationProvider() {
            return this.computationProvider;
        }

        public IObjectHolder getObjectHolder() {
            return this.objectHolder;
        }

        @Nonnull
        protected  BlockPattern createStructurePattern() {
            return FactoryBlockPattern.start()
                    .aisle("XXX", "VVV", "PPP", "PSP", "PPP", "VVV", "XXX")
                    .aisle("XXX", "VAV", "XAX", "XAX", "XAX", "VAV", "XXX")
                    .aisle("XXX", "VAV", "XAX", "XAX", "XAX", "VAV", "XXX")
                    .aisle("XXX", "VAV", "XAX", "XAX", "XAX", "VAV", "XXX")
                    .aisle("XXX", "VAV", "XAX", "XAX", "XAX", "VAV", "XXX")
                    .aisle("XXX", "VAV", "XAX", "XAX", "XAX", "VAV", "XXX")
                    .aisle("XXX", "VVV", "PPP", "PHP", "PPP", "VVV", "XXX")
                    .where('S', this.selfPredicate()).where('X', states(getCasingState()))
                    .where(' ', any()).where('-', air())
                    .where('V', states(getVentState()))
                    .where('A', states(getAdvancedState())).where('P', states(new IBlockState[]{getCasingState()}).or(abilities(MultiblockAbility.INPUT_ENERGY).setMinGlobalLimited(1)).or(abilities(MultiblockAbility.MAINTENANCE_HATCH).setExactLimit(1)).or(abilities(MultiblockAbility.COMPUTATION_DATA_RECEPTION).setExactLimit(1))).where('H', abilities(MultiblockAbility.OBJECT_HOLDER)).build();
        }


        private static  IBlockState getVentState() {
            return MetaBlocks.COMPUTER_CASING.getState(BlockComputerCasing.CasingType.COMPUTER_HEAT_VENT);
        }

        private static  IBlockState getAdvancedState() {
            return MetaBlocks.COMPUTER_CASING.getState(BlockComputerCasing.CasingType.ADVANCED_COMPUTER_CASING);
        }

        private static  IBlockState getCasingState() {
            return MetaBlocks.COMPUTER_CASING.getState(BlockComputerCasing.CasingType.COMPUTER_CASING);
        }

        @SideOnly(Side.CLIENT)
        public ICubeRenderer getBaseTexture(IMultiblockPart sourcePart) {
            return sourcePart != null && !(sourcePart instanceof IObjectHolder) ? Textures.COMPUTER_CASING : Textures.ADVANCED_COMPUTER_CASING;
        }

        @Nonnull
        @SideOnly(Side.CLIENT)
        protected  ICubeRenderer getFrontOverlay() {
            return Textures.RESEARCH_STATION_OVERLAY;
        }

        protected boolean shouldShowVoidingModeButton() {
            return false;
        }

        public boolean canVoidRecipeItemOutputs() {
            return true;
        }

        public void addInformation(ItemStack stack, World world, List<String> tooltip, boolean advanced) {
            super.addInformation(stack, world, tooltip, advanced);
            tooltip.add(I18n.format("gregtech.machine.research_station.tooltip.1"));
            tooltip.add(I18n.format("gregtech.machine.research_station.tooltip.2"));
            tooltip.add(I18n.format("gregtech.machine.research_station.tooltip.3"));
            tooltip.add(I18n.format("gregtech.machine.research_station.tooltip.4"));
        }

        protected void addDisplayText(List<ITextComponent> textList) {
            super.addDisplayText(textList);
            if (this.isStructureFormed() && this.isActive()) {
                ComputationRecipeLogic recipeLogic = this.getRecipeMapWorkable();
                textList.add(new TextComponentTranslation("gregtech.multiblock.computation.usage", recipeLogic.getCurrentDrawnCWUt()));
                if (recipeLogic.isHasNotEnoughComputation()) {
                    textList.add((new TextComponentTranslation("gregtech.multiblock.computation.not_enough_computation")).setStyle((new Style()).setColor(TextFormatting.RED)));
                }
            }

        }

        protected void addWarningText(List<ITextComponent> textList) {
            super.addWarningText(textList);
            if (this.isStructureFormed() && this.isActive() && this.getRecipeMapWorkable().isHasNotEnoughComputation()) {
                textList.add((new TextComponentTranslation("gregtech.multiblock.computation.not_enough_computation")).setStyle((new Style()).setColor(TextFormatting.RED)));
            }

        }

        private static class ResearchStationRecipeLogic extends ComputationRecipeLogic {
            public ResearchStationRecipeLogic(MetaTileEntitykeQingNet metaTileEntity) {
                super(metaTileEntity, ComputationType.SPORADIC);
            }

            @Nonnull
            public  MetaTileEntitykeQingNet getMetaTileEntity() {
                return (MetaTileEntitykeQingNet)super.getMetaTileEntity();
            }

            public boolean isAllowOverclocking() {
                return false;
            }

            protected boolean setupAndConsumeRecipeInputs( Recipe recipe,  IItemHandlerModifiable importInventory) {
                this.overclockResults = new int[]{recipe.getEUt(), recipe.getDuration()};
                if (!this.hasEnoughPower(this.overclockResults)) {
                    return false;
                } else if (recipe.matches(false, importInventory, this.getInputTank())) {
                    this.metaTileEntity.addNotifiedInput(importInventory);
                    return true;
                } else {
                    return false;
                }
            }

            protected void setupRecipe(Recipe recipe) {
                IObjectHolder holder = this.getMetaTileEntity().getObjectHolder();
                holder.setLocked(true);
                super.setupRecipe(recipe);
            }

            protected void outputRecipeOutputs() {
                IObjectHolder holder = this.getMetaTileEntity().getObjectHolder();
                holder.setHeldItem(ItemStack.EMPTY);
                ItemStack outputItem = ItemStack.EMPTY;
                if (this.itemOutputs != null && this.itemOutputs.size() >= 1) {
                    outputItem = (ItemStack)this.itemOutputs.get(0);
                }

                holder.setDataItem(outputItem);
                holder.setLocked(false);
            }
        }
    }
