package com.deveisia.enceladus.voidminerrecipe.ui;

import com.deveisia.enceladus.voidminerrecipe.machine.VoidMinerMachine;
import com.deveisia.enceladus.voidminerrecipe.util.VeinGroup;
import com.deveisia.enceladus.voidminerrecipe.util.VeinUtil;

import com.gregtechceu.gtceu.api.gui.GuiTextures;
import com.gregtechceu.gtceu.api.gui.fancy.FancyMachineUIWidget;
import com.gregtechceu.gtceu.api.gui.fancy.IFancyUIProvider;

import com.lowdragmc.lowdraglib.gui.editor.ColorPattern;
import com.lowdragmc.lowdraglib.gui.texture.GuiTextureGroup;
import com.lowdragmc.lowdraglib.gui.texture.IGuiTexture;
import com.lowdragmc.lowdraglib.gui.texture.ItemStackTexture;
import com.lowdragmc.lowdraglib.gui.texture.ResourceBorderTexture;
import com.lowdragmc.lowdraglib.gui.texture.TextTexture;
import com.lowdragmc.lowdraglib.gui.widget.ButtonWidget;
import com.lowdragmc.lowdraglib.gui.widget.DraggableScrollableWidgetGroup;
import com.lowdragmc.lowdraglib.gui.widget.ImageWidget;
import com.lowdragmc.lowdraglib.gui.widget.LabelWidget;
import com.lowdragmc.lowdraglib.gui.widget.Widget;
import com.lowdragmc.lowdraglib.gui.widget.WidgetGroup;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.List;

public class VeinSelectorFancyConfigurator implements IFancyUIProvider {
    /** 선택 버튼 높이 (기존 14의 1.5배). */
    private static final int ROW_H = 21;
    private static final int LIST_W = 150;
    private static final int EXPAND_W = 20;

    private final VoidMinerMachine machine;

    private DraggableScrollableWidgetGroup groupList;
    private final List<DraggableScrollableWidgetGroup> memberLists = new ArrayList<>();
    private ButtonWidget backButton;

    public VeinSelectorFancyConfigurator(VoidMinerMachine machine) {
        this.machine = machine;
    }

    @Override
    public Widget createMainPage(FancyMachineUIWidget widget) {
        memberLists.clear();

        var root = new WidgetGroup(0, 0, 176, 166);
        root.setBackground(GuiTextures.BACKGROUND_INVERSE);

        root.addWidget(new LabelWidget(8, 6,
                () -> Component.translatable("voidminerrecipe.gui.vein_selector.selected",
                        machine.getSelectedVeinSet().size()).getString()).setDropShadow(false));

        backButton = new ButtonWidget(78, 3, 32, 14,
                new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON,
                        new TextTexture("voidminerrecipe.gui.vein_selector.back").setWidth(32)),
                cd -> openGroup(-1));
        root.addWidget(backButton);

        root.addWidget(new ButtonWidget(112, 3, 56, 14,
                new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON,
                        new TextTexture("voidminerrecipe.gui.vein_selector.clear").setWidth(56)),
                cd -> machine.clearVeins()));

        groupList = newList();
        root.addWidget(groupList);

        List<VeinGroup> groups = VeinUtil.groups(machine.isRemote());
        for (int i = 0; i < groups.size(); i++) {
            final int groupIndex = i;
            final VeinGroup g = groups.get(i);
            final int y = i * ROW_H;
            final String name = VeinUtil.displayName(g.representative());

            var members = newList();
            members.setVisible(false);
            members.setActive(false);
            members.addWidget(new LabelWidget(0, 2, name + " (" + g.members().size() + ")")
                    .setDropShadow(false));
            for (int m = 0; m < g.members().size(); m++) {
                addVeinRow(members, 0, (m + 1) * ROW_H + 2, LIST_W, g.members().get(m),
                        (m < g.ownCount() ? "" : "> ") + VeinUtil.displayName(g.members().get(m)));
            }
            root.addWidget(members);
            memberLists.add(members);

            if (g.members().size() == 1) {
                addVeinRow(groupList, 0, y, LIST_W, g.representative(), name);
            } else {
                final int width = LIST_W - EXPAND_W - 2;
                groupList.addWidget(new ButtonWidget(0, y, width, ROW_H, IGuiTexture.EMPTY,
                        cd -> machine.toggleGroup(g.members(), g.representative())));
                groupList.addWidget(new ImageWidget(0, y, width, ROW_H, () -> {
                    int sel = machine.countSelected(g.members());
                    String label = name + " (+" + (g.members().size() - 1) + ")" +
                            (sel > 0 ? " [" + sel + "]" : "");
                    return new GuiTextureGroup(
                            ResourceBorderTexture.BUTTON_COMMON.copy()
                                    .setColor(sel > 0 ? ColorPattern.CYAN.color : -1),
                            new TextTexture(label).setWidth(width).setType(TextTexture.TextType.ROLL));
                }));
                groupList.addWidget(new ButtonWidget(width + 2, y, EXPAND_W, ROW_H,
                        new GuiTextureGroup(ResourceBorderTexture.BUTTON_COMMON,
                                new TextTexture(">").setWidth(EXPAND_W)),
                        cd -> openGroup(groupIndex)));
            }
        }

        openGroup(-1);
        return root;
    }

    private DraggableScrollableWidgetGroup newList() {
        var list = new DraggableScrollableWidgetGroup(8, 22, 160, 136);
        list.setYScrollBarWidth(8);
        list.setYBarStyle(GuiTextures.SLIDER_BACKGROUND_VERTICAL, GuiTextures.BUTTON);
        return list;
    }

    private void addVeinRow(WidgetGroup parent, int x, int y, int width, ResourceLocation id, String label) {
        parent.addWidget(new ButtonWidget(x, y, width, ROW_H, IGuiTexture.EMPTY, cd -> machine.toggleVein(id)));
        parent.addWidget(new ImageWidget(x, y, width, ROW_H,
                () -> new GuiTextureGroup(
                        ResourceBorderTexture.BUTTON_COMMON.copy()
                                .setColor(machine.isVeinSelected(id) ? ColorPattern.CYAN.color : -1),
                        new TextTexture(label).setWidth(width).setType(TextTexture.TextType.ROLL))));
    }

    private void openGroup(int index) {
        groupList.setVisible(index < 0);
        groupList.setActive(index < 0);
        for (int i = 0; i < memberLists.size(); i++) {
            memberLists.get(i).setVisible(i == index);
            memberLists.get(i).setActive(i == index);
        }
        backButton.setVisible(index >= 0);
        backButton.setActive(index >= 0);
    }

    @Override
    public IGuiTexture getTabIcon() {
        return new ItemStackTexture(Items.DIAMOND_PICKAXE);
    }

    @Override
    public Component getTitle() {
        return Component.translatable("voidminerrecipe.gui.vein_selector.title");
    }

    @Override
    public List<Component> getTabTooltips() {
        return List.of(Component.translatable("voidminerrecipe.gui.vein_selector.tooltip"));
    }

    @Override
    public boolean hasPlayerInventory() {
        return false;
    }
}
