package net.burningtnt.hmat.game;

import net.burningtnt.hmat.AnalyzeResult;
import net.burningtnt.hmat.Analyzer;
import net.burningtnt.hmat.LogAnalyzable;
import net.burningtnt.hmat.solver.Solver;
import net.burningtnt.hmat.solver.SolverConfigurator;
import org.jackhuang.hmcl.Launcher;
import org.jackhuang.hmcl.ui.FXUtils;
import org.jackhuang.hmcl.util.StringUtils;
import org.jackhuang.hmcl.util.logging.Logger;
import org.jackhuang.hmcl.util.platform.OperatingSystem;

import java.io.IOException;
import java.util.List;

import static org.jackhuang.hmcl.util.i18n.I18n.i18n;

public class CodePageAnalyzer implements Analyzer<LogAnalyzable> {
    private static final String[] KEYS = {
            "java.lang.ClassNotFoundException",
            "\u0020\u627e\u4e0d\u5230\u6216\u65e0\u6cd5\u52a0\u8f7d\u4e3b\u7c7b\u0020",
            "[LWJGL] Failed to load a library. Possible solutions:"
    };

    @Override
    public ControlFlow analyze(LogAnalyzable input, List<AnalyzeResult<LogAnalyzable>> results) throws Exception {
        // Non-Windows OperatingSystem and ascii path should NOT encounter this problem.
        if (OperatingSystem.CURRENT_OS != OperatingSystem.WINDOWS || StringUtils.isASCII(input.getRepository().getBaseDirectory().toString())) {
            return ControlFlow.CONTINUE;
        }

        List<String> logs = input.getLogs();
        if (logs.size() >= 10) {
            return ControlFlow.CONTINUE;
        }

        if (StringUtils.containsOne(logs, KEYS)) {
            results.add(new AnalyzeResult<>(this, AnalyzeResult.ResultID.LOG_GAME_CODE_PAGE, new Solver() {
                private int BTN_OPEN_INTL = -1;

                private int BTN_REBOOT_COMPUTER = -1;

                @Override
                public void configure(SolverConfigurator configurator) {
                    configurator.setDescription(i18n("analyzer.result.log_game_code_page.steps.1"));
                    configurator.setImage(FXUtils.newBuiltinImage("/assets/img/hmat/log/game/code_page/step_1.png"));

                    BTN_OPEN_INTL = configurator.putButton(i18n("analyzer.result.log_game_code_page.button.open_intl"));
                    BTN_REBOOT_COMPUTER = configurator.putButton(i18n("analyzer.result.log_game_code_page.button.reboot_computer"));
                }

                @Override
                public void callbackSelection(SolverConfigurator configurator, int selectionID) {
                    if (selectionID == BTN_OPEN_INTL) {
                        try {
                            Runtime.getRuntime().exec(new String[]{
                                    "rundll32.exe",
                                    "shell32.dll,Control_RunDLL",
                                    "intl.cpl"
                            });
                        } catch (IOException e) {
                            Logger.LOG.warning("Cannot open intl.", e);
                        }
                    } else if (selectionID == BTN_NEXT || selectionID == BTN_REBOOT_COMPUTER) {
                        Launcher.rebootComputer();
                    }
                }
            }));

            return ControlFlow.BREAK_OTHER;
        }

        return ControlFlow.CONTINUE;
    }
}
