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
package org.javaswift.cloudie.ops;

import java.io.File;

import org.javaswift.cloudie.ops.CloudieOperations.CloudieCallback;
import org.javaswift.cloudie.util.AsyncWrapper;
import org.javaswift.joss.client.factory.AccountConfig;
import org.javaswift.joss.exception.CommandException;
import org.javaswift.joss.model.Account;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class CloudieOperationsAsyncTest {

    private CloudieOperations ops;
    private CloudieCallback callback;
    private AccountConfig accountConfig;
    private Account account;

    @Before
    public void init() {
        accountConfig = new AccountConfig();
        accountConfig.setMockAllowEveryone(true);
        CloudieOperationsImpl target = new CloudieOperationsImpl(true);
        ops = AsyncWrapper.async(target);
        callback = Mockito.mock(CloudieCallback.class);
        target.login(accountConfig, callback);
        account = target.getAccount();
    }

    @Test
    public void shouldSignalStartAndDone() throws InterruptedException {
        ops.createContainer(new ContainerSpecification("x", true), callback);
        Thread.sleep(500L);
        Mockito.verify(callback, Mockito.atLeastOnce()).onStart();
        Mockito.verify(callback, Mockito.atLeastOnce()).onDone();
    }

    @Test
    public void shouldSignalCommandException() throws InterruptedException {
        ops.createStoredObjects(account.getContainer("x"), new File[] { new File("pom.xml") }, callback); // container
                                                                                                          // does
                                                                                                          // not
                                                                                                          // exist.
        Thread.sleep(500L);
        Mockito.verify(callback, Mockito.atLeastOnce()).onStart();
        Mockito.verify(callback, Mockito.atLeastOnce()).onError(Mockito.any(CommandException.class));
        Mockito.verify(callback, Mockito.atLeastOnce()).onDone();
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAcceptNullCallback() {
        ops.createContainer(new ContainerSpecification("x", true), null);
    }
}
