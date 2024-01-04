package keqing.gtqtcore.api.unification.matreials;

import gregtech.api.fluids.FluidBuilder;
import gregtech.api.unification.Elements;
import gregtech.api.unification.material.Material;
import gregtech.api.unification.material.properties.BlastProperty;
import gregtech.api.util.GTUtility;
import keqing.gtqtcore.api.unification.GTQTMaterials;

import static gregtech.api.GTValues.*;
import static gregtech.api.GTValues.UHV;
import static gregtech.api.unification.material.Materials.*;
import static gregtech.api.unification.material.Materials.Tetranitromethane;
import static gregtech.api.unification.material.info.MaterialFlags.*;
import static gregtech.api.unification.material.info.MaterialFlags.GENERATE_SMALL_GEAR;
import static gregtech.api.util.GTUtility.gregtechId;
import static keqing.gtqtcore.api.unification.GTQTMaterials.HighlyPurifiedCoalTar;
import static keqing.gtqtcore.api.unification.GTQTMaterials.Methylhydrazine;
import static keqing.gtqtcore.api.unification.material.info.GTQTMaterialIconSet.CUSTOM_MHCSM;

public class FirstDegreeMaterials {
    public FirstDegreeMaterials() {
    }
    private static int startId = 20000;
    private static final int END_ID = startId + 100;
    private static int getMaterialsId() {
        if (startId < END_ID) {
            return startId++;
        }
        throw new ArrayIndexOutOfBoundsException();
    }

