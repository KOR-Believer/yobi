/**
 * Yobi, Project Hosting SW
 *
 * Copyright 2015 NAVER Corp.
 * http://yobi.io
 *
 * @author Hyeok Oh
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package sshs;

import java.io.IOException;

import org.apache.sshd.SshServer;
import org.apache.sshd.server.keyprovider.PEMGeneratorHostKeyProvider;

import utils.Config;
import utils.Constants;
import utils.Diagnostic;
import utils.SimpleDiagnostic;

public class SshDaemon {
    private static SshServer sshd;

    public void start() {
        // Configure SshDaemon
        sshd = SshServer.setUpDefaultServer();
        sshd.setPort(Config.getSshport());
        sshd.setKeyPairProvider(new PEMGeneratorHostKeyProvider(Constants.HOST_KEY, Constants.ALG_RSA, Constants.SIZE_RSA));
        sshd.setSessionFactory(new SshServerSessionFactory());
        sshd.setPublickeyAuthenticator(new SshPublicKeyAuth());
        sshd.setCommandFactory(new SshCommandFactory());
        sshd.setShellFactory(new SshShellFactory());

        // Start SshDaemon
        try {
            sshd.start();
        } catch (IOException e) {
            play.Logger.error("SSHD isn't start", e);
            sshd.close(true);
        }

        Diagnostic.register(new SimpleDiagnostic() {
            @Override
            public String checkOne() {
                if (sshd == null) {
                    return "Ssh Daemon is not initialized";
                } else if (sshd.isClosed()) {
                    return "Ssh Daemon is not running";
                } else {
                    return null;
                }
            }
        });
    }

    public void stop() {
        try {
            sshd.stop(true);
        } catch (InterruptedException e) {
            play.Logger.error("SSHD is stop anormaly", e);
        }
        sshd.close(true);
        sshd = null;
    }

    public static boolean isRunning() {
        return !sshd.isClosed();
    }

    public static int getCurrent_port() {
        return sshd.getPort();
    }
}