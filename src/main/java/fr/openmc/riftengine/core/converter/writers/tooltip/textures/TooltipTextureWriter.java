package fr.openmc.riftengine.core.converter.writers.tooltip.textures;

import fr.openmc.riftengine.api.scanner.items.ItemEntry;
import fr.openmc.riftengine.api.utils.IdentifierUtils;
import fr.openmc.riftengine.core.RiftLog;
import fr.openmc.riftengine.core.converter.writers.PackWriter;
import fr.openmc.riftengine.core.resolvers.mcmeta.gui.GuiMcMetaResolver;
import fr.openmc.riftengine.core.resolvers.mcmeta.gui.ResolvedGuiMcMeta;
import fr.openmc.riftengine.core.resolvers.tooltip.ResolvedTooltipResource;
import fr.openmc.riftengine.core.resolvers.tooltip.TooltipResourceResolver;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


// - ecrire les textures dans le resourcepack bedrock
//   * faire un scanner pour savoir les frames, fps, et tt chose qui sont relié entre les textures et ui_common.json
// - allouer un glyph pour chaque "type" de tooltip + le mettre dans l'item name + demander reload ia
// - ecrire le ui/ui_common.json (une giga template que j'adapte en fonction des textures)
public class TooltipTextureWriter implements PackWriter {
    private final List<ItemEntry> items;
    private final GuiMcMetaResolver guiResolver;
    private final TooltipResourceResolver resolver;

    public TooltipTextureWriter(List<ItemEntry> items,
                                GuiMcMetaResolver guiResolver,
                                TooltipResourceResolver resolver) {
        this.items = items;
        this.guiResolver = guiResolver;
        this.resolver = resolver;
    }

    @Override
    public void write(Path bedrockRootPath, Path javaRootPath) throws IOException {
        // todo: faire un TooltipScanner quand OpenMC sera divisé en module
        Map<String, ResolvedTooltipResource> tooltipResources = new HashMap<>();
        for (ItemEntry itemEntry : items) {
            ResolvedTooltipResource resource = resolver.resolve(javaRootPath, itemEntry);
            if (resource == null) continue;

            tooltipResources.put(resource.resourceId(), resource);
        }

        for (ResolvedTooltipResource resource : tooltipResources.values()) {
            Path backgroundImagePath = resource.backgroundImagePath(javaRootPath);
            Path frameImagePath = resource.frameImagePath(javaRootPath);

            Path reducedBackgroundImagePath = resource.backgroundImageBedrockPath(javaRootPath);
            Path reducedFrameImagePath = resource.frameImageBedrockPath(javaRootPath);

            if (reducedBackgroundImagePath == null) {
                RiftLog.PATH_CANT_REDUCED_TO_TEXTURES.send(backgroundImagePath.toString());
                return;
            }

            this.handleTextureUnanimated(bedrockRootPath, backgroundImagePath, reducedBackgroundImagePath);

            ResolvedGuiMcMeta resolvedGuiMcMeta = guiResolver.resolve(javaRootPath, resource.frameMCMetaPath(javaRootPath));
            if (resolvedGuiMcMeta == null) {
                this.handleTextureUnanimated(bedrockRootPath, frameImagePath, reducedFrameImagePath);
            } else {
                this.handleTextureAnimated(bedrockRootPath, resolvedGuiMcMeta, frameImagePath, reducedFrameImagePath);
            }
        }
    }

    private void handleTextureUnanimated(Path bedrockRootPath, Path imagePath, Path reducedImagePath) throws IOException {
        this.copyFile(imagePath, bedrockRootPath.resolve(IdentifierUtils.toBedrockTexturePath(reducedImagePath.toString())));
    }

    private void handleTextureAnimated(Path bedrockRootPath, ResolvedGuiMcMeta resolvedGuiMcMeta, Path imagePath, Path reducedImagePath) throws IOException {
        FrameTextureConverter.convert(
                resolvedGuiMcMeta,
                imagePath,
                bedrockRootPath.resolve(IdentifierUtils.toBedrockTexturePath(reducedImagePath.toString()))
        );
    }
}