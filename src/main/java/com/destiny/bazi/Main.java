package com.destiny.bazi;

import com.destiny.bazi.ui.BaziFrame;
import com.destiny.bazi.ui.DarkTheme;

import javax.swing.*;
import java.nio.file.Path;

/**
 * 子平八字排盤程式進入點。
 */
public final class Main {

    public static void main(String[] args) {
        if (args.length == 2 && SelfTest.FLAG.equals(args[0])) {
            System.exit(SelfTest.run(Path.of(args[1])));
        }
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
            } catch (Exception ignored) {
                // 若系統無 Nimbus，退回預設外觀，不影響功能。
            }
            DarkTheme.applyGlobal();
            new BaziFrame().setVisible(true);
        });
    }
}
