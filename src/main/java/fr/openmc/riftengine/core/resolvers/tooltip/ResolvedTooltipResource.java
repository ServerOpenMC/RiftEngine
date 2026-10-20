package fr.openmc.riftengine.core.resolvers.tooltip;

import fr.openmc.riftengine.api.utils.IdentifierUtils;
import fr.openmc.riftengine.core.resolvers.mcmeta.animation.AnimationMcMetaResolver;
import fr.openmc.riftengine.core.resolvers.mcmeta.animation.ResolvedAnimationMcMeta;
import fr.openmc.riftengine.core.resolvers.mcmeta.gui.GuiMcMetaResolver;
import fr.openmc.riftengine.core.resolvers.mcmeta.gui.ResolvedGuiMcMeta;
import fr.openmc.riftengine.core.utils.PathUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public record ResolvedTooltipResource(
        String resourceId // ex omc_tooltips:tooltip/legendary
) {
    public String getKey() {
        String keySplit = IdentifierUtils.normalizeId(resourceId).split(":")[1];
        String[] keySplit2 = keySplit.split("/");
        return keySplit2[keySplit2.length - 1];
    }

    public String backgroundResourceId() {
        return resourceId + "_background";
    }

    public String frameResourceId() {
        return resourceId + "_frame";
    }

    private String normalizeTooltipId(String id) {
        String normalized = IdentifierUtils.normalizeId(id);
        String[] keySplit = normalized.split(":");
        String key = "gui/sprites/tooltip/" +  keySplit[1];

        return keySplit[0] + ":" + key;
    }

    public Path backgroundImagePath(Path javaRoot) {
        Path path = IdentifierUtils.resolveTextureId(javaRoot, IdentifierUtils.normalizeId(backgroundResourceId()));

        if (path.toFile().exists())
            return path;

        return IdentifierUtils.resolveTextureId(javaRoot, normalizeTooltipId(backgroundResourceId()));
    }

    public Path frameImagePath(Path javaRoot) {
        Path path = IdentifierUtils.resolveTextureId(javaRoot, IdentifierUtils.normalizeId(frameResourceId()));

        if (path.toFile().exists())
            return path;

        return IdentifierUtils.resolveTextureId(javaRoot, normalizeTooltipId(frameResourceId()));
    }

    public Path backgroundMCMetaPath(Path javaRoot) {
        Path path = IdentifierUtils.resolveMetaId(javaRoot, IdentifierUtils.normalizeId(backgroundResourceId()), ".png");

        if (path.toFile().exists())
            return path;

        return IdentifierUtils.resolveMetaId(javaRoot, normalizeTooltipId(backgroundResourceId()), ".png");
    }

    public Path frameMCMetaPath(Path javaRoot) {
        Path path = IdentifierUtils.resolveMetaId(javaRoot, IdentifierUtils.normalizeId(frameResourceId()), ".png");

        if (path.toFile().exists())
            return path;

        return IdentifierUtils.resolveMetaId(javaRoot, normalizeTooltipId(frameResourceId()), ".png");
    }

    public Path backgroundImageBedrockPath(Path javaRootPath) {
        return PathUtils.getPathFromRoot(backgroundImagePath(javaRootPath), "textures");
    }

    public Path frameImageBedrockPath(Path javaRootPath) {
        return PathUtils.getPathFromRoot(frameImagePath(javaRootPath), "textures");
    }


    // todo: sacré bordel, surement faier un record Animationinfo nn?
    public int numberFrame(Path javaRootPath, ResolvedGuiMcMeta gui) throws IOException {
        Path frameImagePath = this.frameImagePath(javaRootPath);
        BufferedImage src = ImageIO.read(frameImagePath.toFile());

        int width = src.getWidth();
        int height = src.getHeight();

        int realWidth = gui.widthFrame() != null ? gui.widthFrame() :
                (gui.heightFrame() != null ? gui.heightFrame() : Math.min(width, height));
        int realHeight = gui.heightFrame() != null ? gui.heightFrame() :
                (gui.widthFrame() != null ? gui.widthFrame() : Math.min(width, height));

        int cols = width / realWidth;
        return cols * (height / realHeight);
    }

    public FlipbookAnimation getFrameFlipbookAnimation(AnimationMcMetaResolver animationMcMetaResolver, GuiMcMetaResolver guiMcMetaResolver, Path javaRootPath) throws IOException {
        ResolvedGuiMcMeta guiMeta = guiMcMetaResolver.resolve(javaRootPath, frameMCMetaPath(javaRootPath));
        ResolvedAnimationMcMeta animationMeta = animationMcMetaResolver.resolve(javaRootPath, frameMCMetaPath(javaRootPath));

        if (guiMeta == null)
            return null;

        Double fps = animationMeta == null ? null : (20d / animationMeta.frameTime());
        return new FlipbookAnimation(this.numberFrame(javaRootPath, guiMeta),
                guiMeta.heightFrame(),
                guiMeta.widthFrame(),
                fps);
    }

    public record FlipbookAnimation(int frameCount, int heightFrame, int widthFrame, Double fps) {}
}
