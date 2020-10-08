package calemiutils;

import calemiutils.command.CUCommandBase;
import calemiutils.command.DyeColorArgument;
import calemiutils.event.*;
import calemiutils.gui.*;
import calemiutils.init.*;
import calemiutils.packet.*;
import calemiutils.render.RenderBookStand;
import calemiutils.render.RenderItemStand;
import calemiutils.render.RenderTradingPost;
import calemiutils.world.WorldGenOre;
import net.minecraft.client.gui.ScreenManager;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.RenderTypeLookup;
import net.minecraft.client.renderer.texture.AtlasTexture;
import net.minecraft.command.arguments.ArgumentSerializer;
import net.minecraft.command.arguments.ArgumentTypes;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Tuple;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.TextureStitchEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DeferredWorkQueue;
import net.minecraftforge.fml.InterModComms;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.event.lifecycle.InterModEnqueueEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import net.minecraftforge.server.command.EnumArgument;
import net.minecraftforge.server.command.ModIdArgument;
import top.theillusivec4.curios.api.CuriosAPI;
import top.theillusivec4.curios.api.imc.CurioIMCMessage;

@Mod(CUReference.MOD_ID)
@Mod.EventBusSubscriber(bus=Mod.EventBusSubscriber.Bus.MOD)
public class CalemiUtils {

    public static final ResourceLocation EMPTY_WALLET_SLOT = new ResourceLocation(CUReference.MOD_ID, "items/empty_wallet_slot");
    public static boolean curiosLoaded = false;

    public static final ItemGroup TAB = new CUTab();
    public static CalemiUtils instance;
    public static SimpleChannel network;
    public static IEventBus MOD_EVENT_BUS;

    public CalemiUtils () {

        curiosLoaded = ModList.get().isLoaded("curios");

        MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();
        MOD_EVENT_BUS.addListener(this::setup);
        MOD_EVENT_BUS.addListener(this::doClientStuff);

        InitTileEntityTypes.TILE_ENTITY_TYPES.register(MOD_EVENT_BUS);
        InitContainersTypes.CONTAINER_TYPES.register(MOD_EVENT_BUS);
        InitEnchantments.ENCHANTMENTS.register(MOD_EVENT_BUS);

        InitItems.init();

        instance = this;
        MinecraftForge.EVENT_BUS.register(this);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, CUConfig.spec);
    }

    private void setup (final FMLCommonSetupEvent event) {

        int id = 0;
        network = NetworkRegistry.newSimpleChannel(new ResourceLocation(CUReference.MOD_ID, CUReference.MOD_ID), () -> "1.0", s -> true, s -> true);
        network.registerMessage(++id, PacketEnableTileEntity.class, PacketEnableTileEntity::toBytes, PacketEnableTileEntity::new, PacketEnableTileEntity::handle);
        network.registerMessage(++id, PacketPencilSetColor.class, PacketPencilSetColor::toBytes, PacketPencilSetColor::new, PacketPencilSetColor::handle);
        network.registerMessage(++id, PacketLinkBook.class, PacketLinkBook::toBytes, PacketLinkBook::new, PacketLinkBook::handle);
        network.registerMessage(++id, PacketItemStand.class, PacketItemStand::toBytes, PacketItemStand::new, PacketItemStand::handle);
        network.registerMessage(++id, PacketWallet.class, PacketWallet::toBytes, PacketWallet::new, PacketWallet::handle);
        network.registerMessage(++id, PacketOpenWallet.class, PacketOpenWallet::toBytes, PacketOpenWallet::new, PacketOpenWallet::handle);
        network.registerMessage(++id, PacketBank.class, PacketBank::toBytes, PacketBank::new, PacketBank::handle);
        network.registerMessage(++id, PacketTradingPost.class, PacketTradingPost::toBytes, PacketTradingPost::new, PacketTradingPost::handle);

        MinecraftForge.EVENT_BUS.register(new WrenchEvent());
        MinecraftForge.EVENT_BUS.register(new SecurityEvent());
        MinecraftForge.EVENT_BUS.register(new MobBeaconEvent());

        DeferredWorkQueue.runLater(WorldGenOre::onCommonSetup);

        ArgumentTypes.register("cu:color", DyeColorArgument.class, new ArgumentSerializer<>(DyeColorArgument::color));
    }

    private void doClientStuff (final FMLClientSetupEvent event) {

        MinecraftForge.EVENT_BUS.register(new WrenchLoreEvent());
        MinecraftForge.EVENT_BUS.register(new SledgehammerChargeOverlayEvent());
        MinecraftForge.EVENT_BUS.register(new TradingPostOverlayEvent());
        MinecraftForge.EVENT_BUS.register(new WalletOverlayEvent());
        MinecraftForge.EVENT_BUS.register(new WalletKeyEvent());

        RenderTypeLookup.setRenderLayer(InitItems.BLUEPRINT.get(), RenderType.func_228643_e_());
        RenderTypeLookup.setRenderLayer(InitItems.IRON_SCAFFOLD.get(), RenderType.func_228643_e_());
        RenderTypeLookup.setRenderLayer(InitItems.BOOK_STAND.get(), RenderType.func_228643_e_());
        RenderTypeLookup.setRenderLayer(InitItems.ITEM_STAND.get(), RenderType.func_228643_e_());
        RenderTypeLookup.setRenderLayer(InitItems.TRADING_POST.get(), RenderType.func_228643_e_());

        ScreenManager.registerFactory(InitContainersTypes.WALLET.get(), ScreenWallet::new);
        ScreenManager.registerFactory(InitContainersTypes.TORCH_PLACER.get(), ScreenTorchPlacer::new);
        ScreenManager.registerFactory(InitContainersTypes.BOOK_STAND.get(), ScreenOneSlot::new);
        ScreenManager.registerFactory(InitContainersTypes.ITEM_STAND.get(), ScreenOneSlot::new);
        ScreenManager.registerFactory(InitContainersTypes.BANK.get(), ScreenBank::new);
        ScreenManager.registerFactory(InitContainersTypes.TRADING_POST.get(), ScreenTradingPost::new);

        ClientRegistry.bindTileEntityRenderer(InitTileEntityTypes.BOOK_STAND.get(), RenderBookStand::new);
        ClientRegistry.bindTileEntityRenderer(InitTileEntityTypes.ITEM_STAND.get(), RenderItemStand::new);
        ClientRegistry.bindTileEntityRenderer(InitTileEntityTypes.TRADING_POST.get(), RenderTradingPost::new);

        InitKeyBindings.init();
    }

    public void onServerStarting (FMLServerStartingEvent event) {
        CUCommandBase.register(event.getCommandDispatcher());
    }

    @SubscribeEvent
    @OnlyIn(Dist.CLIENT)
    public static void onTextureStitch(TextureStitchEvent.Pre event) {

        if (event.getMap().func_229223_g_().equals(AtlasTexture.LOCATION_BLOCKS_TEXTURE)) {
            event.addSprite(EMPTY_WALLET_SLOT);
        }
    }

    @SubscribeEvent
    public static void onModRegister(InterModEnqueueEvent event) {

        if (curiosLoaded) {
            InterModComms.sendTo("curios", CuriosAPI.IMC.REGISTER_TYPE, () -> new CurioIMCMessage("wallet").setSize(1));
            InterModComms.sendTo("curios", CuriosAPI.IMC.REGISTER_TYPE, () -> new CurioIMCMessage("belt").setSize(1));
            InterModComms.sendTo("curios", CuriosAPI.IMC.REGISTER_ICON, () -> new Tuple<>("wallet", EMPTY_WALLET_SLOT));
        }
    }
}