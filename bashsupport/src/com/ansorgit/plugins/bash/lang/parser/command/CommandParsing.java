/*
 * Copyright 2009 Joachim Ansorg, mail@ansorg-it.com
 * File: CommandParsing.java, Class: CommandParsing
 * Last modified: 2009-12-04
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ansorgit.plugins.bash.lang.parser.command;

import com.ansorgit.plugins.bash.lang.parser.ParsingChain;
import com.ansorgit.plugins.bash.lang.parser.ParsingTool;
import com.ansorgit.plugins.bash.lang.parser.builtin.BuiltinCommandParser;

/**
 * The parsing of commands. A command is either a simple command, a shell command or a function
 * definition.
 * <p/>
 * Date: 24.03.2009
 * Time: 23:03:27
 *
 * @author Joachim Ansorg
 */
public class CommandParsing extends ParsingChain implements ParsingTool {
    public final SimpleCommandParsingFunction simpleCommandParser = new SimpleCommandParsingFunction();

    public CommandParsing() {
        /* The grammar:

        command2 :	simple_command
            | shell_command
            | shell_command redirection_list
            | function_def ;
         */
        addParsingFunction(new ShellCommandDelegator());
        addParsingFunction(new FunctionDefParsingFunction());
        addParsingFunction(new BuiltinCommandParser());
        addParsingFunction(simpleCommandParser); //has to be last
    }
}
