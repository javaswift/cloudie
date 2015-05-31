/*
 *  Copyright 2012-2013 E.Hooijmeijer
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.javaswift.cloudie;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

import org.javaswift.joss.client.factory.AuthenticationMethod;

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;

/**
 * Main entry point for Cloudie, parses commandline arguments and creates the userinterface.
 */
public class Main {

    public static class Arguments {
        @Parameter(names = { "-basic" }, description = "connects to the cloud using basic auth. Takes 4 arguments: [AuthURL] [TenantName] [Username] [Password]", arity = 4)
        private List<String> basic = new ArrayList<String>();
        @Parameter(names = { "-temp" }, description = "connects to the cloud using basic auth. Takes 4 arguments: [AuthURL] [TenantName] [Username] [Password]", arity = 4)
        private List<String> temp = new ArrayList<String>();
        @Parameter(names = { "-keystone" }, description = "connects to the cloud using basic auth. Takes 5 arguments: [AuthURL] [TenantName] [Username] [Password] [TenantId]", arity = 5)
        private List<String> keystone = new ArrayList<String>();
        @Parameter(names = { "-profile" }, description = "connects to the cloud using an previously stored profile.")
        private String profile;
        @Parameter(names = { "-help", "-?", "--?" }, description = "Brief help.")
        private boolean help;
    }

    public static void main(String[] args) throws IOException {
        Arguments params = new Arguments();
        JCommander cmd = new JCommander(params);
        try {
            cmd.parse(args);
            //
            if (params.help) {
                System.out.println("Cloudie - an Open Stack Storage browser.");
                System.out.println("(C) 2012 - E.Hooijmeijer");
                System.out.println("Distributed under the Apache 2.0 License");
                cmd.usage();
            } else {
                openCloudie(createCloudie(params));
            }
        } catch (ParameterException ex) {
            System.out.println(ex.getMessage());
            cmd.usage();
        }
    }

    private static void openCloudie(final CloudiePanel cp) throws IOException {
        JFrame frame = new JFrame("Cloudie");
        frame.setSize(800, 600);
        frame.setLocationByPlatform(true);
        frame.setIconImage(ImageIO.read(Main.class.getResource("/icons/weather_cloudy.png")));
        frame.getContentPane().add(cp);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (cp.onClose()) {
                    System.exit(0);
                }
            }
        });
        cp.setOwner(frame);
        frame.setJMenuBar(cp.createMenuBar());
        frame.setVisible(true);
    }

    private static CloudiePanel createCloudie(Arguments args) {
        if (args.basic.size() == 4) {
            return new CloudiePanel(AuthenticationMethod.BASIC, args.basic);
        } else if (args.temp.size() == 4) {
            return new CloudiePanel(AuthenticationMethod.TEMPAUTH, args.temp);
        } else if (args.keystone.size() == 5) {
            return new CloudiePanel(AuthenticationMethod.KEYSTONE, args.keystone);
        } else if (args.profile != null) {
            return new CloudiePanel(args.profile);
        } else {
            return new CloudiePanel();
        }
    }
}
