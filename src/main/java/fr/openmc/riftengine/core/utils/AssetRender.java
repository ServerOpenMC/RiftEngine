package fr.openmc.riftengine.core.utils;


import dev.simplified.collection.Concurrent;
import lib.minecraft.renderer.BlockRenderer;
import lib.minecraft.renderer.call.request.BlockOptions;
import lib.minecraft.renderer.call.result.RenderResult;
import lib.minecraft.renderer.content.client.ClientAcquisition;
import lib.minecraft.renderer.content.client.ClientAssets;
import lib.minecraft.renderer.content.client.ClientOptions;
import lib.minecraft.renderer.content.index.RendererContext;
import lombok.Getter;

import java.io.File;

public class AssetRender {
    @Getter
    private final RendererContext context;
    public AssetRender(File javaResourcePack) {
        ClientOptions clientOptions = ClientOptions.builder()
                .version("26.3")
                .texturePacks(Concurrent.newList(javaResourcePack))
                .build();
        ClientAssets assets = ClientAcquisition.acquire(clientOptions);
        this.context = RendererContext.load(assets);

    }

    public RenderResult render(String blockId) {
        return new BlockRenderer(context).render(BlockOptions.builder()
                .blockId(blockId)
                .type(BlockOptions.Type.ISOMETRIC_3D)
                .build());
    }
}
