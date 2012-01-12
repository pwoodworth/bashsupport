package com.ansorgit.plugins.bash.lang.psi.stubs.index;

import com.ansorgit.plugins.bash.lang.psi.api.command.BashIncludeCommand;
import com.intellij.psi.stubs.StringStubIndexExtension;
import com.intellij.psi.stubs.StubIndexKey;

/**
 * User: jansorg
 * Date: 11.01.12
 * Time: 22:46
 */
public class BashIncludeCommandIndex extends StringStubIndexExtension<BashIncludeCommand> {
    public static final StubIndexKey<String, BashIncludeCommand> KEY = StubIndexKey.createIndexKey("bash.includeCommand");

    @Override
    public StubIndexKey<String, BashIncludeCommand> getKey() {
        return KEY;
    }
}