    public static void register() {
        GTQTMaterials.HighPressureSteam = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("high_pressure_steam"))).fluid().color(14601607).build();
        GTQTMaterials.SteamExhaustGas = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("steam_exhaust_gas"))).fluid().color(14601607).build();
        GTQTMaterials.SuperheatedSteam = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("super_heated_steam"))).fluid().color(14601607).build();
        GTQTMaterials.Pyrotheum = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("pyrotheum"))).fluid().color(14601000).build();

        GTQTMaterials.StellarMaterialResidueA = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("stellar_material_residue_a")))  .fluid().plasma().color(14601000).build().setFormula("ST-α", true);
        GTQTMaterials.StellarMaterialResidueB = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("stellar_material_residue_b")))  .fluid().plasma().color(14600000).build().setFormula("ST-β", true);
        GTQTMaterials.StellarMaterialResidueC = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("stellar_material_residue_c")))  .fluid().plasma().color(14639000).build().setFormula("ST-γ", true);
        GTQTMaterials.StellarMaterial = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("stellar_material")))  .fluid().plasma().color(14638000).build().setFormula("ST-ST", true);

        GTQTMaterials.LightNaquadahFuel = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("light_naquadah_fuel"))) .fluid().color(14638000).build().setFormula("NQ", true);
        GTQTMaterials.MediumNaquadahFuel = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("medium_naquadah_fuel"))) .fluid().color(14638000).build().setFormula("-NQ-", true);
        GTQTMaterials.HeavyNaquadahFuel = (new Material.Builder(getMaterialsId(), GTUtility.gregtechId("heavy_naquadah_fuel"))) .fluid().color(14638000).build().setFormula("+NQ+", true);

        GTQTMaterials.MagnetoHydrodynamicallyConstrainedStarMatter = new Material.Builder(getMaterialsId(), gregtechId("magneto_hydrodynamically_constrained_star_matter"))
                .ingot()
                .liquid(new FluidBuilder().temperature(6000000))
                .iconSet(CUSTOM_MHCSM)
                .flags(NO_SMELTING, GENERATE_PLATE, GENERATE_DOUBLE_PLATE, GENERATE_ROD, GENERATE_LONG_ROD, GENERATE_RING, GENERATE_ROUND, GENERATE_BOLT_SCREW, GENERATE_FRAME, GENERATE_GEAR, GENERATE_SMALL_GEAR)
                .build();

        GTQTMaterials.DenseHydrazineMixtureFuel = new Material.Builder(getMaterialsId(), gregtechId("dense_hydrazine_mixture_fuel"))
                .fluid()
                .color(0x912565)
                .components(Dimethylhydrazine, 1, Methanol, 1)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //  24324 Highly Purified Coal Tar
        HighlyPurifiedCoalTar = new Material.Builder(getMaterialsId(), gregtechId("highly_purified_coal_tar"))
                .fluid()
                .color(0x7F811D)
                .components(CoalTar, 1)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //  24325 RP-1 Rocket Fuel
        GTQTMaterials.RP1RocketFuel = new Material.Builder(getMaterialsId(), gregtechId("rp_1_rocket_fuel"))
                .fluid()
                .color(0xFB2A08)
                .components(HighlyPurifiedCoalTar, 1, Oxygen, 1)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //  24326 Methylhydrazine
        Methylhydrazine = new Material.Builder(getMaterialsId(), gregtechId("methylhydrazine"))
                .fluid()
                .color(0x321452)
                .components(Carbon, 1, Hydrogen, 6, Nitrogen, 2)
                .build();
        //  24327 Methylhydrazine Nitrate Rocket Fuel
        GTQTMaterials.MethylhydrazineNitrateRocketFuel = new Material.Builder(getMaterialsId(), gregtechId("methylhydrazine_nitrate_rocket_fuel"))
                .fluid()
                .color(0x607186)
                .components(Methylhydrazine, 1, Tetranitromethane, 1)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        GTQTMaterials.Alumite = new Material.Builder(getMaterialsId(), gregtechId("alumite"))
                .fluid()
                .ingot()
                .color(0x607186)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        GTQTMaterials.Alubrassa = new Material.Builder(getMaterialsId(), gregtechId("alubrassa"))
                .fluid()
                .ingot()
                .color(0x321452)
                .flags(DISABLE_DECOMPOSITION)
                .components(Aluminium, 3, Copper, 1)
                .build();
        //含杂预处理原油
        GTQTMaterials.PreTreatedCrudeOilContainingImpurities = new Material.Builder(getMaterialsId(), gregtechId("pre_treated_crude_oil_containing_impurities"))
                .fluid()
                .color(0x262626)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //预处理原油
        GTQTMaterials.PreTreatedCrudeOil = new Material.Builder(getMaterialsId(), gregtechId("pre_treated_crude_oil"))
                .fluid()
                .color(0xBDB76B)
                .flags(DISABLE_DECOMPOSITION)
                .build();

        //破乳剂
        GTQTMaterials.Demulsifier = new Material.Builder(getMaterialsId(), gregtechId("demulsifier"))
                .fluid()
                .color(0xC6E2FF)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //环氧丙烷
        GTQTMaterials.PropyleneOxide = new Material.Builder(getMaterialsId(), gregtechId("propylene_oxide"))
                .fluid()
                .color(0xBDB76B)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //沥青
        GTQTMaterials.Asphalt = new Material.Builder(getMaterialsId(), gregtechId("asphalt"))
                .fluid()
                .color(0x282828)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //蜡油
        GTQTMaterials.WaxOil = new Material.Builder(getMaterialsId(), gregtechId("wax_oil"))
                .fluid()
                .color(0x8B8B00)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //水煤气
        GTQTMaterials.WaterGas = new Material.Builder(getMaterialsId(), gregtechId("water_gas"))
                .fluid()
                .color(0xA9A9A9)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //常压渣油
        GTQTMaterials.AtmosphericResidue = new Material.Builder(getMaterialsId(), gregtechId("atmospheric_residue"))
                .fluid()
                .color(0x8B1A1A)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //减压渣油
        GTQTMaterials.VacuumResidue = new Material.Builder(getMaterialsId(), gregtechId("vacuum_residue"))
                .fluid()
                .color(0x8B2323)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //粗柴油
        GTQTMaterials.GasOil = new Material.Builder(getMaterialsId(), gregtechId("gas_oil"))
                .fluid()
                .color(0x8B7355)
                .flags(DISABLE_DECOMPOSITION)
                .build();
        //油浆
        GTQTMaterials.OilSlurry = new Material.Builder(getMaterialsId(), gregtechId("oil_slurry"))
                .fluid()
                .color(0x8B7355)
                .flags(DISABLE_DECOMPOSITION)
                .build();
    }
}
