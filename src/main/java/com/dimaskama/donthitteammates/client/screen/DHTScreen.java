package com.dimaskama.donthitteammates.client.screen;

import com.dimaskama.donthitteammates.client.DHTMod;
import com.dimaskama.donthitteammates.client.config.Teammate;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EntryListWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.widget.TexturedButtonWidget;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class DHTScreen extends Screen {
    public static final Text X_TEXT = Text.literal("X");
    public static final Text INPUT_PLACEHOLDER = Text.translatable("text.donthitteammates.nickname_input").styled(style -> style.withColor(0x777777));
    public static final Text BLACK_NICKNAME_ERROR = Text.translatable("error.donthitteammates.black_nickname");
    public static final Text ALREADY_IN_LIST_ERROR = Text.translatable("error.donthitteammates.already_in_list");
    public static final Identifier UNKNOWN_PLAYER_TEXTURE = new Identifier(DHTMod.MOD_ID, "textures/gui/unknown_player.png");
    public static final Identifier ACCEPT_BUTTON = new Identifier(DHTMod.MOD_ID, "textures/gui/accept_button.png");
    private final Screen parent;
    private TeammatesListWidget listWidget;
    private TextFieldWidget inputField;
    private Identifier inputTexture = UNKNOWN_PLAYER_TEXTURE;
    private boolean firstInit = true;
    private Text errorText;
    private int errorTextTime;

    public DHTScreen(Screen parent) {
        super(Text.translatable(DHTMod.MOD_ID));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (firstInit) {
            firstInit = false;
            listWidget = new TeammatesListWidget(client, width, height, 32, height - 64, 40, DHTMod.CONFIG.teammates);
            addSelectableChild(listWidget);

            inputField = new TextFieldWidget(
                    MinecraftClient.getInstance().textRenderer,
                    (width - 120) >> 1,
                    height - 56,
                    120,
                    20,
                    Text.empty()
            );
            inputField.setPlaceholder(INPUT_PLACEHOLDER);
            inputField.setChangedListener(string -> {
                if (!string.isBlank()) inputTexture = getSkinByName(string);
            });

            addDrawableChild(inputField);
        } else {
            inputField.setPosition((width - 120) >> 1, height - 56);
            listWidget.updateSize(width, height, 32, height - 64);
        }

        // Add teammate button
        addDrawableChild(new TexturedButtonWidget(
                ((width + 120) >> 1) + 3, height - 57,
                22, 22,
                0, 0,
                22,
                ACCEPT_BUTTON,
                22, 44,
                button -> {
                    String nickname = inputField.getText();
                    inputField.setText("");
                    focusOn(inputField);
                    if (nickname.isBlank()) {
                        setErrorText(BLACK_NICKNAME_ERROR);
                        return;
                    }
                    if (listWidget.hasNickname(nickname)) {
                        setErrorText(ALREADY_IN_LIST_ERROR);
                        return;
                    }
                    listWidget.addTeammate(new Teammate(nickname));
                }
        ));

        // Back button
        addDrawableChild(ButtonWidget.builder(ScreenTexts.BACK, button -> {
            close();
        }).dimensions((width - 120) >> 1, height - 28, 120, 20).build());
    }

    private void setErrorText(Text errorText) {
        this.errorText = errorText;
        errorTextTime = 60;
    }

    @Override
    public void tick() {
        if (errorTextTime > 0) errorTextTime--;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        renderBackground(context);
        context.drawCenteredTextWithShadow(textRenderer, title, width >> 1, 8, 0xffffff);

        listWidget.render(context, mouseX, mouseY, delta);

        int headX = ((width - 120) >> 1) - 36;
        int headY = height - 56;
        context.fill(headX - 1, headY - 1, headX + 33, headY + 33, 0xffffffff);
        context.drawTexture(inputTexture, headX, headY, 32, 32, 8, 8, 8, 8, 64, 64);

        if (errorText != null && errorTextTime > 0) {
            int alpha = (int) (Math.min(1.0F, (errorTextTime - delta) / 20.0F) * 255.0F);
            int textWidth = textRenderer.getWidth(errorText);
            context.fill((width - textWidth >> 1) - 4, height - 84, (width + textWidth >> 1) + 4, height - 65, alpha >> 1 << 24);
            context.drawCenteredTextWithShadow(textRenderer, errorText, width >> 1, height - 79, alpha << 24 | 0xff9999);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        DHTMod.CONFIG.teammates = listWidget.getTeammatesList();
        DHTMod.CONFIG.saveJson();
        if (client != null) client.setScreen(parent);
        else super.close();
    }

    public static Identifier getSkinByName(String name) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        Identifier skin = UNKNOWN_PLAYER_TEXTURE;
        if (player != null) {
            PlayerEntity renderPlayer = player.getWorld().getPlayers().stream().filter(p -> p.getEntityName().equalsIgnoreCase(name)).findAny().orElse(null);
            if (renderPlayer instanceof AbstractClientPlayerEntity p) {
                skin = p.getSkinTexture();
            }
        }
        return skin;
    }

    public static class TeammatesListWidget extends EntryListWidget<TeammatesListWidget.Entry> {
        public TeammatesListWidget(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight, List<Teammate> teammates) {
            super(client, width, height, top, bottom, itemHeight);
            for (Teammate teammate : teammates) {
                addEntry(new Entry(this, teammate));
            }
        }

        @Override
        public void appendNarrations(NarrationMessageBuilder builder) {
        }

        public void addTeammate(Teammate teammate) {
            addEntry(new Entry(this, teammate));
        }

        public List<Teammate> getTeammatesList() {
            List<Teammate> teammates = new ArrayList<>();
            for (int i = 0; i < getEntryCount(); i++) {
                teammates.add(getEntry(i).teammate);
            }
            return teammates;
        }

        public boolean hasNickname(String nickname) {
            for (int i = 0; i < getEntryCount(); i++) {
                Entry entry = getEntry(i);
                if (entry.teammate.name.equalsIgnoreCase(nickname)) {
                    return true;
                }
            }
            return false;
        }

        public static class Entry extends EntryListWidget.Entry<Entry> {
            private final TeammatesListWidget list;
            private final Identifier skinTexture;
            private final Teammate teammate;
            private final ButtonWidget removeButton;
            private final ButtonWidget hideButton;

            public Entry(TeammatesListWidget list, Teammate teammate) {
                this.list = list;
                this.teammate = teammate;

                skinTexture = getSkinByName(teammate.name);

                removeButton = ButtonWidget.builder(X_TEXT, button -> {
                    this.list.removeEntry(this);
                }).size(16, 16).build();

                hideButton = ButtonWidget.builder(teammate.enabled ? ScreenTexts.OFF : ScreenTexts.ON, button -> {
                    toggleEnabled();
                }).size(32, 16).build();
            }

            @Override
            public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
                TextRenderer textRenderer = MinecraftClient.getInstance().textRenderer;

                context.fill(x + 1, y + 1, x + 35, y + 35, 0xffffffff);
                context.drawTexture(skinTexture, x + 2, y + 2, 32, 32, 8, 8, 8, 8, 64, 64);
                context.drawTextWithShadow(textRenderer, teammate.name, x + 40, y + ((entryHeight - textRenderer.fontHeight) >> 1), 0xffffff);

                updatePos(x, y, entryWidth);

                removeButton.render(context, mouseX, mouseY, tickDelta);
                hideButton.render(context, mouseX, mouseY, tickDelta);

                if (!teammate.enabled) context.fillGradient(x, y, x + entryWidth - 4, y + entryHeight, 0x88000000, 0x88000000);
            }

            public void toggleEnabled() {
                teammate.enabled = !teammate.enabled;
                hideButton.setMessage(teammate.enabled ? ScreenTexts.OFF : ScreenTexts.ON);
            }

            public void updatePos(int x, int y, int entryWidth) {
                y += 9;
                removeButton.setX(x + entryWidth - 26);
                removeButton.setY(y);

                hideButton.setX(x + entryWidth - 62);
                hideButton.setY(y);
            }

            @Override
            public boolean mouseClicked(double mouseX, double mouseY, int button) {
                if (removeButton.isMouseOver(mouseX, mouseY)) {
                    removeButton.mouseClicked(mouseX, mouseY, button);
                }
                else if (hideButton.isMouseOver(mouseX, mouseY)) {
                    hideButton.mouseClicked(mouseX, mouseY, button);
                }
                else return true;
                return false;
            }

            @Override
            public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
                if (keyCode == GLFW.GLFW_KEY_DELETE) {
                    list.removeEntry(this);
                    return true;
                }
                if (keyCode == GLFW.GLFW_KEY_BACKSPACE || keyCode == GLFW.GLFW_KEY_H) {
                    toggleEnabled();
                    return true;
                }
                return false;
            }
        }
    }
}
